package es.us.meerkat.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.us.meerkat.backend.dto.RecomendacionResponse;
import es.us.meerkat.backend.entity.Comunidad;
import es.us.meerkat.backend.entity.FactorRecomendacion;
import es.us.meerkat.backend.entity.RecomendacionComunidad;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.ComunidadRepository;
import es.us.meerkat.backend.repository.RecomendacionComunidadRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

/** Servicio para la gestión y generación de recomendaciones de comunidades. */
@Service
@RequiredArgsConstructor
@Transactional
public class RecomendacionComunidadService {

    private final RecomendacionComunidadRepository recomendacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final ComunidadRepository comunidadRepository;

    /**
     * Crea una nueva recomendación de comunidad para un usuario.
     *
     * @param usuarioId ID del usuario
     * @param comunidadId ID de la comunidad recomendada
     * @param factor factor por el cual se recomienda
     * @param relevancia puntuación de relevancia (0-100)
     * @return la recomendación creada
     */
    public RecomendacionComunidad crearRecomendacion(
            Long usuarioId, Long comunidadId, FactorRecomendacion factor, Double relevancia) {
        Usuario usuario =
                usuarioRepository
                        .findById(usuarioId)
                        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Comunidad comunidad =
                comunidadRepository
                        .findById(comunidadId)
                        .orElseThrow(() -> new IllegalArgumentException("Comunidad no encontrada"));

        // Verificar que no exista ya una recomendación
        var existente = recomendacionRepository.findByUsuarioAndComunidad(usuario, comunidad);
        if (existente.isPresent()) {
            // Actualizar la existente con mayor relevancia
            RecomendacionComunidad rec = existente.get();
            rec.setRelevancia(Math.max(rec.getRelevancia(), relevancia));
            rec.setFactor(factor);
            return recomendacionRepository.save(rec);
        }

        RecomendacionComunidad recomendacion =
                RecomendacionComunidad.builder()
                        .usuario(usuario)
                        .comunidad(comunidad)
                        .factor(factor)
                        .relevancia(relevancia)
                        .vista(false)
                        .build();

        return recomendacionRepository.save(recomendacion);
    }

    /**
     * Obtiene las recomendaciones de un usuario de forma paginada.
     *
     * @param usuarioId ID del usuario
     * @param pageable paginación
     * @return página de recomendaciones
     */
    @Transactional(readOnly = true)
    public Page<RecomendacionComunidad> getRecomendacionesUsuario(
            Long usuarioId, Pageable pageable) {
        Usuario usuario =
                usuarioRepository
                        .findById(usuarioId)
                        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        return recomendacionRepository.findByUsuarioOrderByRelevanciaDesc(usuario, pageable);
    }

    /**
     * Obtiene las recomendaciones no vistas de un usuario.
     *
     * @param usuarioId ID del usuario
     * @param pageable paginación
     * @return página de recomendaciones no vistas
     */
    @Transactional(readOnly = true)
    public Page<RecomendacionComunidad> getRecomendacionesNoVistas(
            Long usuarioId, Pageable pageable) {
        Usuario usuario =
                usuarioRepository
                        .findById(usuarioId)
                        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        return recomendacionRepository.findByUsuarioAndVistaFalseOrderByRelevanciaDesc(
                usuario, pageable);
    }

    /**
     * Marca una recomendación como vista.
     *
     * @param recomendacionId ID de la recomendación
     * @throws IllegalArgumentException si no existe
     */
    public void marcarComoVista(Long recomendacionId) {
        RecomendacionComunidad recomendacion =
                recomendacionRepository
                        .findById(recomendacionId)
                        .orElseThrow(
                                () -> new IllegalArgumentException("Recomendación no encontrada"));

        recomendacion.marcarComoVista();
        recomendacionRepository.save(recomendacion);
    }

    /**
     * Elimina una recomendación.
     *
     * @param recomendacionId ID de la recomendación
     */
    public void eliminarRecomendacion(Long recomendacionId) {
        recomendacionRepository.deleteById(recomendacionId);
    }

    /**
     * Convierte una recomendación a su DTO de respuesta.
     *
     * @param recomendacion la recomendación
     * @return el DTO
     */
    public RecomendacionResponse toResponse(RecomendacionComunidad recomendacion) {
        String motivo = generarMotivoRecomendacion(recomendacion.getFactor());
        return RecomendacionResponse.builder()
                .id(recomendacion.getId())
                .communityId(recomendacion.getComunidad().getId())
                .nombre(recomendacion.getComunidad().getNombre())
                .descripcion(recomendacion.getComunidad().getDescripcion())
                .imagenUrl(recomendacion.getComunidad().getImagenUrl())
                .factor(recomendacion.getFactor())
                .relevancia(Math.round(recomendacion.getRelevancia() * 100.0) / 100.0)
                .motivo(motivo)
                .build();
    }

    /**
     * Genera un mensaje descriptivo del motivo de la recomendación.
     *
     * @param factor el factor de recomendación
     * @return el motivo
     */
    private String generarMotivoRecomendacion(FactorRecomendacion factor) {
        return switch (factor) {
            case INTERES_SIMILAR -> "Coincide con tus intereses";
            case UBICACION -> "Popular en tu zona";
            case NIVEL_EDUCATIVO -> "Acorde a tu nivel de estudios";
            case COMUNIDAD_SIMILAR -> "Similar a comunidades donde participas";
            case POPULARIDAD -> "Tendencia actual";
            case ACTIVIDAD_SIMILAR -> "Basado en tu actividad anterior";
        };
    }

    /**
     * Obtiene una recomendación por ID (lectura).
     *
     * @param recomendacionId ID de la recomendación
     * @return la recomendación
     */
    @Transactional(readOnly = true)
    public RecomendacionComunidad getRecomendacionById(Long recomendacionId) {
        return recomendacionRepository
                .findById(recomendacionId)
                .orElseThrow(() -> new IllegalArgumentException("Recomendación no encontrada"));
    }
}
