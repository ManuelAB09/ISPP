package es.us.meerkat.backend.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.us.meerkat.backend.dto.ChangePasswordRequest;
import es.us.meerkat.backend.dto.UpdateUserRequest;
import es.us.meerkat.backend.dto.UserDetailResponse;
import es.us.meerkat.backend.dto.UserPublicResponse;
import es.us.meerkat.backend.dto.VisibilityRequest;
import es.us.meerkat.backend.entity.Ubicacion;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.UbicacionRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

/**
 * Servicio para gestionar la lógica de negocio de usuarios.
 *
 * <p>Cubre los endpoints de /api/v1/users del OpenAPI: obtener perfil propio, actualizar, cambiar
 * contraseña, eliminar cuenta, visibilidad y ver perfiles públicos.
 */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    /** Longitud mínima requerida para las contraseñas. */
    private static final int MIN_PASSWORD_LENGTH = 8;

    /** Repositorio para acceder a la información de usuarios. */
    private final UsuarioRepository usuarioRepository;

    /** Repositorio para acceder a la información de ubicaciones. */
    private final UbicacionRepository ubicacionRepository;

    /** Codificador de contraseñas BCrypt. */
    private final BCryptPasswordEncoder passwordEncoder;

    // ===============================
    // GET /api/v1/users/me
    // ===============================

    /**
     * Devuelve el perfil completo del usuario autenticado.
     *
     * @param usuario Usuario autenticado extraído del contexto.
     * @return Perfil completo del usuario.
     */
    @Transactional
    public UserDetailResponse obtenerPerfilPropio(final Usuario usuario) {
        Usuario usuarioActualizado =
                usuarioRepository
                        .findByEmail(usuario.getEmail())
                        .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        if (usuarioActualizado.getTutores() != null) {
            usuarioActualizado.getTutores().size();
        }
        return mapToDetailResponse(usuarioActualizado);
    }

    // ===============================
    // PUT /api/v1/users/me
    // ===============================

    /**
     * Actualiza la información personal del usuario autenticado.
     *
     * <p>Solo modifica los campos que no sean nulos en el request.
     *
     * @param usuario Usuario autenticado.
     * @param requestParam Datos a actualizar.
     * @return Perfil actualizado.
     */
    @Transactional
    public UserDetailResponse actualizarPerfil(
            final Usuario usuario, final UpdateUserRequest requestParam) {

        if (requestParam.getNombre() != null) {
            usuario.setNombre(requestParam.getNombre());
        }
        if (requestParam.getFoto() != null) {
            usuario.setFoto(requestParam.getFoto());
        }
        if (requestParam.getBio() != null) {
            usuario.setBio(requestParam.getBio());
        }
        if (requestParam.getIntereses() != null) {
            usuario.setIntereses(requestParam.getIntereses());
        }

        if (requestParam.getUniversidad() != null) {
            usuario.setUniversidad(requestParam.getUniversidad());
        }
        if (requestParam.getGrado() != null) {
            usuario.setGrado(requestParam.getGrado());
        }
        if (requestParam.getUbicacion() != null) {
            Ubicacion ubicacion = requestParam.getUbicacion();
            if (ubicacion.getId() != null && ubicacion.getId() == 0) {
                ubicacion.setId(null);
            }
            ubicacion = ubicacionRepository.save(ubicacion);
            usuario.setUbicacion(ubicacion);
        }

        usuarioRepository.save(usuario);
        return mapToDetailResponse(usuario);
    }

    // ===============================
    // DELETE /api/v1/users/me
    // ===============================

    /**
     * Elimina permanentemente la cuenta del usuario autenticado.
     *
     * <p>Esta acción es irreversible. El frontend debe mostrar confirmación antes de llamar a este
     * endpoint.
     *
     * @param usuario Usuario autenticado a eliminar.
     */
    @Transactional
    public void eliminarCuenta(final Usuario usuario) {
        usuarioRepository.delete(usuario);
    }

    // ===============================
    // PUT /api/v1/users/me/password
    // ===============================

    /**
     * Cambia la contraseña del usuario autenticado.
     *
     * <p>Verifica la contraseña actual antes de aplicar el cambio.
     *
     * @param usuario Usuario autenticado.
     * @param requestParam Contraseña actual y nueva.
     * @throws RuntimeException si la contraseña actual es incorrecta o la nueva no cumple los
     *     requisitos.
     */
    @Transactional
    public void cambiarPassword(final Usuario usuario, final ChangePasswordRequest requestParam) {

        if (!passwordEncoder.matches(requestParam.getCurrentPassword(), usuario.getPassword())) {
            throw new RuntimeException("La contraseña actual es incorrecta");
        }

        if (requestParam.getNewPassword() == null
                || requestParam.getNewPassword().length() < MIN_PASSWORD_LENGTH) {
            throw new RuntimeException("La nueva contraseña debe tener " + "al menos 8 caracteres");
        }

        usuario.setPassword(passwordEncoder.encode(requestParam.getNewPassword()));
        usuarioRepository.save(usuario);
    }

    // ===============================
    // PUT /api/v1/users/me/visibility
    // ===============================

    /**
     * Actualiza la visibilidad del perfil en listados públicos.
     *
     * @param usuario Usuario autenticado.
     * @param requestParam Nueva configuración de visibilidad.
     * @return Perfil actualizado.
     */
    @Transactional
    public UserDetailResponse actualizarVisibilidad(
            final Usuario usuario, final VisibilityRequest requestParam) {

        if (requestParam.getVisibleEnListados() != null) {
            usuario.setVisibleEnListados(requestParam.getVisibleEnListados());
            usuarioRepository.save(usuario);
        }

        return mapToDetailResponse(usuario);
    }

    // ===============================
    // GET /api/v1/users/{userId}
    // ===============================

    /**
     * Devuelve el perfil público de un usuario por su ID.
     *
     * <p>Solo expone datos que el usuario ha hecho públicos.
     *
     * @param usuarioId Identificador del usuario.
     * @return Perfil público del usuario.
     * @throws RuntimeException si el usuario no existe.
     */
    public UserPublicResponse obtenerPerfilPublico(final Long usuarioId) {

        final Usuario usuario =
                usuarioRepository
                        .findById(usuarioId)
                        .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return mapToPublicResponse(usuario);
    }

    // ===============================
    // MÉTODOS AUXILIARES
    // ===============================

    /**
     * Mapea {@link Usuario} a {@link UserDetailResponse}.
     *
     * @param usuario Usuario a mapear.
     * @return DTO con datos completos del usuario.
     */
    private UserDetailResponse mapToDetailResponse(final Usuario usuario) {
        return UserDetailResponse.builder()
                .id(usuario.getId())
                .email(usuario.getEmail())
                .nombre(usuario.getNombre())
                .foto(usuario.getFoto())
                .bio(usuario.getBio())
                .universidad(usuario.getUniversidad())
                .grado(usuario.getGrado())
                .ubicacion(usuario.getUbicacion())
                .intereses(usuario.getIntereses())
                .visibleEnListados(usuario.getVisibleEnListados())
                .esTutor(usuario.getEsTutor())
                .createdAt(usuario.getCreatedAt())
                .build();
    }

    /**
     * Mapea {@link Usuario} a {@link UserPublicResponse}.
     *
     * @param usuario Usuario a mapear.
     * @return DTO con datos públicos del usuario.
     */
    private UserPublicResponse mapToPublicResponse(final Usuario usuario) {
        return UserPublicResponse.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .foto(usuario.getFoto())
                .bio(usuario.getBio())
                .intereses(usuario.getIntereses())
                .esTutor(usuario.getEsTutor())
                .build();
    }
}
