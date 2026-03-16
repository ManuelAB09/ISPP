package es.us.meerkat.backend.service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import es.us.meerkat.backend.dto.RecomendacionesPageResponse;
import es.us.meerkat.backend.dto.RegistrarActividadRequest;
import es.us.meerkat.backend.dto.ValoracionTutorRequest;
import es.us.meerkat.backend.entity.*;
import es.us.meerkat.backend.repository.*;

@Service
public class RecommendationService {

    @Autowired private RecomendacionRepository recomendacionRepository;
    @Autowired private FeedbackRecomendacionRepository feedbackRepository;
    @Autowired private ActividadUsuarioRepository actividadRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private TutorRepository tutorRepository;
    @Autowired private ComunidadRepository comunidadRepository;
    @Autowired private MiembroComunidadRepository miembroRepository;
    @Autowired private ValoracionTutorRepository valoracionRepository; // NUEVO
    @Autowired private ContenidoRepository contenidoRepository; // NUEVO
    @Autowired private CuestionarioRepository cuestionarioRepository; // NUEVO

    // -----------------------------------------------------------------------
    // Constantes
    // -----------------------------------------------------------------------
    private static final int TOP_RECOMENDACIONES = 10;
    private static final int TOP_PARA_TI = 6;
    private static final int DIAS_LOOKBACK = 30;
    private static final long EXP_PROFESORES = 24 * 7;
    private static final long EXP_CONTENIDO = 24 * 3;
    private static final long EXP_QUIZ = 24 * 5;
    private static final long EXP_COMUNIDAD = 24 * 7;
    private static final double UMBRAL_TUTOR = 0.10;

    // Pesos score profesores — ahora suman 1.0 con valoración y nivel
    private static final double W_SIMILITUD = 0.35; // similitud intereses/especialidades
    private static final double W_ACTIVIDAD = 0.20; // boost búsquedas recientes
    private static final double W_UBICACION = 0.10; // proximidad geográfica
    private static final double W_FEEDBACK = 0.15; // feedback propio del usuario
    private static final double W_VALORACION = 0.15; // valoraciones de OTROS usuarios ← NUEVO
    private static final double W_NIVEL = 0.05; // coincidencia nivel educativo ← NUEVO

    // Palabras clave de nivel educativo para matching con bio del tutor
    private static final Map<String, List<String>> KEYWORDS_NIVEL =
            Map.of(
                    "primaria",
                    List.of("primaria", "infantil", "niños", "6", "7", "8", "9", "10", "11", "12"),
                    "secundaria",
                    List.of(
                            "secundaria",
                            "eso",
                            "bachillerato",
                            "instituto",
                            "12",
                            "13",
                            "14",
                            "15",
                            "16",
                            "17",
                            "18"),
                    "universidad",
                    List.of(
                            "universidad",
                            "universitario",
                            "grado",
                            "máster",
                            "master",
                            "superior",
                            "college"),
                    "profesional",
                    List.of(
                            "profesional",
                            "laboral",
                            "empresa",
                            "corporativo",
                            "adultos",
                            "formación"));

    // -----------------------------------------------------------------------
    // CONSULTA
    // -----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public RecomendacionesPageResponse getRecomendacionesPage(Long usuarioId) {
        return RecomendacionesPageResponse.builder()
                .paraTi(topParaTi(usuarioId, TOP_PARA_TI))
                .profesores(porTipo(usuarioId, TipoRecomendacion.PROFESOR, 6))
                .contenidos(porTipo(usuarioId, TipoRecomendacion.CONTENIDO, 8))
                .cuestionarios(porTipo(usuarioId, TipoRecomendacion.CUESTIONARIO, 6))
                .comunidades(porTipo(usuarioId, TipoRecomendacion.COMUNIDAD, 4))
                .generadoEn(LocalDateTime.now())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<RecomendacionResponse> getRecomendacionesProfesores(Long uid, Pageable p) {
        return recomendacionRepository
                .findPorTipo(uid, TipoRecomendacion.PROFESOR, p)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<RecomendacionResponse> getRecomendacionesContenido(Long uid, Pageable p) {
        return recomendacionRepository
                .findPorTipo(uid, TipoRecomendacion.CONTENIDO, p)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<RecomendacionResponse> getRecomendacionesCuestionarios(Long uid, Pageable p) {
        return recomendacionRepository
                .findPorTipo(uid, TipoRecomendacion.CUESTIONARIO, p)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<RecomendacionResponse> getRecomendacionesComunidades(Long uid, Pageable p) {
        return recomendacionRepository
                .findPorTipo(uid, TipoRecomendacion.COMUNIDAD, p)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<RecomendacionResponse> getRecomendacionesActivas(Long uid, Pageable p) {
        return recomendacionRepository.findRecomendacionesActivas(uid, p).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<RecomendacionResponse> getRecomendacionesNoVistas(Long uid, Pageable p) {
        return recomendacionRepository.findNoVistas(uid, p).map(this::toResponse);
    }

    // -----------------------------------------------------------------------
    // VALORACIONES DE TUTORES ← NUEVO
    // -----------------------------------------------------------------------

    /**
     * Guarda o actualiza la valoración (1-5) de un usuario sobre un tutor. Tras guardar, regenera
     * las recomendaciones de profesores de forma asíncrona para que la nueva valoración afecte al
     * algoritmo inmediatamente.
     */
    @Transactional
    public void valorarTutor(Long tutorId, Long usuarioId, ValoracionTutorRequest request) {
        Tutor tutor =
                tutorRepository
                        .findById(tutorId)
                        .orElseThrow(() -> new IllegalArgumentException("Tutor no encontrado"));
        if (tutor.getUsuario().getId().equals(usuarioId)) {
            throw new IllegalArgumentException("No puedes valorarte a ti mismo");
        }
        Usuario usuario =
                usuarioRepository
                        .findById(usuarioId)
                        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // Upsert: si ya valoró este tutor, actualiza
        ValoracionTutor valoracion =
                valoracionRepository
                        .findByTutorIdAndUsuarioId(tutorId, usuarioId)
                        .orElse(ValoracionTutor.builder().tutor(tutor).usuario(usuario).build());

        valoracion.setPuntuacion(request.getPuntuacion());
        valoracion.setComentario(request.getComentario());
        valoracionRepository.save(valoracion);

        // Regenerar recomendaciones de PROFESORES para todos los usuarios afectados
        // (asíncrono, no bloquea la respuesta)
        regenerarTipo(usuarioId, TipoRecomendacion.PROFESOR);
    }

    /** Devuelve la valoración media de un tutor (0.0 si no tiene ninguna). */
    @Transactional(readOnly = true)
    public Double getValoracionMedia(Long tutorId) {
        return valoracionRepository.findMediaByTutorId(tutorId);
    }

    // -----------------------------------------------------------------------
    // VISTA / ELIMINAR
    // -----------------------------------------------------------------------

    @Transactional
    public void marcarComoVista(Long recomendacionId, Long usuarioId) {
        Recomendacion rec =
                recomendacionRepository
                        .findById(recomendacionId)
                        .orElseThrow(
                                () -> new IllegalArgumentException("Recomendación no encontrada"));
        if (!rec.getUsuario().getId().equals(usuarioId)) {
            throw new IllegalArgumentException("No tienes permisos sobre esta recomendación");
        }
        rec.setVista(true);
        rec.setFechaVista(LocalDateTime.now());
        recomendacionRepository.save(rec);
    }

    @Transactional
    public void eliminarRecomendacion(Long recomendacionId, Long usuarioId) {
        Recomendacion rec =
                recomendacionRepository
                        .findById(recomendacionId)
                        .orElseThrow(
                                () -> new IllegalArgumentException("Recomendación no encontrada"));
        if (!rec.getUsuario().getId().equals(usuarioId)) {
            throw new IllegalArgumentException("No tienes permisos sobre esta recomendación");
        }
        recomendacionRepository.delete(rec);
    }

    // -----------------------------------------------------------------------
    // FEEDBACK
    // -----------------------------------------------------------------------

    @Transactional
    public void darFeedbackRecomendacion(
            Long recomendacionId, FeedbackRecomendacionRequest request, Long usuarioId) {
        Recomendacion rec =
                recomendacionRepository
                        .findById(recomendacionId)
                        .orElseThrow(
                                () -> new IllegalArgumentException("Recomendación no encontrada"));
        if (!rec.getUsuario().getId().equals(usuarioId)) {
            throw new IllegalArgumentException("No tienes permisos para dar feedback");
        }
        Usuario usuario =
                usuarioRepository
                        .findById(usuarioId)
                        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        FeedbackRecomendacion feedback =
                feedbackRepository
                        .findByRecomendacionIdAndUsuarioId(recomendacionId, usuarioId)
                        .orElse(new FeedbackRecomendacion());
        feedback.setRecomendacion(rec);
        feedback.setUsuario(usuario);
        feedback.setEsUtil(request.getEsUtil());
        feedback.setComentario(request.getComentario());
        feedback.setSatisfaccion(request.getSatisfaccion());
        feedbackRepository.save(feedback);

        rec.setEsFavorable(request.getEsUtil());
        recomendacionRepository.save(rec);

        if (Boolean.FALSE.equals(request.getEsUtil())) {
            regenerarTipo(usuarioId, rec.getTipo());
        }
    }

    // -----------------------------------------------------------------------
    // ACTIVIDAD
    // -----------------------------------------------------------------------

    @Transactional
    public void registrarActividad(Long usuarioId, RegistrarActividadRequest request) {
        registrarActividad(
                usuarioId,
                request.getTipoActividad(),
                request.getCategoriaObjeto(),
                request.getIdObjeto(),
                request.getTerminosBusqueda(),
                request.getDuracionSegundos(),
                request.getDatosAdicionales());
    }

    @Transactional
    public void registrarActividad(
            Long usuarioId, String tipo, String categoria, Long idObjeto, String terminos) {
        registrarActividad(usuarioId, tipo, categoria, idObjeto, terminos, null, null);
    }

    @Transactional
    public void registrarActividad(
            Long usuarioId,
            String tipo,
            String categoria,
            Long idObjeto,
            String terminos,
            Long duracionSegundos,
            String datosAdicionales) {
        Usuario usuario =
                usuarioRepository
                        .findById(usuarioId)
                        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        actividadRepository.save(
                ActividadUsuario.builder()
                        .usuario(usuario)
                        .tipoActividad(tipo)
                        .categoriaObjeto(categoria)
                        .idObjeto(idObjeto)
                        .terminosBusqueda(terminos)
                        .duracionSegundos(duracionSegundos)
                        .datosAdicionales(datosAdicionales)
                        .dispositivo("web")
                        .build());

        TipoRecomendacion tipoRec = mapCategoria(categoria);
        if (tipoRec != null) {
            regenerarTipo(usuarioId, tipoRec);
        } else {

            generarRecomendacionesUsuario(usuarioId);
        }
    }

    // -----------------------------------------------------------------------
    // GENERACIÓN ASÍNCRONA
    // -----------------------------------------------------------------------

    @Transactional
    @Async
    public void generarRecomendacionesUsuario(Long usuarioId) {
        try {
            Usuario u =
                    usuarioRepository
                            .findById(usuarioId)
                            .orElseThrow(
                                    () -> new IllegalArgumentException("Usuario no encontrado"));
            limpiarExpiradas(usuarioId);
            generarProfesores(u);
            generarContenido(u);
            generarQuizzes(u);
            generarComunidades(u);
        } catch (Exception e) {
            System.err.println(
                    "Error recomendaciones usuario " + usuarioId + ": " + e.getMessage());
        }
    }

    @Async
    @Transactional
    public void regenerarTipo(Long usuarioId, TipoRecomendacion tipo) {
        try {
            Usuario u =
                    usuarioRepository
                            .findById(usuarioId)
                            .orElseThrow(
                                    () -> new IllegalArgumentException("Usuario no encontrado"));
            recomendacionRepository.deleteByUsuarioIdAndTipo(usuarioId, tipo);
            switch (tipo) {
                case PROFESOR -> generarProfesores(u);
                case CONTENIDO -> generarContenido(u);
                case CUESTIONARIO -> generarQuizzes(u);
                case COMUNIDAD -> generarComunidades(u);
                default -> throw new IllegalArgumentException("Tipo no válido: " + tipo);
            }
        } catch (Exception e) {
            System.err.println(
                    "Error regenerando " + tipo + " usuario " + usuarioId + ": " + e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void generarRecomendacionesTodosUsuarios() {
        usuarioRepository.findAll().forEach(u -> generarRecomendacionesUsuario(u.getId()));
    }

    // -----------------------------------------------------------------------
    // GENERACIÓN POR TIPO
    // -----------------------------------------------------------------------

    private void generarProfesores(Usuario usuario) {
        List<String> intereses = interesesEfectivos(usuario);
        double tasaFeedback = tasaFeedbackUtil(usuario.getId());
        String ubicUsuario = nombreUbicacion(usuario.getUbicacion());
        String nivelUsuario = nvl(usuario.getNivelEstudios()).toLowerCase(); // ← NUEVO

        tutorRepository.findByVerificadoTrue().stream()
                .filter(t -> !t.getUsuario().getId().equals(usuario.getId()))
                .map(
                        t -> {
                            double sim = jaccard(intereses, t.getEspecialidades());
                            double act = boostActividad(usuario.getId(), t.getEspecialidades());
                            double ubic = boostUbicacionTutor(t, ubicUsuario);
                            double valoracion = boostValoracion(t.getId()); // ← NUEVO
                            double nivel = boostNivel(t, nivelUsuario); // ← NUEVO
                            double score =
                                    scoreProfesor(sim, act, ubic, tasaFeedback, valoracion, nivel);
                            return Map.entry(t, score);
                        })
                .filter(e -> e.getValue() > UMBRAL_TUTOR)
                .sorted(Map.Entry.<Tutor, Double>comparingByValue().reversed())
                .limit(TOP_RECOMENDACIONES)
                .forEach(
                        e -> {
                            Tutor t = e.getKey();
                            guardar(
                                    usuario,
                                    TipoRecomendacion.PROFESOR,
                                    t.getId(),
                                    t.getUsuario().getNombre(),
                                    descripcionTutor(
                                            t, valoracionRepository.findMediaByTutorId(t.getId())),
                                    t.getUsuario().getFoto(),
                                    e.getValue() * 100,
                                    motivoProfesor(
                                            intereses,
                                            t.getEspecialidades(),
                                            valoracionRepository.findMediaByTutorId(t.getId()),
                                            nivelUsuario,
                                            t),
                                    EXP_PROFESORES);
                        });
    }

    private void generarComunidades(Usuario usuario) {
        Set<Long> yaUnidas =
                miembroRepository
                        .findByUsuarioId(usuario.getId(), PageRequest.of(0, 1000))
                        .getContent()
                        .stream()
                        .map(m -> m.getComunidad().getId())
                        .collect(Collectors.toSet());

        List<String> intereses = interesesEfectivos(usuario);
        String ubicUsuario = nombreUbicacion(usuario.getUbicacion());

        comunidadRepository.findAll().stream()
                .filter(c -> !yaUnidas.contains(c.getId()))
                .filter(c -> TipoGrupo.COMUNIDAD_PUBLICA.equals(c.getTipoGrupo()))
                .map(c -> Map.entry(c, scoreComunidad(c, intereses, ubicUsuario)))
                .sorted(Map.Entry.<Comunidad, Double>comparingByValue().reversed())
                .limit(TOP_RECOMENDACIONES)
                .forEach(
                        e -> {
                            Comunidad c = e.getKey();
                            guardar(
                                    usuario,
                                    TipoRecomendacion.COMUNIDAD,
                                    c.getId(),
                                    c.getNombre(),
                                    c.getDescripcion(),
                                    null,
                                    e.getValue() * 100,
                                    motivoComunidad(c, intereses),
                                    EXP_COMUNIDAD);
                        });
    }

    private void generarContenido(Usuario usuario) {
        LocalDateTime desde = LocalDateTime.now().minusDays(DIAS_LOOKBACK);
        List<String> temas = actividadRepository.findTemasInteres(usuario.getId(), desde);
        double tasaFeedback = tasaFeedbackUtil(usuario.getId());
        String nivelUsuario = nvl(usuario.getNivelEstudios());

        if (temas.isEmpty()) {

            return;
        }
        // Normalizar temas a minúsculas para el matching en BD
        List<String> temasLower =
                temas.stream().map(String::toLowerCase).collect(Collectors.toList());

        // Buscar contenidos reales en BD filtrando por nivel educativo si está
        // disponible
        List<Contenido> candidatos =
                nivelUsuario.isBlank()
                        ? contenidoRepository.findActivosByTemasInteres(temasLower)
                        : contenidoRepository.findActivosByTemasYNivel(temasLower, nivelUsuario);

        // IDs de contenidos ya vistos por el usuario (para no repetir)
        List<Long> yaVistos =
                actividadRepository.findObjetosVisitados(usuario.getId(), "Contenido");

        double score = 0.70 + (tasaFeedback * 0.20);

        candidatos.stream()
                .filter(c -> !yaVistos.contains(c.getId()))
                .limit(8)
                .forEach(
                        c ->
                                guardar(
                                        usuario,
                                        TipoRecomendacion.CONTENIDO,
                                        c.getId(), // ID REAL de la BD
                                        c.getTitulo(),
                                        c.getDescripcion(),
                                        c.getImagenUrl(),
                                        score * 100,
                                        motivoContenido(c, temas),
                                        EXP_CONTENIDO));
    }

    private void generarQuizzes(Usuario usuario) {
        LocalDateTime desde = LocalDateTime.now().minusDays(DIAS_LOOKBACK);
        List<String> temas = actividadRepository.findTemasInteres(usuario.getId(), desde);
        Map<String, Double> rend = rendimientoPorTema(usuario.getId());

        if (temas.isEmpty()) {
            return;
        }

        List<String> temasLower =
                temas.stream().map(String::toLowerCase).collect(Collectors.toList());

        // IDs de quizzes ya completados recientemente (para no repetir)
        List<Long> yaCompletados =
                actividadRepository.findObjetosVisitados(usuario.getId(), "Cuestionario");

        // Separar temas débiles (rendimiento < 0.5) de temas normales
        List<String> temasDebiles =
                temas.stream()
                        .filter(t -> rend.getOrDefault(t, 0.6) < 0.5)
                        .map(String::toLowerCase)
                        .collect(Collectors.toList());

        List<String> temasNormales =
                temas.stream()
                        .filter(t -> rend.getOrDefault(t, 0.6) >= 0.5)
                        .map(String::toLowerCase)
                        .collect(Collectors.toList());

        List<Cuestionario> candidatos = new ArrayList<>();

        // Temas débiles → buscar cuestionarios BASICO e INTERMEDIO (para practicar)
        if (!temasDebiles.isEmpty()) {
            candidatos.addAll(
                    cuestionarioRepository.findActivosByTemasYDificultad(
                            temasDebiles, NivelDificultad.BASICO));
            candidatos.addAll(
                    cuestionarioRepository.findActivosByTemasYDificultad(
                            temasDebiles, NivelDificultad.INTERMEDIO));
        }

        // Temas normales → buscar cualquier dificultad
        if (!temasNormales.isEmpty()) {
            candidatos.addAll(cuestionarioRepository.findActivosByTemasInteres(temasNormales));
        }

        // Deduplicar por ID
        Map<Long, Cuestionario> dedup = new LinkedHashMap<>();
        candidatos.forEach(q -> dedup.putIfAbsent(q.getId(), q));

        dedup.values().stream()
                .filter(q -> !yaCompletados.contains(q.getId()))
                .limit(TOP_RECOMENDACIONES)
                .forEach(
                        q -> {
                            double r = rend.getOrDefault(q.getMateria().toLowerCase(), 0.6);
                            boolean debil = r < 0.5;
                            double score = debil ? 90.0 : 65.0;
                            String motivo =
                                    debil
                                            ? motivoFactor(FactorRecomendacion.ACTIVIDAD_SIMILAR)
                                                    + " – rendimiento bajo en "
                                                    + q.getMateria()
                                            : "Refuerza lo aprendido sobre " + q.getMateria();
                            guardar(
                                    usuario,
                                    TipoRecomendacion.CUESTIONARIO,
                                    q.getId(), // ID REAL de la BD
                                    q.getTitulo(),
                                    q.getDescripcion(),
                                    q.getImagenUrl(),
                                    score,
                                    motivo,
                                    EXP_QUIZ);
                        });
    }

    // -----------------------------------------------------------------------
    // SCORING
    // -----------------------------------------------------------------------

    /**
     * Score profesor — 6 factores: 0.35 · similitud Jaccard(intereses, especialidades) 0.20 · boost
     * actividad reciente (búsquedas últimos 7 días) 0.10 · boost ubicación geográfica 0.15 · boost
     * feedback propio del usuario 0.15 · boost valoración media de OTROS usuarios ← NUEVO 0.05 ·
     * boost coincidencia nivel educativo ← NUEVO
     */
    private double scoreProfesor(
            double sim, double act, double ubic, double feedback, double valoracion, double nivel) {
        double boostFb = feedback > 0.7 ? 1.0 : feedback < 0.3 ? 0.3 : 0.6;
        return Math.min(
                1.0,
                W_SIMILITUD * sim
                        + W_ACTIVIDAD * act
                        + W_UBICACION * ubic
                        + W_FEEDBACK * boostFb
                        + W_VALORACION * valoracion // ← NUEVO
                        + W_NIVEL * nivel); // ← NUEVO
    }

    /**
     * Boost de valoración media de otros usuarios. Normaliza la media (1-5) a rango 0-1. Si no
     * tiene valoraciones devuelve 0.5 (neutro, no penaliza ni premia).
     */
    private double boostValoracion(Long tutorId) {
        long numValoraciones = valoracionRepository.countByTutorId(tutorId);
        if (numValoraciones == 0) {
            return 0.5; // sin datos → neutro
        }

        Double media = valoracionRepository.findMediaByTutorId(tutorId);
        return media != null ? (media - 1.0) / 4.0 : 0.5; // normaliza 1-5 → 0-1
    }

    /**
     * Boost de nivel educativo: compara Usuario.nivelEstudios con palabras clave en Tutor.bio y
     * Tutor.especialidades.
     *
     * <p>Estrategia: como Tutor no tiene campo de nivel, buscamos las keywords del nivel del
     * usuario en el texto de la bio y las especialidades del tutor. Coincidencia → boost 1.0. Sin
     * coincidencia → 0.0.
     */
    private double boostNivel(Tutor tutor, String nivelUsuario) {
        if (nivelUsuario == null || nivelUsuario.isBlank()) {
            return 0.5; // sin datos → neutro
        }

        // Normalizar nivel del usuario a una de las categorías conocidas
        String categoriaUsuario =
                KEYWORDS_NIVEL.entrySet().stream()
                        .filter(
                                e ->
                                        e.getValue().stream()
                                                .anyMatch(kw -> nivelUsuario.contains(kw)))
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElse(null);

        if (categoriaUsuario == null) {

            return 0.5; // nivel desconocido → neutro
        }

        // Texto del tutor donde buscar: bio + especialidades
        String textoTutor =
                Stream.of(
                                nvl(tutor.getBio()),
                                tutor.getEspecialidades() != null
                                        ? String.join(" ", tutor.getEspecialidades())
                                        : "")
                        .collect(Collectors.joining(" "))
                        .toLowerCase();

        List<String> keywords = KEYWORDS_NIVEL.get(categoriaUsuario);
        boolean coincide = keywords.stream().anyMatch(textoTutor::contains);

        return coincide ? 1.0 : 0.0;
    }

    private double scoreComunidad(Comunidad c, List<String> intereses, String ubicUsuario) {
        List<String> tags = tokenizar(nvl(c.getNombre()) + " " + nvl(c.getDescripcion()));
        double sim = jaccard(intereses, tags);
        double boostUbic =
                ubicUsuario != null
                                && tags.stream()
                                        .anyMatch(
                                                t ->
                                                        t.contains(ubicUsuario)
                                                                || ubicUsuario.contains(t))
                        ? 1.0
                        : 0.0;
        long miembros = miembroRepository.countByComunidadId(c.getId());
        double pop = miembros > 500 ? 1.0 : miembros > 100 ? 0.6 : miembros > 10 ? 0.3 : 0.1;
        return Math.min(
                1.0, 0.50 * sim + 0.25 * (sim > 0 ? 0.5 : 0.0) + 0.15 * boostUbic + 0.10 * pop);
    }

    private double jaccard(List<String> a, List<String> b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }

        Set<String> s1 = a.stream().map(String::toLowerCase).collect(Collectors.toSet());
        Set<String> s2 = b.stream().map(String::toLowerCase).collect(Collectors.toSet());
        Set<String> inter = new HashSet<>(s1);
        inter.retainAll(s2);
        Set<String> union = new HashSet<>(s1);
        union.addAll(s2);
        return union.isEmpty() ? 0.0 : (double) inter.size() / union.size();
    }

    private double boostActividad(Long uid, List<String> materias) {
        if (uid == null || materias == null || materias.isEmpty()) {
            return 0.0;
        }
        List<String> rec =
                actividadRepository.findTemasInteres(uid, LocalDateTime.now().minusDays(7));
        long hits =
                materias.stream()
                        .filter(
                                m ->
                                        rec.stream()
                                                .anyMatch(
                                                        t ->
                                                                t != null
                                                                        && t.toLowerCase()
                                                                                .contains(
                                                                                        m
                                                                                                .toLowerCase())))
                        .count();
        return Math.min(1.0, (double) hits / Math.max(1, materias.size()));
    }

    private double boostUbicacionTutor(Tutor tutor, String ubicUsuario) {
        if (ubicUsuario == null || tutor.getUbicacion() == null) {
            return 0.0;
        }
        String ubicTutor = nombreUbicacion(tutor.getUbicacion());
        if (ubicTutor == null) {
            return 0.0;
        }
        return ubicTutor.equals(ubicUsuario)
                ? 1.0
                : (ubicTutor.contains(ubicUsuario) || ubicUsuario.contains(ubicTutor)) ? 0.5 : 0.0;
    }

    private double tasaFeedbackUtil(Long uid) {
        long utiles = feedbackRepository.countUtilesByUsuario(uid);
        long noUtiles = feedbackRepository.countNoUtilesByUsuario(uid);
        long total = utiles + noUtiles;
        return total == 0 ? 0.5 : (double) utiles / total;
    }

    // -----------------------------------------------------------------------
    // MOTIVOS
    // -----------------------------------------------------------------------

    public String motivoFactor(FactorRecomendacion factor) {
        return switch (factor) {
            case INTERES_SIMILAR -> "Coincide con tus intereses";
            case UBICACION -> "Popular en tu zona";
            case NIVEL_EDUCATIVO -> "Acorde a tu nivel de estudios";
            case COMUNIDAD_SIMILAR -> "Similar a comunidades donde participas";
            case POPULARIDAD -> "Tendencia actual";
            case ACTIVIDAD_SIMILAR -> "Basado en tu actividad reciente";
        };
    }

    /** Motivo del profesor: indica intereses comunes, valoración y nivel si aplica. */
    private String motivoProfesor(
            List<String> intereses,
            List<String> especialidades,
            Double valoracionMedia,
            String nivelUsuario,
            Tutor tutor) {
        List<String> partes = new ArrayList<>();

        // Materias comunes
        if (especialidades != null) {
            List<String> comunes =
                    especialidades.stream()
                            .filter(e -> intereses.stream().anyMatch(i -> i.equalsIgnoreCase(e)))
                            .limit(2)
                            .collect(Collectors.toList());
            if (!comunes.isEmpty()) {
                partes.add("Especializado en " + String.join(", ", comunes));
            }
        }

        // Valoración de otros usuarios
        if (valoracionMedia != null && valoracionMedia >= 4.0) {
            partes.add(String.format("%.1f⭐ valorado por otros usuarios", valoracionMedia));
        }

        // Nivel educativo
        if (!nivelUsuario.isBlank() && boostNivel(tutor, nivelUsuario) == 1.0) {
            partes.add(motivoFactor(FactorRecomendacion.NIVEL_EDUCATIVO));
        }

        if (partes.isEmpty()) {
            return "Tutor recomendado para ti";
        }
        return String.join(" · ", partes);
    }

    private String motivoContenido(Contenido c, List<String> temas) {
        // Encontrar qué tema de interés del usuario disparó esta recomendación
        String temaClave =
                temas.stream()
                        .filter(
                                t ->
                                        c.getMateria().toLowerCase().contains(t.toLowerCase())
                                                || c.getTags().stream()
                                                        .anyMatch(
                                                                tag ->
                                                                        tag.toLowerCase()
                                                                                .contains(
                                                                                        t
                                                                                                .toLowerCase())))
                        .findFirst()
                        .orElse(c.getMateria());
        return motivoFactor(FactorRecomendacion.ACTIVIDAD_SIMILAR) + ": " + temaClave;
    }

    private String motivoComunidad(Comunidad c, List<String> intereses) {
        List<String> tags = tokenizar(nvl(c.getNombre()) + " " + nvl(c.getDescripcion()));
        List<String> comunes =
                intereses.stream()
                        .filter(i -> tags.stream().anyMatch(t -> t.equalsIgnoreCase(i)))
                        .limit(2)
                        .collect(Collectors.toList());
        return comunes.isEmpty()
                ? motivoFactor(FactorRecomendacion.POPULARIDAD)
                : motivoFactor(FactorRecomendacion.INTERES_SIMILAR)
                        + ": "
                        + String.join(", ", comunes);
    }

    // -----------------------------------------------------------------------
    // HELPERS
    // -----------------------------------------------------------------------

    private List<String> interesesEfectivos(Usuario usuario) {
        List<String> intereses =
                usuario.getIntereses() != null
                        ? new ArrayList<>(usuario.getIntereses())
                        : new ArrayList<>();
        actividadRepository
                .findTemasInteres(usuario.getId(), LocalDateTime.now().minusDays(DIAS_LOOKBACK))
                .stream()
                .filter(t -> !intereses.contains(t))
                .limit(10)
                .forEach(intereses::add);
        return intereses;
    }

    private Map<String, Double> rendimientoPorTema(Long uid) {
        return actividadRepository.findRendimientoQuizPorTema(uid).stream()
                .collect(
                        Collectors.toMap(
                                row -> (String) row[0],
                                row -> row[1] != null ? ((Number) row[1]).doubleValue() : 0.6));
    }

    /** Descripción enriquecida del tutor: bio + tarifa + valoración media si existe. */
    private String descripcionTutor(Tutor t, Double valoracionMedia) {
        List<String> partes = new ArrayList<>();
        if (t.getBio() != null && !t.getBio().isBlank()) {
            partes.add(t.getBio());
        }

        if (t.getTarifaHora() != null) {
            partes.add(t.getTarifaHora() + "€/h");
        }

        if (t.getDisponibilidad() != null && !t.getDisponibilidad().isBlank()) {
            partes.add(t.getDisponibilidad());
        }

        if (valoracionMedia != null && valoracionMedia > 0) {
            partes.add(
                    String.format(
                            "%.1f⭐ (%d valoraciones)",
                            valoracionMedia, valoracionRepository.countByTutorId(t.getId())));
        }

        return partes.isEmpty()
                ? "Tutor verificado en " + String.join(", ", t.getEspecialidades())
                : String.join(" · ", partes);
    }

    private String nombreUbicacion(Ubicacion u) {
        return (u != null && u.getNombre() != null) ? u.getNombre().toLowerCase() : null;
    }

    private List<String> tokenizar(String texto) {
        if (texto == null || texto.isBlank()) {
            return Collections.emptyList();
        }

        return Arrays.stream(texto.toLowerCase().split("[\\s,;.]+"))
                .filter(t -> t.length() > 2)
                .distinct()
                .collect(Collectors.toList());
    }

    private String capitalizar(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }

        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    private String nvl(String s) {
        return s != null ? s : "";
    }

    private TipoRecomendacion mapCategoria(String categoria) {
        if (categoria == null) {
            return null;
        }

        return switch (categoria.toUpperCase()) {
            case "TUTOR", "PROFESOR" -> TipoRecomendacion.PROFESOR;
            case "CONTENIDO" -> TipoRecomendacion.CONTENIDO;
            case "CUESTIONARIO", "QUIZ" -> TipoRecomendacion.CUESTIONARIO;
            case "COMUNIDAD" -> TipoRecomendacion.COMUNIDAD;
            default -> null;
        };
    }

    // -----------------------------------------------------------------------
    // PERSISTENCIA
    // -----------------------------------------------------------------------

    private void guardar(
            Usuario usuario,
            TipoRecomendacion tipo,
            Long idObjeto,
            String titulo,
            String descripcion,
            String imagenUrl,
            Double puntuacion,
            String razon,
            long horasExp) {
        if (!recomendacionRepository
                .findByUsuarioTipoObjeto(usuario.getId(), tipo, idObjeto)
                .isEmpty()) {
            return;
        }

        recomendacionRepository.save(
                Recomendacion.builder()
                        .usuario(usuario)
                        .tipo(tipo)
                        .idObjetoRecomendado(idObjeto)
                        .titulo(titulo)
                        .descripcion(descripcion)
                        .imagenUrl(imagenUrl)
                        .puntuacionRelevancia(Math.min(puntuacion, 100.0))
                        .razonRecomendacion(razon)
                        .vista(false)
                        .fechaExpiracion(LocalDateTime.now().plusHours(horasExp))
                        .build());
    }

    private void limpiarExpiradas(Long uid) {
        recomendacionRepository.deleteExpiradas(uid, LocalDateTime.now());
    }

    private List<RecomendacionResponse> topParaTi(Long uid, int n) {
        return recomendacionRepository.findTopParaTi(uid, n).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private List<RecomendacionResponse> porTipo(Long uid, TipoRecomendacion tipo, int size) {
        return recomendacionRepository.findPorTipo(uid, tipo, PageRequest.of(0, size)).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private RecomendacionResponse toResponse(Recomendacion r) {
        return RecomendacionResponse.builder()
                .id(r.getId())
                .tipo(r.getTipo().toString())
                .communityId(r.getIdObjetoRecomendado())
                .nombre(r.getTitulo())
                .descripcion(r.getDescripcion())
                .imagenUrl(r.getImagenUrl())
                .relevancia(r.getPuntuacionRelevancia())
                .motivo(r.getRazonRecomendacion())
                .vista(r.getVista())
                .esFavorable(r.getEsFavorable())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
