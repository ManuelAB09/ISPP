package es.us.meerkat.backend.service;

import java.util.ArrayList;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.us.meerkat.backend.dto.AuthResponse;
import es.us.meerkat.backend.dto.ForgotPasswordRequest;
import es.us.meerkat.backend.dto.LoginRequest;
import es.us.meerkat.backend.dto.MessageResponse;
import es.us.meerkat.backend.dto.RegisterRequest;
import es.us.meerkat.backend.dto.UserDetailResponse;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.UsuarioRepository;
import es.us.meerkat.backend.security.JwtService;
import lombok.RequiredArgsConstructor;

/**
 * Servicio de autenticación.
 *
 * <p>Gestiona el registro de nuevos usuarios y el inicio de sesión, generando tokens JWT reales
 * mediante {@link JwtService}. Corresponde a los endpoints POST /api/v1/auth/register y POST
 * /api/v1/auth/login del OpenAPI.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    /** Longitud mínima requerida para las contraseñas. */
    private static final int MIN_PASSWORD_LENGTH = 8;

    /** Repositorio para acceder a la información de usuarios. */
    private final UsuarioRepository usuarioRepository;

    /** Codificador de contraseñas BCrypt. */
    private final BCryptPasswordEncoder passwordEncoder;

    /** Servicio para generación y validación de tokens JWT. */
    private final JwtService jwtService;

    /** Servicio de correo electrónico * */
    private final EmailService emailService;

    // ===============================
    // REGISTRO
    // ===============================

    /**
     * Registra un nuevo usuario con email y contraseña.
     *
     * <p>Valida que el email sea único y que la contraseña tenga al menos 8 caracteres. Devuelve un
     * token JWT listo para usar.
     *
     * @param requestParam Datos del nuevo usuario.
     * @return AuthResponse con token JWT y datos del usuario.
     * @throws RuntimeException si el email ya está en uso o los datos no son válidos.
     */
    @Transactional
    public AuthResponse registrar(final RegisterRequest requestParam) {

        if (requestParam.getEmail() == null || requestParam.getEmail().isBlank()) {
            throw new RuntimeException("El email no puede estar vacío");
        }

        if (requestParam.getPassword() == null
                || requestParam.getPassword().length() < MIN_PASSWORD_LENGTH) {
            throw new RuntimeException("La contraseña debe tener al menos 8 caracteres");
        }

        if (usuarioRepository.existsByEmail(requestParam.getEmail())) {
            throw new RuntimeException("El email ya está registrado");
        }

        final Usuario usuario = new Usuario();
        usuario.setEmail(requestParam.getEmail());
        usuario.setPassword(passwordEncoder.encode(requestParam.getPassword()));
        usuario.setNombre(requestParam.getNombre());
        usuario.setEsTutor(false);
        usuario.setVisibleEnListados(true);
        usuario.setIntereses(new ArrayList<>());

        usuarioRepository.save(usuario);

        final String token = jwtService.generateToken(usuario.getEmail());

        return buildAuthResponse(usuario, token);
    }

    // ===============================
    // INICIO DE SESIÓN
    // ===============================

    /**
     * Autentica a un usuario con sus credenciales.
     *
     * <p>Verifica que el email exista y que la contraseña coincida con la almacenada cifrada.
     * Devuelve un token JWT válido.
     *
     * @param requestParam Credenciales del usuario.
     * @return AuthResponse con token JWT y datos del usuario.
     * @throws RuntimeException si las credenciales son incorrectas.
     */
    public AuthResponse iniciarSesion(final LoginRequest requestParam) {

        final Usuario usuario =
                usuarioRepository
                        .findByEmail(requestParam.getEmail())
                        .orElseThrow(() -> new RuntimeException("Credenciales incorrectas"));

        if (!passwordEncoder.matches(requestParam.getPassword(), usuario.getPassword())) {
            throw new RuntimeException("Credenciales incorrectas");
        }

        final String token = jwtService.generateToken(usuario.getEmail());

        return buildAuthResponse(usuario, token);
    }

    /**
     * Solicita la recuperación de contraseña para un usuario.
     *
     * @param request DTO con el email del usuario
     * @return MessageResponse con confirmación
     * @throws NotFoundException
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
            throw new RuntimeException("No se pudo enviar el email de recuperación", e);
        }
    }

    /** Genera una contraseña segura aleatoría. */
    private String generarContrasenaSegura(final int length) {
        return RandomStringUtils.randomAlphanumeric(length).toUpperCase()
                + RandomStringUtils.randomNumeric(2);
    }

    // ===============================
    // MÉTODOS AUXILIARES
    // ===============================

    /**
     * Construye un {@link AuthResponse} a partir del usuario y token.
     *
     * @param usuario Usuario autenticado.
     * @param token Token JWT generado.
     * @return AuthResponse completo.
     */
    private AuthResponse buildAuthResponse(final Usuario usuario, final String token) {

        final UserDetailResponse userDetail =
                UserDetailResponse.builder()
                        .id(usuario.getId())
                        .email(usuario.getEmail())
                        .nombre(usuario.getNombre())
                        .foto(usuario.getFoto())
                        .bio(usuario.getBio())
                        .intereses(usuario.getIntereses())
                        .visibleEnListados(usuario.getVisibleEnListados())
                        .esTutor(usuario.getEsTutor())
                        .createdAt(usuario.getCreatedAt())
                        .build();

        return AuthResponse.builder().accessToken(token).user(userDetail).build();
    }
}
