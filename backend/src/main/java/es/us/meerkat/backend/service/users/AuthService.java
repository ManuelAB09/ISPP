package es.us.meerkat.backend.service.users;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import es.us.meerkat.backend.dto.chats.MessageResponse;
import es.us.meerkat.backend.dto.google.GoogleAuthRequest;
import es.us.meerkat.backend.dto.google.GoogleAuthResponse;
import es.us.meerkat.backend.dto.maps.UbicacionResponse;
import es.us.meerkat.backend.dto.users.AuthResponse;
import es.us.meerkat.backend.dto.users.ForgotPasswordRequest;
import es.us.meerkat.backend.dto.users.LoginRequest;
import es.us.meerkat.backend.dto.users.RegisterRequest;
import es.us.meerkat.backend.dto.users.ResetPasswordRequest;
import es.us.meerkat.backend.dto.users.TotpEnableResponse;
import es.us.meerkat.backend.dto.users.TotpSetupResponse;
import es.us.meerkat.backend.dto.users.TwoFactorChallengeResponse;
import es.us.meerkat.backend.dto.users.UserDetailResponse;
import es.us.meerkat.backend.entity.maps.Ubicacion;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.exception.ConflictException;
import es.us.meerkat.backend.exception.EmailNotVerifiedException;
import es.us.meerkat.backend.exception.ValidationException;
import es.us.meerkat.backend.repository.users.UsuarioRepository;
import es.us.meerkat.backend.security.JwtService;
import es.us.meerkat.backend.service.emails.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servicio de autenticación.
 *
 * <p>Gestiona el registro de nuevos usuarios y el inicio de sesión, generando tokens JWT reales
 * mediante {@link JwtService}. Corresponde a los endpoints POST /api/v1/auth/register y POST
 * /api/v1/auth/login del OpenAPI.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    /** Longitud mínima requerida para las contraseñas. */
    private static final int MIN_PASSWORD_LENGTH = 8;

    /** Longitud máxima permitida para las contraseñas. */
    private static final int MAX_PASSWORD_LENGTH = 128;

    /** Horas de validez del token de verificación. */
    private static final int VERIFICATION_TOKEN_HOURS = 24;

    /** Cantidad de códigos de respaldo generados al activar 2FA. */
    private static final int BACKUP_CODES_COUNT = 8;

    /** Longitud de cada código de respaldo (sin separador). */
    private static final int BACKUP_CODE_LENGTH = 8;

    /** Máximo de solicitudes de recuperación por email en la ventana de tiempo. */
    private static final int PASSWORD_RESET_MAX_REQUESTS = 3;

    /** Ventana de tiempo del rate limit para recuperación de contraseña (15 minutos). */
    private static final long PASSWORD_RESET_WINDOW_MS = 15 * 60 * 1000L;

    /**
     * Cache in-memory para rate-limiting de solicitudes de recuperación de contraseña. Clave: email
     * normalizado, Valor: lista de timestamps de solicitudes.
     */
    private final java.util.concurrent.ConcurrentHashMap<
                    String, java.util.concurrent.CopyOnWriteArrayList<Long>>
            passwordResetAttempts = new java.util.concurrent.ConcurrentHashMap<>();

    /** URL base de la aplicación frontend para verificación. */
    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    /** Repositorio para acceder a la información de usuarios. */
    private final UsuarioRepository usuarioRepository;

    /** Codificador de contraseñas BCrypt. */
    private final BCryptPasswordEncoder passwordEncoder;

    /** Servicio para generación y validación de tokens JWT. */
    private final JwtService jwtService;

    /** Servicio de correo electrónico */
    private final EmailService emailService;

    @Value("${google.classroom.client-id}")
    private String googleClientId;

    @Value("${google.classroom.client-secret}")
    private String googleClientSecret;

    @Value("${oauth.redirect-uri-login}")
    private String googleRedirectUriLogin;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    // Temp store for OAuth state -> flowType ("login" or "link")
    private final Map<String, String> oauthStateStore = new ConcurrentHashMap<>();

    // Temp store for login challenges (2FA). Token -> (userId, expiresAt)
    private final Map<String, TempLogin> tempLoginStore = new ConcurrentHashMap<>();

    private static record TempLogin(Long userId, Instant expiresAt) {}

    // ===============================
    // REGISTRO
    // ===============================

    /**
     * Registra un nuevo usuario con email y contraseña.
     *
     * <p>Valida que el email sea único y que la contraseña tenga al menos 8 caracteres. Genera un
     * token de verificación y envía un email para que el usuario verifique su cuenta.
     *
     * @param requestParam Datos del nuevo usuario.
     * @return MessageResponse con instrucciones para verificar el email.
     * @throws ValidationException si los datos no son válidos (400).
     * @throws ConflictException si el email ya está registrado (409).
     */
    @Transactional
    public MessageResponse registrar(final RegisterRequest requestParam) {
        validateRegistrationData(requestParam);

        if (usuarioRepository.existsByEmail(requestParam.getEmail())) {
            throw new ConflictException("El email ya está registrado");
        }

        // Generar token de verificación
        final String verificationToken = UUID.randomUUID().toString();
        final LocalDateTime tokenExpiration =
                LocalDateTime.now().plusHours(VERIFICATION_TOKEN_HOURS);

        final Usuario usuario = new Usuario();
        usuario.setEmail(requestParam.getEmail());
        usuario.setPassword(passwordEncoder.encode(requestParam.getPassword()));
        usuario.setNombre(requestParam.getNombre());
        usuario.setEsTutor(Boolean.TRUE.equals(requestParam.getEsTutor()));
        usuario.setVisibleEnListados(true);
        usuario.setAutenticacionDosFactores(false);
        usuario.setNotificacionesPush(true);
        usuario.setIntereses(new ArrayList<>());
        usuario.setEmailVerificado(false);
        usuario.setVerificationToken(verificationToken);
        usuario.setTokenExpiration(tokenExpiration);
        usuarioRepository.save(usuario);

        // Enviar email de verificación
        try {
            String verificationUrl = frontendUrl + "/verify-email";
            emailService.sendVerificationEmail(
                    usuario.getEmail(),
                    usuario.getNombre() != null ? usuario.getNombre() : "Usuario",
                    verificationToken,
                    verificationUrl);
        } catch (Exception e) {
            log.error("Error al enviar email de verificación: {}", e.getMessage());
            // Continuamos aunque falle el email, el usuario puede solicitar reenvío
        }

        return MessageResponse.builder()
                .message(
                        "Registro exitoso. Por favor, revisa tu correo electrónico para verificar"
                                + " tu cuenta.")
                .build();
    }

    /**
     * Valida los datos de registro.
     *
     * @param request Datos a validar.
     * @throws ValidationException si algún dato es inválido (400).
     */
    private void validateRegistrationData(final RegisterRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new ValidationException("El email no puede estar vacío");
        }

        // Validar formato de email (más estricto)
        final String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        if (!request.getEmail().matches(emailRegex)) {
            throw new ValidationException("El formato del email no es válido");
        }

        if (request.getNombre() == null || request.getNombre().isBlank()) {
            throw new ValidationException("El nombre no puede estar vacío");
        }

        if (request.getPassword() == null || request.getPassword().length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException("La contraseña debe tener al menos 8 caracteres");
        }

        if (request.getPassword().length() > MAX_PASSWORD_LENGTH) {
            throw new ValidationException("La contraseña no puede tener más de 128 caracteres");
        }

        if (!request.getPassword().matches(".*[A-Z].*")
                || !request.getPassword().matches(".*[a-z].*")
                || !request.getPassword().matches(".*[0-9].*")) {
            throw new ValidationException(
                    "La contraseña debe contener mayúsculas, minúsculas y números");
        }
    }

    // ===============================
    // INICIO DE SESIÓN
    // ===============================

    /**
     * Autentica a un usuario con sus credenciales.
     *
     * <p>Verifica que el email exista, que la contraseña coincida con la almacenada cifrada y que
     * el email haya sido verificado. Devuelve un token JWT válido.
     *
     * @param requestParam Credenciales del usuario.
     * @return AuthResponse con token JWT y datos del usuario.
     * @throws ValidationException si las credenciales son incorrectas (400).
     * @throws EmailNotVerifiedException si el email no ha sido verificado (403).
     */
    public Object iniciarSesion(final LoginRequest requestParam) {

        final Usuario usuario =
                usuarioRepository
                        .findByEmail(requestParam.getEmail())
                        .orElseThrow(
                                () -> new ValidationException("Este email no está registrado"));

        if (!passwordEncoder.matches(requestParam.getPassword(), usuario.getPassword())) {
            throw new ValidationException("Credenciales incorrectas");
        }

        if (!Boolean.TRUE.equals(usuario.getEmailVerificado())) {
            throw new EmailNotVerifiedException(
                    "Debes verificar tu email antes de iniciar sesión. Revisa tu bandeja de entrada"
                            + " o solicita un nuevo email de verificación.");
        }

        if (applyPreferenceDefaultsIfNeeded(usuario)) {
            usuarioRepository.save(usuario);
        }

        // Si el usuario tiene 2FA activado, iniciar desafío y devolver tempToken
        if (usuario.getAutenticacionDosFactores()
                && usuario.getTotpSecret() != null
                && !usuario.getTotpSecret().isBlank()) {
            String tempToken = createTempLoginToken(usuario.getId());
            return new TwoFactorChallengeResponse(true, tempToken);
        }

        final String token = jwtService.generateToken(usuario.getEmail());

        return buildAuthResponse(usuario, token);
    }

    private String createTempLoginToken(Long userId) {
        String t = java.util.UUID.randomUUID().toString();
        Instant expires = Instant.now().plusSeconds(300); // 5 minutes
        tempLoginStore.put(t, new TempLogin(userId, expires));
        return t;
    }

    private Long consumeTempLoginToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        TempLogin tl = tempLoginStore.remove(token);
        if (tl == null) {
            return null;
        }
        if (Instant.now().isAfter(tl.expiresAt())) {
            return null;
        }
        return tl.userId();
    }

    public AuthResponse completeLoginWith2fa(final String tempToken, final String code) {
        Long userId = consumeTempLoginToken(tempToken);
        if (userId == null) {
            throw new ValidationException("Token temporal inválido o expirado");
        }
        Usuario usuario =
                usuarioRepository
                        .findById(userId)
                        .orElseThrow(() -> new ValidationException("Usuario no encontrado"));
        if (usuario.getTotpSecret() == null) {
            throw new ValidationException("2FA no configurado para este usuario");
        }
        boolean validTotp = verifyTotpCode(usuario.getTotpSecret(), code);
        boolean usedBackupCode = false;
        if (!validTotp) {
            usedBackupCode = consumeBackupCode(usuario, code);
        }
        if (!validTotp && !usedBackupCode) {
            throw new ValidationException("Código 2FA o de respaldo inválido");
        }
        if (usedBackupCode) {
            usuarioRepository.save(usuario);
        }

        final String token = jwtService.generateToken(usuario.getEmail());
        return buildAuthResponse(usuario, token);
    }

    /** Vincula una cuenta Google (idToken) al usuario actualmente autenticado. */
    public MessageResponse linkGoogleToCurrentUser(final GoogleAuthRequest request) {
        if (request == null || request.getIdToken() == null || request.getIdToken().isBlank()) {
            throw new ValidationException("idToken de Google requerido");
        }

        try {
            String sanitizedClientId = googleClientId != null ? googleClientId.trim() : "";
            log.info(
                    "Vinculando Google: Verificando ID Token con Client ID: '{}'",
                    sanitizedClientId);

            GoogleIdTokenVerifier verifier =
                    new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                            .setAudience(java.util.Collections.singletonList(sanitizedClientId))
                            .build();

            GoogleIdToken idToken = verifier.verify(request.getIdToken());
            if (idToken == null) {
                try {
                    GoogleIdToken unverifiedToken =
                            GoogleIdToken.parse(new GsonFactory(), request.getIdToken());
                    log.error(
                            "Token fail. Audience del token: {}",
                            unverifiedToken.getPayload().getAudience());
                    log.error("Issuer del token: {}", unverifiedToken.getPayload().getIssuer());
                } catch (Exception e) {
                    // Ignored
                }
                throw new ValidationException("ID token de Google inválido");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String googleId = payload.getSubject();
            String email = payload.getEmail();

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                throw new ValidationException("Unauthorized");
            }
            Object principal = auth.getPrincipal();
            if (!(principal instanceof Usuario)) {
                throw new ValidationException("Invalid user principal");
            }
            Usuario usuario = (Usuario) principal;

            // Si otro usuario ya tiene este googleId, conflicto
            usuarioRepository
                    .findByGoogleId(googleId)
                    .ifPresent(
                            other -> {
                                if (!other.getId().equals(usuario.getId())) {
                                    throw new ConflictException(
                                            "Este Google account ya está vinculado a otra cuenta");
                                }
                            });

            // Requerir que el email del idToken coincida con el email del usuario
            if (email == null || !email.equalsIgnoreCase(usuario.getEmail())) {
                throw new ConflictException(
                        "El email del Google account no coincide con la cuenta actual");
            }

            usuario.setGoogleId(googleId);
            usuarioRepository.save(usuario);

            return MessageResponse.builder()
                    .message("Cuenta Google vinculada correctamente")
                    .build();

        } catch (ValidationException ve) {
            throw ve;
        } catch (Exception e) {
            throw new ValidationException("Error verificando ID token de Google", e);
        }
    }

    /** Desvincula la cuenta Google del usuario autenticado. */
    public MessageResponse unlinkGoogleFromCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new ValidationException("Unauthorized");
        }
        Object principal = auth.getPrincipal();
        if (!(principal instanceof Usuario)) {
            throw new ValidationException("Invalid user principal");
        }
        Usuario usuario = (Usuario) principal;

        usuario.setGoogleId(null);
        usuarioRepository.save(usuario);

        return MessageResponse.builder().message("Cuenta Google desvinculada").build();
    }

    public TotpSetupResponse generateTotpSetupForCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new ValidationException("Unauthorized");
        }
        Object principal = auth.getPrincipal();
        if (!(principal instanceof Usuario)) {
            throw new ValidationException("Invalid user principal");
        }
        Usuario usuario = (Usuario) principal;

        String secret = generateBase32Secret(20);
        usuario.setTotpTempSecret(secret);
        usuarioRepository.save(usuario);

        String issuer = "Meerkat";
        String label = issuer + ":" + usuario.getEmail();
        String otpauth =
                String.format(
                        "otpauth://totp/%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=30",
                        urlEncode(label), secret, urlEncode(issuer));

        return new TotpSetupResponse(secret, otpauth);
    }

    public TotpEnableResponse enableTotpForCurrentUser(final String code) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new ValidationException("Unauthorized");
        }
        Object principal = auth.getPrincipal();
        if (!(principal instanceof Usuario)) {
            throw new ValidationException("Invalid user principal");
        }
        Usuario usuario = (Usuario) principal;

        String temp = usuario.getTotpTempSecret();
        if (temp == null || temp.isBlank()) {
            throw new ValidationException("No hay clave TOTP temporal registrada");
        }
        if (!verifyTotpCode(temp, code)) {
            throw new ValidationException("Código 2FA inválido");
        }

        List<String> backupCodes = generateBackupCodes();
        List<String> backupCodeHashes = new ArrayList<>();
        for (String backupCode : backupCodes) {
            backupCodeHashes.add(passwordEncoder.encode(normalizeBackupCode(backupCode)));
        }

        usuario.setTotpSecret(temp);
        usuario.setTotpTempSecret(null);
        usuario.setBackupCodeHashes(backupCodeHashes);
        usuario.setAutenticacionDosFactores(true);
        usuarioRepository.save(usuario);

        return TotpEnableResponse.builder()
                .message("2FA activado")
                .backupCodes(backupCodes)
                .build();
    }

    public MessageResponse disableTotpForCurrentUser(final String code) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new ValidationException("Unauthorized");
        }
        Object principal = auth.getPrincipal();
        if (!(principal instanceof Usuario)) {
            throw new ValidationException("Invalid user principal");
        }
        Usuario usuario = (Usuario) principal;

        String secret = usuario.getTotpSecret();
        if (secret == null || secret.isBlank()) {
            throw new ValidationException("2FA no está activado");
        }

        boolean validTotp = verifyTotpCode(secret, code);
        boolean usedBackupCode = false;
        if (!validTotp) {
            usedBackupCode = consumeBackupCode(usuario, code);
        }
        if (!validTotp && !usedBackupCode) {
            throw new ValidationException("Código 2FA o de respaldo inválido");
        }

        usuario.setTotpSecret(null);
        usuario.setTotpTempSecret(null);
        usuario.setBackupCodeHashes(new ArrayList<>());
        usuario.setAutenticacionDosFactores(false);
        usuarioRepository.save(usuario);

        return MessageResponse.builder().message("2FA desactivado").build();
    }

    @SuppressWarnings("deprecation")
    private List<String> generateBackupCodes() {
        List<String> backupCodes = new ArrayList<>();
        for (int i = 0; i < BACKUP_CODES_COUNT; i++) {
            String rawCode = RandomStringUtils.randomAlphanumeric(BACKUP_CODE_LENGTH).toUpperCase();
            String formattedCode = rawCode.substring(0, 4) + "-" + rawCode.substring(4);
            backupCodes.add(formattedCode);
        }
        return backupCodes;
    }

    private boolean consumeBackupCode(final Usuario usuario, final String code) {
        if (code == null || code.isBlank()) {
            return false;
        }

        List<String> backupCodeHashes = usuario.getBackupCodeHashes();
        if (backupCodeHashes == null || backupCodeHashes.isEmpty()) {
            return false;
        }

        String normalizedCode = normalizeBackupCode(code);
        for (int i = 0; i < backupCodeHashes.size(); i++) {
            String hash = backupCodeHashes.get(i);
            if (passwordEncoder.matches(normalizedCode, hash)) {
                backupCodeHashes.remove(i);
                usuario.setBackupCodeHashes(backupCodeHashes);
                return true;
            }
        }

        return false;
    }

    private String normalizeBackupCode(final String code) {
        return code.replace("-", "").replace(" ", "").trim().toUpperCase();
    }

    private String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }

    // ------------------ TOTP implementation (Base32 + RFC6238) ------------------
    private String generateBase32Secret(int numBytes) {
        byte[] bytes = new byte[numBytes];
        new SecureRandom().nextBytes(bytes);
        return base32Encode(bytes);
    }

    private boolean verifyTotpCode(String base32Secret, String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        byte[] key = base32Decode(base32Secret);
        long timeWindow = System.currentTimeMillis() / 1000L / 30L;
        for (int i = -1; i <= 1; i++) {
            long t = timeWindow + i;
            String generated = generateTotpForCounter(key, t);
            if (generated.equals(code)) {
                return true;
            }
        }
        return false;
    }

    private String generateTotpForCounter(byte[] key, long counter) {
        try {
            byte[] data = new byte[8];
            for (int i = 7; i >= 0; i--) {
                data[i] = (byte) (counter & 0xff);
                counter >>= 8;
            }
            Mac mac = Mac.getInstance("HmacSHA1");
            SecretKeySpec signKey = new SecretKeySpec(key, "HmacSHA1");
            mac.init(signKey);
            byte[] hash = mac.doFinal(data);
            int offset = hash[hash.length - 1] & 0xf;
            int binary =
                    ((hash[offset] & 0x7f) << 24)
                            | ((hash[offset + 1] & 0xff) << 16)
                            | ((hash[offset + 2] & 0xff) << 8)
                            | (hash[offset + 3] & 0xff);
            int otp = binary % 1000000;
            return String.format("%06d", otp);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private String base32Encode(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int i = 0, index = 0, digit = 0;
        int currByte, nextByte;
        while (i < data.length) {
            currByte = data[i] >= 0 ? data[i] : data[i] + 256;
            if (index > 3) {
                if ((i + 1) < data.length) {
                    nextByte = data[i + 1] >= 0 ? data[i + 1] : data[i + 1] + 256;
                } else {
                    nextByte = 0;
                }
                digit = currByte & (0xFF >> index);
                index = (index + 5) % 8;
                digit = (digit << index) | (nextByte >> (8 - index));
                i++;
            } else {
                digit = (currByte >> (8 - (index + 5))) & 0x1F;
                index = (index + 5) % 8;
                if (index == 0) {
                    i++;
                }
            }
            sb.append(BASE32_ALPHABET.charAt(digit));
        }
        return sb.toString();
    }

    private byte[] base32Decode(String s) {
        s = s.replace("=", "").replace(" ", "").toUpperCase();
        int numBytes = s.length() * 5 / 8;
        byte[] result = new byte[numBytes];
        int buffer = 0, bitsLeft = 0, count = 0;
        for (char c : s.toCharArray()) {
            int val = BASE32_ALPHABET.indexOf(c);
            if (val < 0) {
                continue;
            }
            buffer <<= 5;
            buffer |= val & 0x1F;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                result[count++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }
        if (count == result.length) {
            return result;
        }
        byte[] truncated = new byte[count];
        System.arraycopy(result, 0, truncated, 0, count);
        return truncated;
    }

    /**
     * Devuelve la URL a la que el frontend debe redirigir al usuario para el inicio de sesión o
     * vinculación de Google.
     */
    public String getGoogleAuthorizeUrl(String flowType) {
        String state = UUID.randomUUID().toString();
        // Guardamos si es para "login" o para "link" (incluye userId en link)
        if ("link".equals(flowType)) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof Usuario) {
                Usuario user = (Usuario) auth.getPrincipal();
                oauthStateStore.put(state, "link:" + user.getId());
            } else {
                throw new ValidationException("Debes estar autenticado para vincular");
            }
        } else {
            oauthStateStore.put(state, "login");
        }

        String scopes = "openid email profile";
        try {
            return "https://accounts.google.com/o/oauth2/v2/auth"
                    + "?client_id="
                    + googleClientId
                    + "&redirect_uri="
                    + URLEncoder.encode(googleRedirectUriLogin, StandardCharsets.UTF_8)
                    + "&response_type=code"
                    + "&scope="
                    + URLEncoder.encode(scopes, StandardCharsets.UTF_8)
                    + "&state="
                    + URLEncoder.encode(state, StandardCharsets.UTF_8)
                    + "&prompt=select_account";
        } catch (Exception e) {
            throw new RuntimeException("Error construyendo URL de Google", e);
        }
    }

    /**
     * Procesa el código devuelto por Google, obtiene los tokens, el perfil de usuario y deuelve un
     * HTML que envía los datos al frontend.
     */
    @Transactional
    public ResponseEntity<String> processGoogleCallback(
            String code, String errorMsg, String state) {
        if (errorMsg != null) {
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(htmlPostMessageError(errorMsg));
        }
        if (code == null || state == null) {
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(htmlPostMessageError("Falta codigo o state"));
        }

        String flowData = oauthStateStore.remove(state);
        if (flowData == null) {
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(htmlPostMessageError("Estado OAuth invalido o expirado"));
        }

        try {
            // 1. Intercambiar código por Access Token
            String tokenUrl = "https://oauth2.googleapis.com/token";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("code", code);
            form.add("client_id", googleClientId);
            form.add("client_secret", googleClientSecret);
            form.add("redirect_uri", googleRedirectUriLogin);
            form.add("grant_type", "authorization_code");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);
            ResponseEntity<String> tokenResp =
                    restTemplate.postForEntity(tokenUrl, request, String.class);
            if (!tokenResp.getStatusCode().is2xxSuccessful() || tokenResp.getBody() == null) {
                return ResponseEntity.ok()
                        .contentType(MediaType.TEXT_HTML)
                        .body(htmlPostMessageError("Error obteniendo token"));
            }

            JsonNode tokenJson = objectMapper.readTree(tokenResp.getBody());
            String accessToken =
                    tokenJson.hasNonNull("access_token")
                            ? tokenJson.get("access_token").asText()
                            : null;

            if (accessToken == null) {
                return ResponseEntity.ok()
                        .contentType(MediaType.TEXT_HTML)
                        .body(htmlPostMessageError("Google no envio el access_token"));
            }

            // 2. Obtener Perfil de Usuario
            String userInfoUrl = "https://www.googleapis.com/oauth2/v3/userinfo";
            HttpHeaders uiHeaders = new HttpHeaders();
            uiHeaders.setBearerAuth(accessToken);
            HttpEntity<Void> uiReq = new HttpEntity<>(uiHeaders);

            ResponseEntity<String> uiResp =
                    restTemplate.exchange(userInfoUrl, HttpMethod.GET, uiReq, String.class);
            if (!uiResp.getStatusCode().is2xxSuccessful() || uiResp.getBody() == null) {
                return ResponseEntity.ok()
                        .contentType(MediaType.TEXT_HTML)
                        .body(htmlPostMessageError("Error obteniendo info de usuario"));
            }

            JsonNode uiJson = objectMapper.readTree(uiResp.getBody());
            String googleId = uiJson.hasNonNull("sub") ? uiJson.get("sub").asText() : null;
            String email = uiJson.hasNonNull("email") ? uiJson.get("email").asText() : null;

            if (googleId == null || email == null) {
                return ResponseEntity.ok()
                        .contentType(MediaType.TEXT_HTML)
                        .body(htmlPostMessageError("Perfil de Google incompleto"));
            }

            // 3. Evaluar el fujo
            if (flowData.startsWith("link:")) {
                Long userId = Long.parseLong(flowData.split(":")[1]);
                Usuario usuario = usuarioRepository.findById(userId).orElseThrow();
                usuarioRepository
                        .findByGoogleId(googleId)
                        .ifPresent(
                                other -> {
                                    if (!other.getId().equals(usuario.getId())) {
                                        throw new RuntimeException(
                                                "Este Google account ya está vinculado a otra"
                                                        + " cuenta");
                                    }
                                });
                if (!email.equalsIgnoreCase(usuario.getEmail())) {
                    throw new RuntimeException(
                            "El email del Google account no coincide con la cuenta actual");
                }
                usuario.setGoogleId(googleId);
                usuarioRepository.save(usuario);

                String html =
                        "<html><head><meta charset=\"UTF-8\"></head><body><script>"
                                + "window.opener.postMessage({ type: 'google-link-success' }, '*');"
                                + "window.close();</script></body></html>";
                return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);

            } else {
                // Es "login"
                Usuario usuario = usuarioRepository.findByGoogleId(googleId).orElse(null);
                if (usuario == null) {
                    Usuario byEmail = usuarioRepository.findByEmail(email).orElse(null);
                    if (byEmail != null) {
                        return ResponseEntity.ok()
                                .contentType(MediaType.TEXT_HTML)
                                .body(
                                        htmlPostMessageError(
                                                "Existe una cuenta con este email. Inicia sesion y"
                                                        + " vincula la cuenta desde ajustes."));
                    }
                    // No permitir login con Google si no existe cuenta registrada
                    return ResponseEntity.ok()
                            .contentType(MediaType.TEXT_HTML)
                            .body(
                                    htmlPostMessageError(
                                            "No existe una cuenta con este email. Regístrate"
                                                    + " primero y vincula tu cuenta de Google"
                                                    + " desde ajustes."));
                }

                if (Boolean.TRUE.equals(usuario.getAutenticacionDosFactores())) {
                    String tempToken = UUID.randomUUID().toString();
                    tempLoginStore.put(
                            tempToken,
                            new TempLogin(usuario.getId(), Instant.now().plusSeconds(300)));

                    String payloadStr =
                            "{\"isTwoFactor\": true, \"tempToken\": \"" + tempToken + "\"}";
                    String html =
                            "<html><head><meta charset=\"UTF-8\"></head>"
                                    + "<body><script>window.opener.postMessage({ type:"
                                    + " 'google-auth-success', payload: "
                                    + payloadStr
                                    + " }, '*');"
                                    + "window.close();</script></body></html>";
                    return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
                }

                String finalJwt = jwtService.generateToken(usuario.getEmail());
                AuthResponse authRes = buildAuthResponse(usuario, finalJwt);
                String payloadStr = objectMapper.writeValueAsString(authRes);

                String html =
                        "<html><head><meta charset=\"UTF-8\"></head>"
                                + "<body><script>window.opener.postMessage({ type:"
                                + " 'google-auth-success', payload: "
                                + payloadStr
                                + " }, '*');"
                                + "window.close();</script></body></html>";
                return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
            }

        } catch (Exception e) {
            log.error("Google Auth Code Exchange error", e);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(
                            htmlPostMessageError(
                                    e.getMessage() != null
                                            ? e.getMessage()
                                            : "Error en el servidor"));
        }
    }

    private String htmlPostMessageError(String err) {
        String safe = err.replace("'", "\\'");
        return "<html><head><meta charset=\"UTF-8\"></head><body><script>"
                + "window.opener.postMessage({ type: 'google-auth-error', error: '"
                + safe
                + "' }, '*');"
                + "window.close();</script></body></html>";
    }

    /**
     * Inicia sesión usando un Google ID Token (MANTENIDO PARA COMPATIBILIDAD PARCIAL SI HACE FALTA
     * ALGUN DIA, AUNQUE NO SE USE CON EL NUEVO FLUJO).
     */
    @Transactional
    public GoogleAuthResponse iniciarSesionConGoogle(final GoogleAuthRequest request) {
        if (request == null || request.getIdToken() == null || request.getIdToken().isBlank()) {
            throw new ValidationException("idToken de Google requerido");
        }

        try {
            String sanitizedClientId = googleClientId != null ? googleClientId.trim() : "";
            log.info("Login Google: Verificando ID Token con Client ID: '{}'", sanitizedClientId);

            GoogleIdTokenVerifier verifier =
                    new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                            .setAudience(java.util.Collections.singletonList(sanitizedClientId))
                            .build();

            GoogleIdToken idToken = verifier.verify(request.getIdToken());
            if (idToken == null) {
                try {
                    GoogleIdToken unverifiedToken =
                            GoogleIdToken.parse(new GsonFactory(), request.getIdToken());
                    log.error(
                            "Token fail. Audience del token: {}",
                            unverifiedToken.getPayload().getAudience());
                    log.error("Issuer del token: {}", unverifiedToken.getPayload().getIssuer());
                } catch (Exception e) {
                    // Ignored
                }
                throw new ValidationException("ID token de Google inválido");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String googleId = payload.getSubject();
            String email = payload.getEmail();
            boolean emailVerified = Boolean.TRUE.equals(payload.getEmailVerified());
            String name = (String) payload.get("name");

            // Buscar por googleId
            Usuario usuario = usuarioRepository.findByGoogleId(googleId).orElse(null);

            if (usuario == null && email != null) {
                // Si existe una cuenta con el mismo email, no la vinculamos automáticamente;
                // exigimos acción explícita
                Usuario byEmail = usuarioRepository.findByEmail(email).orElse(null);
                if (byEmail != null) {
                    throw new ConflictException(
                            "Existe una cuenta con este email. Inicia sesión y vincula la cuenta"
                                    + " desde ajustes.");
                }
            }

            if (usuario == null) {
                // Crear nuevo usuario
                Usuario nuevo = new Usuario();
                nuevo.setEmail(email);
                // Generar password aleatoria (no se usará para login por Google)
                String randomPwd = generarContrasenaSegura(12);
                nuevo.setPassword(passwordEncoder.encode(randomPwd));
                nuevo.setNombre(name != null ? name : "");
                nuevo.setEsTutor(false);
                nuevo.setVisibleEnListados(true);
                nuevo.setAutenticacionDosFactores(false);
                nuevo.setNotificacionesEmail(true);
                nuevo.setNotificacionesPush(true);
                nuevo.setIntereses(new java.util.ArrayList<>());
                nuevo.setEmailVerificado(emailVerified);
                nuevo.setVerificationToken(null);
                nuevo.setTokenExpiration(null);
                nuevo.setGoogleId(googleId);

                usuario = usuarioRepository.save(nuevo);
            } else {
                // Si existe pero no tiene googleId, asociarlo
                if (usuario.getGoogleId() == null || usuario.getGoogleId().isBlank()) {
                    usuario.setGoogleId(googleId);
                }
                if (!Boolean.TRUE.equals(usuario.getEmailVerificado())) {
                    usuario.setEmailVerificado(emailVerified);
                }
                usuarioRepository.save(usuario);
            }

            final String token = jwtService.generateToken(usuario.getEmail());

            AuthResponse authResp = buildAuthResponse(usuario, token);

            boolean requestClassroom = Boolean.TRUE.equals(request.getRequestClassroomAccess());

            return new GoogleAuthResponse(authResp, requestClassroom);

        } catch (ValidationException ve) {
            throw ve;
        } catch (Exception e) {
            throw new ValidationException("Error verificando ID token de Google", e);
        }
    }

    /**
     * Genera una contraseña aleatoria segura con la longitud especificada.
     *
     * @param length longitud de la contraseña
     * @return contraseña aleatoria con mayúsculas, minúsculas y dígitos
     */
    private String generarContrasenaSegura(final int length) {
        final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        final java.security.SecureRandom random = new java.security.SecureRandom();
        final StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * Solicita la recuperación de contraseña para un usuario. Genera un token JWT de corta duración
     * y envía un enlace de restablecimiento por email.
     *
     * @param request DTO con el email del usuario
     * @return MessageResponse con confirmación (siempre genérica por seguridad)
     */
    public MessageResponse recuperarContrasena(final ForgotPasswordRequest request) {
        final String email = request.getEmail().toLowerCase().trim();
        final MessageResponse genericResponse =
                MessageResponse.builder()
                        .message(
                                "Si el email existe en el sistema, recibirás instrucciones "
                                        + "de recuperación de contraseña en tu bandeja de entrada")
                        .build();

        // Rate limiting: máximo PASSWORD_RESET_MAX_REQUESTS solicitudes por email
        // en una ventana de PASSWORD_RESET_WINDOW_MS
        final long now = System.currentTimeMillis();
        final java.util.concurrent.CopyOnWriteArrayList<Long> attempts =
                passwordResetAttempts.computeIfAbsent(
                        email, k -> new java.util.concurrent.CopyOnWriteArrayList<>());

        // Eliminar intentos fuera de la ventana
        attempts.removeIf(ts -> now - ts > PASSWORD_RESET_WINDOW_MS);

        // Evitar acumulación de entradas vacías en memoria
        if (attempts.isEmpty()) {
            passwordResetAttempts.remove(email);
        }

        if (attempts.size() >= PASSWORD_RESET_MAX_REQUESTS) {
            log.warn("Rate limit alcanzado para recuperación de contraseña: {}", email);
            // Devolver respuesta genérica para no revelar que se ha bloqueado
            return genericResponse;
        }
        passwordResetAttempts
                .computeIfAbsent(email, k -> new java.util.concurrent.CopyOnWriteArrayList<>())
                .add(now);

        try {
            final Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);

            // Respuesta genérica aunque el email no exista (evita enumeración de usuarios)
            if (usuario == null) {
                return genericResponse;
            }

            // Generar token JWT de restablecimiento (15 min de expiración)
            final String resetToken = jwtService.generatePasswordResetToken(email);

            // Construir enlace de restablecimiento
            final String resetLink = frontendUrl + "/reset-password?token=" + resetToken;

            // Enviar email con enlace
            emailService.sendPasswordResetEmail(usuario.getEmail(), usuario.getNombre(), resetLink);

            return genericResponse;

        } catch (Exception e) {
            log.error("Error al procesar solicitud de recuperación", e);
            // Devolver respuesta genérica para no revelar errores internos
            return genericResponse;
        }
    }

    /**
     * Restablece la contraseña usando un token JWT de recuperación.
     *
     * @param request DTO con el token y la nueva contraseña.
     * @return MessageResponse con confirmación.
     * @throws ValidationException si el token es inválido, ha expirado, o la contraseña no cumple
     *     requisitos.
     */
    @Transactional
    public MessageResponse restablecerContrasena(final ResetPasswordRequest request) {
        final String email;
        try {
            email = jwtService.validatePasswordResetToken(request.getToken());
        } catch (Exception e) {
            throw new ValidationException(
                    "El enlace de recuperación es inválido o ha expirado. Solicita uno nuevo.");
        }

        final Usuario usuario =
                usuarioRepository
                        .findByEmail(email)
                        .orElseThrow(
                                () ->
                                        new ValidationException(
                                                "El enlace de recuperación es inválido o ha"
                                                        + " expirado. Solicita uno nuevo."));

        // Rechazar token si la contraseña ya fue cambiada después de su emisión (single-use)
        if (usuario.getPasswordChangedAt() != null) {
            final java.util.Date tokenIssuedAt = jwtService.extractIssuedAt(request.getToken());
            final java.time.Instant passwordChangedInstant =
                    usuario.getPasswordChangedAt()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toInstant();
            if (tokenIssuedAt.toInstant().isBefore(passwordChangedInstant)) {
                throw new ValidationException(
                        "El enlace de recuperación es inválido o ha expirado. Solicita uno nuevo.");
            }
        }

        // Validar complejidad de la nueva contraseña
        final String newPassword = request.getNewPassword();
        if (newPassword == null || newPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException("La contraseña debe tener al menos 8 caracteres");
        }
        if (newPassword.length() > MAX_PASSWORD_LENGTH) {
            throw new ValidationException("La contraseña no puede tener más de 128 caracteres");
        }
        if (!newPassword.matches(".*[A-Z].*")
                || !newPassword.matches(".*[a-z].*")
                || !newPassword.matches(".*[0-9].*")) {
            throw new ValidationException(
                    "La contraseña debe contener mayúsculas, minúsculas y números");
        }

        usuario.setPassword(passwordEncoder.encode(newPassword));
        usuario.setPasswordChangedAt(LocalDateTime.now());
        usuarioRepository.save(usuario);

        return MessageResponse.builder()
                .message("Contraseña restablecida correctamente. Ya puedes iniciar sesión.")
                .build();
    }

    // ===============================
    // VERIFICACIÓN DE EMAIL
    // ===============================

    /**
     * Verifica el email de un usuario usando el token de verificación.
     *
     * @param token Token de verificación enviado por email.
     * @return AuthResponse con token JWT y datos del usuario si la verificación es exitosa.
     * @throws ValidationException si el token es inválido o ha expirado (400).
     */
    @Transactional
    public AuthResponse verificarEmail(final String token) {
        if (token == null || token.isBlank()) {
            throw new ValidationException("Token de verificación inválido");
        }

        final Usuario usuario =
                usuarioRepository
                        .findByVerificationToken(token)
                        .orElseThrow(
                                () ->
                                        new ValidationException(
                                                "Token de verificación inválido o ya utilizado"));

        // Verificar que el token no ha expirado
        if (usuario.getTokenExpiration() == null
                || LocalDateTime.now().isAfter(usuario.getTokenExpiration())) {
            throw new ValidationException(
                    "El token de verificación ha expirado. Solicita un nuevo email de"
                            + " verificación.");
        }

        // Marcar el email como verificado y limpiar el token
        usuario.setEmailVerificado(true);
        usuario.setVerificationToken(null);
        usuario.setTokenExpiration(null);
        usuarioRepository.save(usuario);

        // Generar JWT para que el usuario pueda iniciar sesión automáticamente
        final String jwtToken = jwtService.generateToken(usuario.getEmail());

        return buildAuthResponse(usuario, jwtToken);
    }

    /**
     * Reenvía el email de verificación a un usuario.
     *
     * @param email Email del usuario que solicita el reenvío.
     * @return MessageResponse con confirmación.
     * @throws ValidationException si el email no existe o ya está verificado (400).
     */
    @Transactional
    public MessageResponse reenviarVerificacion(final String email) {
        if (email == null || email.isBlank()) {
            throw new ValidationException("El email no puede estar vacío");
        }

        final Usuario usuario =
                usuarioRepository
                        .findByEmail(email)
                        .orElseThrow(
                                () ->
                                        new ValidationException(
                                                "No existe una cuenta con este email"));

        if (Boolean.TRUE.equals(usuario.getEmailVerificado())) {
            throw new ValidationException("Este email ya ha sido verificado");
        }

        // Generar nuevo token de verificación
        final String verificationToken = UUID.randomUUID().toString();
        final LocalDateTime tokenExpiration =
                LocalDateTime.now().plusHours(VERIFICATION_TOKEN_HOURS);

        usuario.setVerificationToken(verificationToken);
        usuario.setTokenExpiration(tokenExpiration);
        usuarioRepository.save(usuario);

        // Enviar email de verificación
        try {
            String verificationUrl = frontendUrl + "/verify-email";
            emailService.sendVerificationEmail(
                    usuario.getEmail(),
                    usuario.getNombre() != null ? usuario.getNombre() : "Usuario",
                    verificationToken,
                    verificationUrl);
        } catch (Exception e) {
            log.error("Error al reenviar email de verificación: {}", e.getMessage());
            throw new ValidationException(
                    "No se pudo enviar el email de verificación. Inténtalo de nuevo más tarde.");
        }

        return MessageResponse.builder()
                .message("Se ha enviado un nuevo email de verificación a " + email)
                .build();
    }

    // ===============================
    // MÉTODOS AUXILIARES
    // ===============================

    /**
     * Construye un {@link AuthResponse} a partir del usuario y token.
     *
     * <p>Mantiene la estructura del DTO existente con UserDetailResponse anidado.
     *
     * @param usuario Usuario autenticado.
     * @param token Token JWT generado.
     * @return AuthResponse completo con todos los datos del usuario.
     */
    private AuthResponse buildAuthResponse(final Usuario usuario, final String token) {

        final UserDetailResponse userDetail =
                UserDetailResponse.builder()
                        .id(usuario.getId())
                        .email(usuario.getEmail())
                        .nombre(usuario.getNombre())
                        .foto(usuario.getFoto())
                        .fotoBackgroundColor(usuario.getFotoBackgroundColor())
                        .bio(usuario.getBio())
                        .universidad(usuario.getUniversidad())
                        .grado(usuario.getGrado())
                        .nivelEstudios(usuario.getNivelEstudios())
                        .baseFormativa(usuario.getBaseFormativa())
                        .ubicacion(convertToUbicacionResponse(usuario.getUbicacion()))
                        .intereses(usuario.getIntereses())
                        .visibleEnListados(usuario.getVisibleEnListados())
                        .esTutor(usuario.getEsTutor())
                        .autenticacionDosFactores(usuario.getAutenticacionDosFactores())
                        .notificacionesPush(usuario.getNotificacionesPush())
                        .createdAt(usuario.getCreatedAt())
                        .googleLinked(usuario.getGoogleId() != null)
                        .build();

        return AuthResponse.builder().accessToken(token).user(userDetail).build();
    }

    /**
     * Convierte una entidad Ubicacion a su DTO correspondiente.
     *
     * @param ubicacion Entidad de ubicación.
     * @return UbicacionResponse DTO o null si la entrada es null.
     */
    private UbicacionResponse convertToUbicacionResponse(final Ubicacion ubicacion) {
        if (ubicacion == null) {
            return null;
        }
        return UbicacionResponse.builder()
                .id(ubicacion.getId())
                .nombre(ubicacion.getNombre())
                .direccion(ubicacion.getDireccion())
                .latitud(ubicacion.getLatitud())
                .longitud(ubicacion.getLongitud())
                .tipo(ubicacion.getTipo())
                .coste(ubicacion.getCoste())
                .build();
    }

    /**
     * Aplica defaults de preferencias cuando faltan en usuarios ya existentes.
     *
     * @param usuario Usuario sobre el que aplicar defaults.
     * @return true si se cambió algún valor.
     */
    private boolean applyPreferenceDefaultsIfNeeded(final Usuario usuario) {
        boolean changed = false;

        if (usuario.getVisibleEnListados() == null) {
            usuario.setVisibleEnListados(true);
            changed = true;
        }
        if (usuario.getAutenticacionDosFactores() == null) {
            usuario.setAutenticacionDosFactores(false);
            changed = true;
        }
        if (usuario.getNotificacionesPush() == null) {
            usuario.setNotificacionesPush(true);
            changed = true;
        }

        return changed;
    }
}
