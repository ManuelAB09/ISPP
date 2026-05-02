package es.us.meerkat.backend.service.communities;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import es.us.meerkat.backend.dto.communities.ApunteResponse;
import es.us.meerkat.backend.entity.communities.Apunte;
import es.us.meerkat.backend.entity.communities.Comunidad;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.communities.ApunteRepository;
import es.us.meerkat.backend.repository.communities.ComunidadRepository;
import es.us.meerkat.backend.repository.users.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Servicio para gestionar apuntes en comunidades. */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApunteService {

    private final ApunteRepository apunteRepository;
    private final ComunidadRepository comunidadRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuthorizationService authorizationService;

    /**
     * Sube un apunte a una comunidad.
     *
     * @param comunidadId ID de la comunidad
     * @param usuarioId ID del usuario que sube el apunte
     * @param titulo Título del apunte
     * @param descripcion Descripción del apunte (opcional)
     * @param file Archivo del apunte
     * @return DTO del apunte creado
     * @throws IllegalArgumentException si la comunidad o usuario no existen, o hay error en el
     *     archivo
     */
    @Transactional
    public ApunteResponse subirApunte(
            Long comunidadId,
            Long usuarioId,
            String titulo,
            String descripcion,
            MultipartFile file) {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("El archivo no puede estar vacío");
        }

        if (file.getSize() > 100 * 1024 * 1024) { // 100 MB límite
            throw new IllegalArgumentException("El archivo no puede superar 100 MB");
        }

        Comunidad comunidad =
                comunidadRepository
                        .findById(comunidadId)
                        .orElseThrow(() -> new IllegalArgumentException("Comunidad no encontrada"));

        Usuario usuario =
                usuarioRepository
                        .findById(usuarioId)
                        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (authorizationService != null
                && !authorizationService.canParticipate(usuarioId, comunidadId)) {
            throw new IllegalArgumentException(
                    "Debes ser miembro para publicar en una comunidad privada");
        }

        try {
            byte[] contenido = file.getBytes();

            Apunte apunte =
                    Apunte.builder()
                            .titulo(
                                    titulo != null && !titulo.isBlank()
                                            ? titulo
                                            : file.getOriginalFilename())
                            .descripcion(descripcion)
                            .contenido(contenido)
                            .nombreArchivo(file.getOriginalFilename())
                            .tipoMime(file.getContentType())
                            .tamanioArchivo(file.getSize())
                            .comunidad(comunidad)
                            .usuario(usuario)
                            .createdAt(LocalDateTime.now())
                            .descargas(0)
                            .valoracionMedia(0.0)
                            .build();

            Apunte apunteGuardado = apunteRepository.save(apunte);
            log.info("Apunte subido exitosamente: {}", apunteGuardado.getId());

            return mapToResponse(apunteGuardado);
        } catch (Exception e) {
            log.error("Error al subir apunte: {}", e.getMessage());
            throw new IllegalArgumentException("Error al procesar el archivo: " + e.getMessage());
        }
    }

    /**
     * Obtiene los apuntes de una comunidad con paginación.
     *
     * @param comunidadId ID de la comunidad
     * @param pageable Información de paginación
     * @return Página con los apuntes
     */
    @Transactional(readOnly = true)
    public Page<ApunteResponse> obtenerApuntesComunidad(Long comunidadId, Pageable pageable) {
        verificarComunidadExiste(comunidadId);
        return apunteRepository
                .findByComunidadIdOrderByCreatedAtDesc(comunidadId, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Obtiene todos los apuntes de una comunidad (sin paginación).
     *
     * @param comunidadId ID de la comunidad
     * @return Lista de apuntes
     */
    @Transactional(readOnly = true)
    public List<ApunteResponse> obtenerTodosApuntesComunidad(Long comunidadId) {
        verificarComunidadExiste(comunidadId);
        return apunteRepository.findByComunidadIdOrderByCreatedAtDesc(comunidadId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene un apunte por su ID.
     *
     * @param apunteId ID del apunte
     * @return DTO del apunte
     * @throws IllegalArgumentException si el apunte no existe
     */
    @Transactional(readOnly = true)
    public ApunteResponse obtenerApunte(Long apunteId) {
        Apunte apunte =
                apunteRepository
                        .findById(apunteId)
                        .orElseThrow(() -> new IllegalArgumentException("Apunte no encontrado"));
        return mapToResponse(apunte);
    }

    /**
     * Descarga un apunte (incrementa el contador de descargas).
     *
     * @param apunteId ID del apunte
     * @return Contenido del archivo
     * @throws IllegalArgumentException si el apunte no existe
     */
    @Transactional
    public byte[] descargarApunte(Long apunteId) {
        Apunte apunte =
                apunteRepository
                        .findById(apunteId)
                        .orElseThrow(() -> new IllegalArgumentException("Apunte no encontrado"));

        apunte.setDescargas(apunte.getDescargas() != null ? apunte.getDescargas() + 1 : 1);
        apunteRepository.save(apunte);

        log.info("Apunte descargado: {} (descargas totales: {})", apunteId, apunte.getDescargas());
        return apunte.getContenido();
    }

    /**
     * Elimina un apunte.
     *
     * @param apunteId ID del apunte
     * @param usuarioId ID del usuario que intenta eliminar
     * @throws IllegalArgumentException si el apunte no existe o no tiene permisos
     */
    @Transactional
    public void eliminarApunte(Long apunteId, Long usuarioId) {
        Apunte apunte =
                apunteRepository
                        .findById(apunteId)
                        .orElseThrow(() -> new IllegalArgumentException("Apunte no encontrado"));

        if (!apunte.getUsuario().getId().equals(usuarioId)) {
            throw new IllegalArgumentException("No tienes permisos para eliminar este apunte");
        }

        apunteRepository.delete(apunte);
        log.info("Apunte eliminado: {}", apunteId);
    }

    /**
     * Busca apuntes por título en una comunidad.
     *
     * @param comunidadId ID de la comunidad
     * @param titulo Título a buscar
     * @param pageable Información de paginación
     * @return Página con los apuntes que coinciden
     */
    @Transactional(readOnly = true)
    public Page<ApunteResponse> buscarApuntes(Long comunidadId, String titulo, Pageable pageable) {
        verificarComunidadExiste(comunidadId);
        return apunteRepository
                .searchByTituloInComunidad(comunidadId, titulo, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Obtiene los apuntes subidos por un usuario en una comunidad.
     *
     * @param comunidadId ID de la comunidad
     * @param usuarioId ID del usuario
     * @return Lista de apuntes del usuario
     */
    @Transactional(readOnly = true)
    public List<ApunteResponse> obtenerApuntesUsuario(Long comunidadId, Long usuarioId) {
        verificarComunidadExiste(comunidadId);
        return apunteRepository
                .findByComunidadIdAndUsuarioIdOrderByCreatedAtDesc(comunidadId, usuarioId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convierte un Apunte a ApunteResponse.
     *
     * @param apunte Apunte a convertir
     * @return DTO del apunte
     */
    private ApunteResponse mapToResponse(Apunte apunte) {
        return ApunteResponse.builder()
                .id(apunte.getId())
                .titulo(apunte.getTitulo())
                .descripcion(apunte.getDescripcion())
                .nombreArchivo(apunte.getNombreArchivo())
                .tipoMime(apunte.getTipoMime())
                .tamanioArchivo(apunte.getTamanioArchivo())
                .descargas(apunte.getDescargas())
                .valoracionMedia(apunte.getValoracionMedia())
                .createdAt(apunte.getCreatedAt())
                .updatedAt(apunte.getUpdatedAt())
                .usuarioId(apunte.getUsuario().getId())
                .usuarioNombre(apunte.getUsuario().getNombre())
                .usuarioFoto(apunte.getUsuario().getFoto())
                .comunidadId(apunte.getComunidad().getId())
                .build();
    }

    private void verificarComunidadExiste(Long comunidadId) {
        if (!comunidadRepository.existsById(comunidadId)) {
            throw new IllegalArgumentException("Comunidad no encontrada");
        }
    }
}
