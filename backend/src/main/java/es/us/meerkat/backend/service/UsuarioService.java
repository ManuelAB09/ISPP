package es.us.meerkat.backend.service;

import java.util.List;

import es.us.meerkat.backend.dto.AuthResponse;
import es.us.meerkat.backend.dto.CambiarPasswordRequest;
import es.us.meerkat.backend.dto.LoginRequest;
import es.us.meerkat.backend.dto.PrivacidadRequest;
import es.us.meerkat.backend.dto.RegisterRequest;
import es.us.meerkat.backend.dto.UpdatePerfilRequest;
import es.us.meerkat.backend.dto.UsuarioPerfilResponse;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio para gestionar la lógica de negocio relacionada con los usuarios.
 *
 * Cubre registro, autenticación, edición de perfil, cambio de contraseña,
 * eliminación de cuenta y configuración de privacidad.
 */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    /** Longitud mínima requerida para las contraseñas. */
    private static final int MIN_PASSWORD_LENGTH = 8;

    /** Repositorio para acceder a la información de usuarios. */
    private final UsuarioRepository usuarioRepository;

    /** Codificador de contraseñas BCrypt. */
    private final BCryptPasswordEncoder passwordEncoder;

    // ===============================
    // REGISTRO
    // ===============================

    /**
     * Registra un nuevo usuario con email y contraseña.
     *
     * Verifica que el email no esté ya en uso y que la contraseña
     * tenga al menos 8 caracteres antes de guardar.
     *
     * @param requestParam Datos del nuevo usuario.
     * @return Mensaje de confirmación de registro.
     * @throws RuntimeException si el email ya está en uso
     *         o los datos no son válidos.
     */
    @Transactional
    public String registrar(final RegisterRequest requestParam) {

        if (requestParam.getEmail() == null
                || requestParam.getEmail().isBlank()) {
            throw new RuntimeException("El email no puede estar vacío");
        }

        if (requestParam.getPassword() == null
                || requestParam.getPassword().length()
                < MIN_PASSWORD_LENGTH) {
            throw new RuntimeException(
                "La contraseña debe tener al menos 8 caracteres");
        }

        if (usuarioRepository.existsByEmail(requestParam.getEmail())) {
            throw new RuntimeException("El email ya está en uso");
        }

        final Usuario usuario = new Usuario();
        usuario.setEmail(requestParam.getEmail());
        usuario.setPassword(
            passwordEncoder.encode(requestParam.getPassword()));
        usuario.setNombre(requestParam.getNombre());
        usuario.setEsTutor(false);
        usuario.setVisibleEnListados(true);

        usuarioRepository.save(usuario);

        return "Cuenta creada correctamente";
    }

    // ===============================
    // INICIO DE SESIÓN
    // ===============================

    /**
     * Autentica a un usuario con sus credenciales.
     *
     * Comprueba que el email exista y que la contraseña coincida
     * con la almacenada cifrada en base de datos.
     *
     * @param requestParam Credenciales del usuario.
     * @return DTO con los datos básicos del usuario autenticado.
     * @throws RuntimeException si las credenciales son incorrectas.
     */
    public AuthResponse iniciarSesion(final LoginRequest requestParam) {

        final Usuario usuario = usuarioRepository
                .findByEmail(requestParam.getEmail())
                .orElseThrow(
                    () -> new RuntimeException("Credenciales incorrectas"));

        if (!passwordEncoder.matches(
                requestParam.getPassword(), usuario.getPassword())) {
            throw new RuntimeException("Credenciales incorrectas");
        }

        return AuthResponse.builder()
                .id(usuario.getId())
                .email(usuario.getEmail())
                .nombre(usuario.getNombre())
                .esTutor(usuario.getEsTutor())
                .token("jwt-generado-por-spring-security")
                .build();
    }

    // ===============================
    // ACTUALIZAR PERFIL
    // ===============================

    /**
     * Actualiza la información personal del usuario autenticado.
     *
     * Solo se modifican los campos que no sean nulos en el request.
     * Los cambios se reflejan inmediatamente en el perfil público.
     *
     * @param usuarioIdParam Identificador del usuario a actualizar.
     * @param requestParam   Datos nuevos del perfil.
     * @return Perfil público actualizado del usuario.
     */
    @Transactional
    public UsuarioPerfilResponse actualizarPerfil(
            final Long usuarioIdParam,
            final UpdatePerfilRequest requestParam) {

        final Usuario usuario = usuarioRepository.findById(usuarioIdParam)
                .orElseThrow(
                    () -> new RuntimeException("Usuario no encontrado"));

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

        usuarioRepository.save(usuario);

        return mapToPerfilResponse(usuario);
    }

    // ===============================
    // CAMBIAR CONTRASEÑA
    // ===============================

    /**
     * Permite al usuario autenticado modificar su contraseña.
     *
     * Verifica la contraseña actual, comprueba que la nueva y su
     * confirmación coincidan, y la almacena cifrada.
     *
     * @param usuarioIdParam Identificador del usuario.
     * @param requestParam   Datos del cambio de contraseña.
     * @return Mensaje de éxito.
     * @throws RuntimeException si la contraseña actual es incorrecta,
     *         la nueva no cumple requisitos o las confirmaciones
     *         no coinciden.
     */
    @Transactional
    public String cambiarPassword(
            final Long usuarioIdParam,
            final CambiarPasswordRequest requestParam) {

        final Usuario usuario = usuarioRepository.findById(usuarioIdParam)
                .orElseThrow(
                    () -> new RuntimeException("Usuario no encontrado"));

        if (!passwordEncoder.matches(
                requestParam.getPasswordActual(), usuario.getPassword())) {
            throw new RuntimeException(
                "La contraseña actual es incorrecta");
        }

        if (requestParam.getPasswordNueva() == null
                || requestParam.getPasswordNueva().length()
                < MIN_PASSWORD_LENGTH) {
            throw new RuntimeException(
                "La nueva contraseña debe tener al menos 8 caracteres");
        }

        if (!requestParam.getPasswordNueva()
                .equals(requestParam.getPasswordConfirmacion())) {
            throw new RuntimeException(
                "La nueva contraseña y su confirmación no coinciden");
        }

        usuario.setPassword(
            passwordEncoder.encode(requestParam.getPasswordNueva()));
        usuarioRepository.save(usuario);

        return "Contraseña actualizada correctamente";
    }

    // ===============================
    // ELIMINAR CUENTA
    // ===============================

    /**
     * Elimina permanentemente la cuenta del usuario autenticado.
     *
     * Esta acción es irreversible. El frontend debe mostrar un flujo
     * de confirmación antes de llamar a este endpoint.
     *
     * @param usuarioIdParam Identificador del usuario a eliminar.
     * @return Mensaje de confirmación de eliminación.
     */
    @Transactional
    public String eliminarCuenta(final Long usuarioIdParam) {

        final Usuario usuario = usuarioRepository.findById(usuarioIdParam)
                .orElseThrow(
                    () -> new RuntimeException("Usuario no encontrado"));

        usuarioRepository.delete(usuario);

        return "Cuenta eliminada permanentemente";
    }

    // ===============================
    // VER PERFIL PÚBLICO
    // ===============================

    /**
     * Obtiene el perfil público de un usuario por su identificador.
     *
     * @param usuarioIdParam Identificador del usuario cuyo perfil
     *                       se quiere ver.
     * @return Perfil público del usuario.
     */
    public UsuarioPerfilResponse verPerfil(final Long usuarioIdParam) {

        final Usuario usuario = usuarioRepository.findById(usuarioIdParam)
                .orElseThrow(
                    () -> new RuntimeException("Usuario no encontrado"));

        return mapToPerfilResponse(usuario);
    }

    /**
     * Devuelve la lista de perfiles públicos visibles en listados.
     *
     * Solo incluye usuarios que hayan activado la visibilidad.
     *
     * @return Lista de perfiles públicos visibles.
     */
    public List<UsuarioPerfilResponse> listarPerfilesPublicos() {

        return usuarioRepository.findByVisibleEnListadosTrue()
                .stream()
                .map(this::mapToPerfilResponse)
                .toList();
    }

    // ===============================
    // PRIVACIDAD
    // ===============================

    /**
     * Actualiza la configuración de privacidad del usuario.
     *
     * Permite al usuario decidir si su perfil aparece en listados
     * públicos y resultados de búsqueda dentro de la plataforma.
     *
     * @param usuarioIdParam Identificador del usuario.
     * @param requestParam   Configuración de privacidad deseada.
     * @return Mensaje de confirmación.
     */
    @Transactional
    public String actualizarPrivacidad(
            final Long usuarioIdParam,
            final PrivacidadRequest requestParam) {

        final Usuario usuario = usuarioRepository.findById(usuarioIdParam)
                .orElseThrow(
                    () -> new RuntimeException("Usuario no encontrado"));

        if (requestParam.getVisibleEnListados() != null) {
            usuario.setVisibleEnListados(
                requestParam.getVisibleEnListados());
        }

        usuarioRepository.save(usuario);

        return "Configuración de privacidad actualizada";
    }

    // ===============================
    // MÉTODOS AUXILIARES
    // ===============================

    /**
     * Mapea un objeto {@link Usuario} a {@link UsuarioPerfilResponse}.
     *
     * @param usuarioParam Usuario a mapear.
     * @return DTO con la información pública del usuario.
     */
    private UsuarioPerfilResponse mapToPerfilResponse(
            final Usuario usuarioParam) {
        return UsuarioPerfilResponse.builder()
                .id(usuarioParam.getId())
                .nombre(usuarioParam.getNombre())
                .foto(usuarioParam.getFoto())
                .bio(usuarioParam.getBio())
                .intereses(usuarioParam.getIntereses())
                .esTutor(usuarioParam.getEsTutor())
                .build();
    }
}
