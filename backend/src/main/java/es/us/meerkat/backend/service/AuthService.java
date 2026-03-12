package es.us.meerkat.backend.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.us.meerkat.backend.dto.AuthResponse;
import es.us.meerkat.backend.dto.ForgotPasswordRequest;
import es.us.meerkat.backend.dto.LoginRequest;
import es.us.meerkat.backend.dto.MessageResponse;
import es.us.meerkat.backend.dto.RegisterRequest;
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

        // Verificar que el email esté verificado
        if (!Boolean.TRUE.equals(usuario.getEmailVerificado())) {
            throw new EmailNotVerifiedException(
                    "Debes verificar tu email antes de iniciar sesión. Revisa tu bandeja de entrada"
                            + " o solicita un nuevo email de verificación.");
        }

        if (applyPreferenceDefaultsIfNeeded(usuario)) {
            usuarioRepository.save(usuario);
        }

        final String token = jwtService.generateToken(usuario.getEmail());

        return buildAuthResponse(usuario, token);
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
