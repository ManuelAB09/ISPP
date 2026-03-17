package es.us.meerkat.backend.service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import es.us.meerkat.backend.dto.AuthResponse;
import es.us.meerkat.backend.dto.ForgotPasswordRequest;
import es.us.meerkat.backend.dto.GoogleAuthRequest;
import es.us.meerkat.backend.dto.GoogleAuthResponse;
import es.us.meerkat.backend.dto.LoginRequest;
import es.us.meerkat.backend.dto.MessageResponse;
import es.us.meerkat.backend.dto.RegisterRequest;
import es.us.meerkat.backend.dto.TotpSetupResponse;
import es.us.meerkat.backend.dto.TwoFactorChallengeResponse;
import es.us.meerkat.backend.dto.UbicacionResponse;
import es.us.meerkat.backend.dto.UserDetailResponse;
import es.us.meerkat.backend.entity.Ubicacion;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.exception.ConflictException;
import es.us.meerkat.backend.exception.EmailNotVerifiedException;
import es.us.meerkat.backend.exception.ValidationException;
import es.us.meerkat.backend.repository.UsuarioRepository;
import es.us.meerkat.backend.security.JwtService;
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

    /** Horas de validez del token de verificación. */
    private static final int VERIFICATION_TOKEN_HOURS = 24;

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

    private final GoogleClassroomService googleClassroomService;

    @Value("${google.classroom.client-id:}")
    private String googleClientId;

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
        usuario.setNotificacionesEmail(true);
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

        // Validar formato de email
        final String emailRegex = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";
        if (!request.getEmail().matches(emailRegex)) {
            throw new ValidationException("El formato del email no es válido");
        }

        if (request.getNombre() == null || request.getNombre().isBlank()) {
            throw new ValidationException("El nombre no puede estar vacío");
        }

        if (request.getPassword() == null || request.getPassword().length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException("La contraseña debe tener al menos 8 caracteres");
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
    public AuthResponse iniciarSesion(final LoginRequest requestParam) {

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
            return (AuthResponse) (Object) new TwoFactorChallengeResponse(true, tempToken);
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
        if (token == null || token.isBlank()) return null;
        TempLogin tl = tempLoginStore.remove(token);
        if (tl == null) return null;
        if (Instant.now().isAfter(tl.expiresAt())) return null;
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
        if (!verifyTotpCode(usuario.getTotpSecret(), code)) {
            throw new ValidationException("Código 2FA inválido");
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
            GoogleIdTokenVerifier verifier =
                    new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                            .setAudience(java.util.Collections.singletonList(googleClientId))
                            .build();

            GoogleIdToken idToken = verifier.verify(request.getIdToken());
            if (idToken == null) {
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

    public MessageResponse enableTotpForCurrentUser(final String code) {
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

        usuario.setTotpSecret(temp);
        usuario.setTotpTempSecret(null);
        usuario.setAutenticacionDosFactores(true);
        usuarioRepository.save(usuario);

        return MessageResponse.builder().message("2FA activado").build();
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
        if (!verifyTotpCode(secret, code)) {
            throw new ValidationException("Código 2FA inválido");
        }

        usuario.setTotpSecret(null);
        usuario.setAutenticacionDosFactores(false);
        usuarioRepository.save(usuario);

        return MessageResponse.builder().message("2FA desactivado").build();
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
        if (code == null || code.isBlank()) return false;
        byte[] key = base32Decode(base32Secret);
        long timeWindow = System.currentTimeMillis() / 1000L / 30L;
        for (int i = -1; i <= 1; i++) {
            long t = timeWindow + i;
            String generated = generateTotpForCounter(key, t);
            if (generated.equals(code)) return true;
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
                if (index == 0) i++;
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
            if (val < 0) continue;
            buffer <<= 5;
            buffer |= val & 0x1F;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                result[count++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }
        if (count == result.length) return result;
        byte[] truncated = new byte[count];
        System.arraycopy(result, 0, truncated, 0, count);
        return truncated;
    }

    /**
     * Inicia sesión usando un Google ID Token (Sign-in with Google). Si el usuario no existe, se
     * crea automáticamente. Si existe una cuenta con el mismo email, se vincula el googleId.
     *
     * @param request GoogleAuthRequest con idToken y flag requestClassroomAccess
     * @return AuthResponse con JWT y datos del usuario.
     */
    @Transactional
    public GoogleAuthResponse iniciarSesionConGoogle(final GoogleAuthRequest request) {
        if (request == null || request.getIdToken() == null || request.getIdToken().isBlank()) {
            throw new ValidationException("idToken de Google requerido");
        }

        try {
            GoogleIdTokenVerifier verifier =
                    new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                            .setAudience(java.util.Collections.singletonList(googleClientId))
                            .build();

            GoogleIdToken idToken = verifier.verify(request.getIdToken());
            if (idToken == null) {
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
     * Solicita la recuperación de contraseña para un usuario.
     *
     * @param request DTO con el email del usuario
     * @return MessageResponse con confirmación
     * @throws ValidationException si hay error al procesar la solicitud (400).
     */
    public MessageResponse recuperarContrasena(final ForgotPasswordRequest request) {
        final String email = request.getEmail();

        try {
            Usuario usuario =
                    usuarioRepository
                            .findByEmail(email)
                            .orElseThrow(
                                    () -> {
                                        // log.warn("Email no existe: {}", email);
                                        return new NotFoundException();
                                    });

            // Generar contraseña temporal segura
            final String temporaryPassword = generarContrasenaSegura(12);

            // Guardar contraseña temporal codificada
            usuario.setPassword(passwordEncoder.encode(temporaryPassword));
            usuarioRepository.save(usuario);

            // Enviar email
            emailService.sendPasswordResetEmail(
                    usuario.getEmail(), usuario.getNombre(), temporaryPassword);

            return MessageResponse.builder()
                    .message(
                            "Si el email existe en el sistema, recibirás instrucciones "
                                    + "de recuperación de contraseña en tu bandeja de entrada")
                    .build();

        } catch (Exception e) {
            // log.error("Error al enviar email de recuperación: {}", e.getMessage());
            throw new ValidationException("No se pudo enviar el email de recuperación", e);
        }
    }

    /** Genera una contraseña segura aleatoría. */
    private String generarContrasenaSegura(final int length) {
        return RandomStringUtils.randomAlphanumeric(length).toUpperCase()
                + RandomStringUtils.randomNumeric(2);
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
                        .notificacionesEmail(usuario.getNotificacionesEmail())
                        .notificacionesPush(usuario.getNotificacionesPush())
                        .createdAt(usuario.getCreatedAt())
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
        if (usuario.getNotificacionesEmail() == null) {
            usuario.setNotificacionesEmail(true);
            changed = true;
        }
        if (usuario.getNotificacionesPush() == null) {
            usuario.setNotificacionesPush(true);
            changed = true;
        }

        return changed;
    }
}
