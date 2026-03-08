package es.us.meerkat.backend.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.web.multipart.MultipartFile;

import es.us.meerkat.backend.dto.ChangePasswordRequest;
import es.us.meerkat.backend.dto.UpdateUserRequest;
import es.us.meerkat.backend.dto.UserDetailResponse;
import es.us.meerkat.backend.dto.UserPublicResponse;
import es.us.meerkat.backend.dto.VisibilityRequest;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

/**
 * Servicio para gestionar la lógica de negocio de usuarios.
 *
 * <p>
 * Cubre los endpoints de /api/v1/users del OpenAPI: obtener perfil propio,
 * actualizar, cambiar
 * contraseña, eliminar cuenta, visibilidad y ver perfiles públicos.
 */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    /** Prefijo público de los avatares predefinidos de Renata. */
    private static final String RENATA_AVATAR_PUBLIC_PREFIX = "/static/images/renata/";

    /** Patrón classpath para leer avatares predefinidos empaquetados en backend. */
    private static final String RENATA_AVATAR_CLASSPATH_PATTERN = "classpath:/static/static/images/renata/*.*";

    /** Longitud mínima requerida para las contraseñas. */
    private static final int MIN_PASSWORD_LENGTH = 8;

    /** Tamaño máximo de foto de perfil en bytes (5MB). */
    private static final long MAX_PROFILE_PHOTO_SIZE_BYTES = 5L * 1024L * 1024L;

    /** MIME types permitidos para foto de perfil. */
    private static final Set<String> ALLOWED_PROFILE_PHOTO_MIME_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    /** Repositorio para acceder a la información de usuarios. */
    private final UsuarioRepository usuarioRepository;

    /** Codificador de contraseñas BCrypt. */
    private final BCryptPasswordEncoder passwordEncoder;

    /** Resolver para localizar recursos estáticos en classpath. */
    private final ResourcePatternResolver resourcePatternResolver;

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
        Usuario usuarioActualizado = usuarioRepository
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
     * <p>
     * Solo modifica los campos que no sean nulos en el request.
     *
     * @param usuario      Usuario autenticado.
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
            usuario.setFoto(normalizarFotoPerfil(requestParam.getFoto()));
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
            usuario.setUbicacion(requestParam.getUbicacion());
        }

        usuarioRepository.save(usuario);
        return mapToDetailResponse(usuario);
    }

    /**
     * Actualiza la foto de perfil del usuario autenticado a partir de un archivo.
     *
     * @param usuario Usuario autenticado.
     * @param file    Archivo de imagen recibido en multipart.
     * @return Perfil actualizado.
     */
    @Transactional
    public UserDetailResponse actualizarFotoPerfil(final Usuario usuario, final MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Archivo de imagen requerido");
        }

        if (file.getSize() > MAX_PROFILE_PHOTO_SIZE_BYTES) {
            throw new IllegalArgumentException("La foto supera el límite de 5MB");
        }

        String mimeType = file.getContentType();
        if (mimeType == null || !ALLOWED_PROFILE_PHOTO_MIME_TYPES.contains(mimeType)) {
            throw new IllegalArgumentException("Formato no permitido. Solo JPG, PNG o WEBP");
        }

        try {
            String base64 = Base64.getEncoder().encodeToString(file.getBytes());
            String dataUri = "data:" + mimeType + ";base64," + base64;
            usuario.setFoto(dataUri);
            usuarioRepository.save(usuario);
            return mapToDetailResponse(usuario);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo procesar la imagen", e);
        }
    }

    // ===============================
    // DELETE /api/v1/users/me
    // ===============================

    /**
     * Elimina permanentemente la cuenta del usuario autenticado.
     *
     * <p>
     * Esta acción es irreversible. El frontend debe mostrar confirmación antes de
     * llamar a este
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
     * <p>
     * Verifica la contraseña actual antes de aplicar el cambio.
     *
     * @param usuario      Usuario autenticado.
     * @param requestParam Contraseña actual y nueva.
     * @throws RuntimeException si la contraseña actual es incorrecta o la nueva no
     *                          cumple los
     *                          requisitos.
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
     * @param usuario      Usuario autenticado.
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
     * <p>
     * Solo expone datos que el usuario ha hecho públicos.
     *
     * @param usuarioId Identificador del usuario.
     * @return Perfil público del usuario.
     * @throws RuntimeException si el usuario no existe.
     */
    public UserPublicResponse obtenerPerfilPublico(final Long usuarioId) {

        final Usuario usuario = usuarioRepository
                .findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return mapToPublicResponse(usuario);
    }

    /**
     * Devuelve la lista de avatares predefinidos disponibles para foto de perfil.
     *
     * @return Lista de rutas públicas de avatares de Renata.
     */
    public List<String> obtenerAvataresPerfilDisponibles() {
        Set<String> fileNames = obtenerNombresAvataresRenata();
        if (fileNames.isEmpty()) {
            return List.of();
        }

        return fileNames.stream()
                .sorted()
                .map(fileName -> RENATA_AVATAR_PUBLIC_PREFIX + fileName)
                .toList();
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

    /**
     * Normaliza la foto de perfil recibida desde API.
     *
     * <p>
     * Si llega un nombre de archivo (p.ej. Feliz.png) o una ruta de Renata, la
     * transforma a
     * ruta pública estable. Si llega vacío, deja la foto sin valor (null).
     * Cualquier otra
     * URL/ruta se
     * respeta para no romper compatibilidad con clientes existentes.
     *
     * @param fotoOriginal Valor recibido en UpdateUserRequest.foto.
     * @return Ruta/URL normalizada a persistir.
     */
    private String normalizarFotoPerfil(final String fotoOriginal) {
        if (!StringUtils.hasText(fotoOriginal)) {
            return null;
        }

        String fotoLimpia = fotoOriginal.trim();
        String marker = RENATA_AVATAR_PUBLIC_PREFIX;

        if (fotoLimpia.startsWith(marker)) {
            String fileName = fotoLimpia.substring(marker.length());
            return construirRutaRenataSiExiste(fileName, fotoLimpia);
        }

        int markerIndex = fotoLimpia.indexOf(marker);
        if (markerIndex >= 0) {
            String fileName = fotoLimpia.substring(markerIndex + marker.length());
            return construirRutaRenataSiExiste(fileName, fotoLimpia);
        }

        if (!fotoLimpia.contains("/")) {
            return construirRutaRenataSiExiste(fotoLimpia, fotoLimpia);
        }

        return fotoLimpia;
    }

    /**
     * Construye ruta pública de Renata si el archivo existe en recursos estáticos.
     */
    private String construirRutaRenataSiExiste(final String fileName, final String fallbackValue) {
        if (!StringUtils.hasText(fileName)) {
            return null;
        }

        Set<String> availableNames = obtenerNombresAvataresRenata();
        if (availableNames.contains(fileName)) {
            return RENATA_AVATAR_PUBLIC_PREFIX + fileName;
        }
        return fallbackValue;
    }

    /** Lee nombres de archivos de avatares Renata desde classpath. */
    private Set<String> obtenerNombresAvataresRenata() {
        try {
            Resource[] resources = resourcePatternResolver.getResources(RENATA_AVATAR_CLASSPATH_PATTERN);

            return Arrays.stream(resources)
                    .map(Resource::getFilename)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toSet());
        } catch (IOException ignored) {
            return Collections.emptySet();
        }
    }
}
