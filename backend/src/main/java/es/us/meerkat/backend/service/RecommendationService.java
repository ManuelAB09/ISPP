package es.us.meerkat.backend.service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.us.meerkat.backend.dto.FeedbackRecomendacionRequest;
import es.us.meerkat.backend.dto.RecomendacionResponse;
import es.us.meerkat.backend.entity.*;
import es.us.meerkat.backend.repository.*;

/**
 * Servicio para generar recomendaciones personalizadas basadas en inteligencia artificial.
 *
 * <p>Implementa un motor de recomendaciones que analiza: - Intereses del usuario - Actividades
 * recientes - Comunidades y tutores seguidos - Materias de estudio - Feedback anterior
 */
@Service
public class RecommendationService {

    @Autowired private RecomendacionRepository recomendacionRepository;
    @Autowired private FeedbackRecomendacionRepository feedbackRepository;
    @Autowired private ActividadUsuarioRepository actividadRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private TutorRepository tutorRepository;
    @Autowired private ComunidadRepository comunidadRepository;
    @Autowired private MiembroComunidadRepository miembroRepository;

    private static final int TOP_RECOMENDACIONES = 10;
    private static final int DIAS_LOOKBACK_ACTIVIDADES = 30;
    private static final long HORAS_EXPIRACION_REC = 7 * 24; // 7 días

    /** Obtiene las recomendaciones de profesores para un usuario. */
    @Transactional(readOnly = true)
    public Page<RecomendacionResponse> getRecomendacionesProfesores(
            Long usuarioId, Pageable pageable) {
        return recomendacionRepository
                .findPorTipo(usuarioId, TipoRecomendacion.PROFESOR, pageable)
                .map(this::mapToResponse);
    }

    /** Obtiene las recomendaciones de contenido para un usuario. */
    @Transactional(readOnly = true)
    public Page<RecomendacionResponse> getRecomendacionesContenido(
            Long usuarioId, Pageable pageable) {
        return recomendacionRepository
                .findPorTipo(usuarioId, TipoRecomendacion.CONTENIDO, pageable)
                .map(this::mapToResponse);
    }

    /** Obtiene las recomendaciones de cuestionarios para un usuario. */
    @Transactional(readOnly = true)
    public Page<RecomendacionResponse> getRecomendacionesCuestionarios(
            Long usuarioId, Pageable pageable) {
        return recomendacionRepository
                .findPorTipo(usuarioId, TipoRecomendacion.CUESTIONARIO, pageable)
                .map(this::mapToResponse);
    }

    /** Obtiene las recomendaciones de comunidades para un usuario. */
    @Transactional(readOnly = true)
    public Page<RecomendacionResponse> getRecomendacionesComunidades(
            Long usuarioId, Pageable pageable) {
        return recomendacionRepository
                .findPorTipo(usuarioId, TipoRecomendacion.COMUNIDAD, pageable)
                .map(this::mapToResponse);
    }

    /** Obtiene todas las recomendaciones activas de un usuario. */
    @Transactional(readOnly = true)
    public Page<RecomendacionResponse> getRecomendacionesActivas(
            Long usuarioId, Pageable pageable) {
        return recomendacionRepository
                .findRecomendacionesActivas(usuarioId, pageable)
                .map(this::mapToResponse);
    }

    /** Da feedback sobre una recomendación. */
    @Transactional
    public void darFeedbackRecomendacion(
            Long recomendacionId, FeedbackRecomendacionRequest request, Long usuarioId) {

        Recomendacion recomendacion =
                recomendacionRepository
                        .findById(recomendacionId)
                        .orElseThrow(
                                () -> new IllegalArgumentException("Recomendación no encontrada"));

        if (!recomendacion.getUsuario().getId().equals(usuarioId)) {
            throw new IllegalArgumentException(
                    "No tienes permisos para dar feedback en esta recomendación");
        }

        Usuario usuario =
                usuarioRepository
                        .findById(usuarioId)
                        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // Crear o actualizar feedback
        FeedbackRecomendacion feedback = new FeedbackRecomendacion();
        feedback.setRecomendacion(recomendacion);
        feedback.setUsuario(usuario);
        feedback.setEsUtil(request.getEsUtil());
        feedback.setComentario(request.getComentario());
        feedback.setSatisfaccion(request.getSatisfaccion());

        feedbackRepository.save(feedback);

        // Actualizar la recomendación con el feedback
        recomendacion.setEsFavorable(request.getEsUtil());
        recomendacionRepository.save(recomendacion);
    }

    /** Registra una actividad del usuario para análisis de recomendaciones. */
    @Transactional
    public void registrarActividad(
            Long usuarioId, String tipo, String categoria, Long idObjeto, String terminos) {
        Usuario usuario =
                usuarioRepository
                        .findById(usuarioId)
                        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        ActividadUsuario actividad =
                ActividadUsuario.builder()
                        .usuario(usuario)
                        .tipoActividad(tipo)
                        .categoriaObjeto(categoria)
                        .idObjeto(idObjeto)
                        .terminosBusqueda(terminos)
                        .dispositivo("web")
                        .build();

        actividadRepository.save(actividad);
    }

    /** Genera recomendaciones para un usuario (ejecutado de forma asíncrona). */
    @Transactional
    @Async
    public void generarRecomendacionesUsuario(Long usuarioId) {
        try {
            Usuario usuario =
                    usuarioRepository
                            .findById(usuarioId)
                            .orElseThrow(
                                    () -> new IllegalArgumentException("Usuario no encontrado"));

            // Limpiar recomendaciones expiradas
            limpiarRecomendacionesExpiradas(usuarioId);

            // Generar cada tipo de recomendación
            generarRecomendacionesProfesores(usuario);
            generarRecomendacionesContenido(usuario);
            generarRecomendacionesCuestionarios(usuario);
            generarRecomendacionesComunidades(usuario);

        } catch (Exception e) {
            System.err.println(
                    "Error generando recomendaciones para usuario "
                            + usuarioId
                            + ": "
                            + e.getMessage());
        }
    }

    /** Ejecuta la generación de recomendaciones de forma periódica (cada día). */
    @Scheduled(cron = "0 0 2 * * *") // 2 AM cada día
    @Transactional
    public void generarRecomendacionesTodosUsuarios() {
        List<Usuario> usuarios = usuarioRepository.findAll();
        usuarios.forEach(u -> generarRecomendacionesUsuario(u.getId()));
    }

    // ==================== Métodos Privados ====================

    /** Genera recomendaciones de profesores basadas en intereses. */
    private void generarRecomendacionesProfesores(Usuario usuario) {
        // Obtener intereses del usuario
        List<String> intereses = usuario.getIntereses();

        // Buscar tutores con especialidades coincidentes
        List<Tutor> tutoresRecomendados = new ArrayList<>();
        List<Tutor> todosTutores = tutorRepository.findByVerificadoTrue();

        for (Tutor tutor : todosTutores) {
            // Evitar recomendar tutores ya seguidos
            Double similaridad = calcularSimilaridad(intereses, tutor.getEspecialidades());
            if (similaridad > 0.3) {
                tutoresRecomendados.add(tutor);
            }
        }

        // Ordenar por similitud y crear recomendaciones
        tutoresRecomendados.stream()
                .limit(TOP_RECOMENDACIONES)
                .forEach(
                        tutor -> {
                            Double significancia =
                                    calcularSimilaridad(intereses, tutor.getEspecialidades());
                            crearRecomendacion(
                                    usuario,
                                    TipoRecomendacion.PROFESOR,
                                    tutor.getId(),
                                    tutor.getUsuario().getNombre(),
                                    tutor.getUsuario().getBio(),
                                    null,
                                    significancia * 100,
                                    "Tutor en " + String.join(", ", tutor.getEspecialidades()));
                        });
    }

    /** Genera recomendaciones de comunidades. */
    private void generarRecomendacionesComunidades(Usuario usuario) {
        // Obtener comunidades en las que YA está
        Page<MiembroComunidad> paginaMiembros =
                miembroRepository.findByUsuarioId(usuario.getId(), PageRequest.of(0, 1000));
        Set<Long> comunidadesYaEnListado =
                paginaMiembros.getContent().stream()
                        .map(m -> m.getComunidad().getId())
                        .collect(Collectors.toSet());

        // Obtener todas las comunidades públicas
        List<Comunidad> todasComunidades = comunidadRepository.findAll();

        // Recomendar comunidades similares a las que ya participa
        todasComunidades.stream()
                .filter(c -> !comunidadesYaEnListado.contains(c.getId()))
                .filter(c -> TipoGrupo.COMUNIDAD_PUBLICA.equals(c.getTipoGrupo()))
                .limit(TOP_RECOMENDACIONES)
                .forEach(
                        comunidad -> {
                            crearRecomendacion(
                                    usuario,
                                    TipoRecomendacion.COMUNIDAD,
                                    comunidad.getId(),
                                    comunidad.getNombre(),
                                    comunidad.getDescripcion(),
                                    null,
                                    75.0,
                                    "Comunidad: " + comunidad.getNombre());
                        });
    }

    /** Genera recomendaciones de contenido basadas en actividades. */
    private void generarRecomendacionesContenido(Usuario usuario) {
        // Obtener términos de búsqueda recientes
        List<String> temasInteres = actividadRepository.findTemasInteres(usuario.getId());

        temasInteres.stream()
                .limit(5)
                .forEach(
                        tema -> {
                            // Aquí se integraría con un servicio de contenido externo
                            // o base de datos de recursos educativos
                            crearRecomendacion(
                                    usuario,
                                    TipoRecomendacion.CONTENIDO,
                                    (long) Math.abs(tema.hashCode()),
                                    "Material: " + tema,
                                    "Recursos educativos sobre " + tema,
                                    null,
                                    70.0,
                                    "Basado en tu búsqueda: " + tema);
                        });
    }

    /** Genera recomendaciones de cuestionarios. */
    private void generarRecomendacionesCuestionarios(Usuario usuario) {
        // En una implementación real, aquí se buscarían cuestionarios
        // relacionados con las materias del usuario
        List<String> temasInteres = actividadRepository.findTemasInteres(usuario.getId());

        temasInteres.stream()
                .limit(3)
                .forEach(
                        tema -> {
                            crearRecomendacion(
                                    usuario,
                                    TipoRecomendacion.CUESTIONARIO,
                                    (long) Math.abs(tema.hashCode()),
                                    "Quiz: " + tema,
                                    "Quiz de práctica sobre " + tema,
                                    null,
                                    65.0,
                                    "Para practicar lo aprendido en " + tema);
                        });
    }

    /** Calcula la similitud entre dos listas de palabras clave (Jaccard similarity). */
    private Double calcularSimilaridad(List<String> lista1, List<String> lista2) {
        if (lista1 == null || lista2 == null || lista1.isEmpty() || lista2.isEmpty()) {
            return 0.0;
        }

        Set<String> set1 = new HashSet<>(lista1);
        Set<String> set2 = new HashSet<>(lista2);

        Set<String> interseccion = new HashSet<>(set1);
        interseccion.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        if (union.isEmpty()) {
            return 0.0;
        }

        return (double) interseccion.size() / union.size();
    }

    /** Crea una recomendación si no existe ya. */
    private void crearRecomendacion(
            Usuario usuario,
            TipoRecomendacion tipo,
            Long idObjeto,
            String titulo,
            String descripcion,
            String imagenUrl,
            Double puntuacionRelevancia,
            String razonRecomendacion) {

        // Verificar que no exista ya
        List<Recomendacion> existentes =
                recomendacionRepository.findByUsuarioTipoObjeto(usuario.getId(), tipo, idObjeto);

        if (!existentes.isEmpty()) {
            return; // Ya existe
        }

        Recomendacion recomendacion =
                Recomendacion.builder()
                        .usuario(usuario)
                        .tipo(tipo)
                        .idObjetoRecomendado(idObjeto)
                        .titulo(titulo)
                        .descripcion(descripcion)
                        .imagenUrl(imagenUrl)
                        .puntuacionRelevancia(Math.min(puntuacionRelevancia, 100.0))
                        .razonRecomendacion(razonRecomendacion)
                        .vista(false)
                        .fechaExpiracion(LocalDateTime.now().plusHours(HORAS_EXPIRACION_REC))
                        .build();

        recomendacionRepository.save(recomendacion);
    }

    /** Limpia recomendaciones expiradas. */
    private void limpiarRecomendacionesExpiradas(Long usuarioId) {
        List<Recomendacion> recomendaciones =
                recomendacionRepository
                        .findRecomendacionesActivas(usuarioId, PageRequest.of(0, 1000))
                        .getContent();

        recomendaciones.stream()
                .filter(Recomendacion::estaExpirada)
                .forEach(recomendacionRepository::delete);
    }

    // ==================== Métodos de Mapeo ====================

    private RecomendacionResponse mapToResponse(Recomendacion recomendacion) {
        return RecomendacionResponse.builder()
                .id(recomendacion.getId())
                .tipo(recomendacion.getTipo().toString())
                .communityId(recomendacion.getIdObjetoRecomendado())
                .nombre(recomendacion.getTitulo())
                .descripcion(recomendacion.getDescripcion())
                .imagenUrl(recomendacion.getImagenUrl())
                .relevancia(recomendacion.getPuntuacionRelevancia())
                .motivo(recomendacion.getRazonRecomendacion())
                .vista(recomendacion.getVista())
                .esFavorable(recomendacion.getEsFavorable())
                .createdAt(recomendacion.getCreatedAt())
                .build();
    }
}
