package es.us.meerkat.backend.service.events;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.us.meerkat.backend.entity.AsistenciaEvento;
import es.us.meerkat.backend.entity.Comunidad;
import es.us.meerkat.backend.entity.EstadoAsistencia;
import es.us.meerkat.backend.entity.Evento;
import es.us.meerkat.backend.entity.Notificacion;
import es.us.meerkat.backend.entity.PreferenciasNotificacion;
import es.us.meerkat.backend.entity.Ubicacion;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.AsistenciaEventoRepository;
import es.us.meerkat.backend.repository.ComunidadRepository;
import es.us.meerkat.backend.repository.EventoRepository;
import es.us.meerkat.backend.repository.MiembroComunidadRepository;
import es.us.meerkat.backend.repository.UbicacionRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;
import es.us.meerkat.backend.service.communities.AuthorizationService;
import es.us.meerkat.backend.service.emails.EmailService;
import es.us.meerkat.backend.service.google.GoogleCalendarService;
import es.us.meerkat.backend.service.notifications.NotificacionService;
import es.us.meerkat.backend.service.notifications.PreferenciasNotificacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servicio para gestionar la lógica de negocio relacionada con los eventos.
 *
 * <p>Incluye creación, edición, cancelación y obtención de eventos.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventoService {

    /** Repositorio para acceder a la información de eventos. */
    private final EventoRepository eventoRepository;

    /** Repositorio para acceder a la información de asistencia a eventos. */
    private final AsistenciaEventoRepository asistenciaEventoRepository;

    /** Repositorio para acceder a la información de usuarios. */
    private final UsuarioRepository usuarioRepository;

    /** Repositorio para acceder a la información de comunidades. */
    private final ComunidadRepository comunidadRepository;

    private final NotificacionService notificacionService;

    /** Repositorio para acceder a la información de miembros de comunidad. */
    private final MiembroComunidadRepository miembroComunidadRepository;

    /** Repositorio para acceder a la información de ubicaciones. */
    private final UbicacionRepository ubicacionRepository;

    /** Servicio de autorización para verificar roles en comunidades. */
    private final AuthorizationService authorizationService;

    private final GoogleCalendarService googleCalendarService;
    private final EmailService emailService;
    private final PreferenciasNotificacionService preferenciasNotificacionService;

    // ===============================
    // CREAR EVENTO
    // ===============================

    /**
     * Crea un nuevo evento asociado a una comunidad.
     *
     * @param creadorId Identificador del usuario creador.
     * @param comunidadId Identificador de la comunidad.
     * @param tituloParam Título del evento.
     * @param descripcionParam Descripción del evento.
     * @param fechaHoraParam Fecha y hora de inicio.
     * @param fechaFinParam Fecha y hora de fin.
     * @param aforoParam Aforo máximo.
     * @param queLlevarParam Qué llevar al evento.
     * @param esVirtualParam Si es evento virtual.
     * @param privadoParam Si es un evento privado.
     * @param enlaceVirtualParam Enlace virtual (si aplica).
     * @param visibleMapaParam Si es visible en el mapa.
     * @param ubicacionId ID de la ubicación (para eventos presenciales).
     * @return El evento creado.
     */
    @Transactional
    public Evento crearEvento(
            final Long creadorId,
            final Long comunidadId,
            final String tituloParam,
            final String descripcionParam,
            final LocalDateTime fechaHoraParam,
            final LocalDateTime fechaFinParam,
            final Integer aforoParam,
            final String queLlevarParam,
            final Boolean esVirtualParam,
            final Boolean privadoParam,
            final String enlaceVirtualParam,
            final Boolean visibleMapaParam,
            final Long ubicacionId) {

        validarAforo(aforoParam);
        validarFechaInicioNoPasada(fechaHoraParam);

        final Usuario creador =
                usuarioRepository
                        .findById(creadorId)
                        .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        final Comunidad comunidad =
                comunidadRepository
                        .findById(comunidadId)
                        .orElseThrow(() -> new RuntimeException("Comunidad no encontrada"));

        // Verificar que el usuario es miembro de la comunidad
        boolean esMiembro =
                miembroComunidadRepository
                        .findByUsuarioIdAndComunidadId(creadorId, comunidadId)
                        .isPresent();
        if (!esMiembro) {
            throw new RuntimeException(
                    "No puedes crear eventos en una comunidad a la que no perteneces");
        }

        // Validar que la fecha de inicio sea anterior a la fecha de fin
        if (fechaFinParam != null && fechaHoraParam.isAfter(fechaFinParam)) {
            throw new IllegalArgumentException(
                    "La fecha de inicio no puede ser posterior a la fecha de fin");
        }

        final Evento evento = new Evento();
        evento.crear(
                tituloParam,
                descripcionParam,
                fechaHoraParam,
                fechaFinParam,
                aforoParam,
                queLlevarParam,
                esVirtualParam,
                privadoParam);
        evento.setCreador(creador);
        evento.setComunidad(comunidad);
        evento.setEnlaceVirtual(enlaceVirtualParam);
        evento.setVisibleMapa(visibleMapaParam != null ? visibleMapaParam : true);

        // Establecer ubicación si se proporcionó
        if (ubicacionId != null) {
            final Ubicacion ubicacion =
                    ubicacionRepository
                            .findById(ubicacionId)
                            .orElseThrow(() -> new RuntimeException("Ubicación no encontrada"));
            evento.setUbicacion(ubicacion);
        }
        // Guardar el evento primero para obtener su ID
        final Evento savedEvento = eventoRepository.save(evento);

        // Notificar a todos los miembros de la comunidad (excepto el creador)
        List<Long> miembrosIds =
                miembroComunidadRepository.findUsuarioIdsByComunidadId(comunidadId);
        for (Long usuarioId : miembrosIds) {
            if (!usuarioId.equals(creadorId)) {
                Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
                if (usuario != null) {
                    Notificacion notificacion =
                            Notificacion.builder()
                                    .usuario(usuario)
                                    .titulo("Nuevo evento en la comunidad " + comunidad.getNombre())
                                    .mensaje("Se ha creado el evento '" + tituloParam + "'.")
                                    .tipo("EVENTO")
                                    .leida(false)
                                    .comunidadId(comunidad.getId())
                                    .comunidadNombre(comunidad.getNombre())
                                    .comunidadImagenUrl(comunidad.getImagenUrl())
                                    .eventoId(savedEvento.getId())
                                    .build();
                    notificacionService.crearYNotificar(notificacion);
                }
            }
        }

        // Auto-unir al creador como asistente confirmado
        final AsistenciaEvento asistencia = new AsistenciaEvento();
        asistencia.setEvento(savedEvento);
        asistencia.setUsuario(creador);
        asistencia.confirmarAsistencia();
        asistencia.setCreatedAt(LocalDateTime.now());
        asistenciaEventoRepository.save(asistencia);
        savedEvento.setAsistentesConfirmados(1);
        eventoRepository.save(savedEvento);

        try {
            googleCalendarService.sincronizarCreacion(savedEvento);
        } catch (Exception e) {
            // No propagamos el error: el evento se crea aunque GCal falle
        }
        return savedEvento;
    }

    // ===============================
    // EDITAR EVENTO
    // ===============================

    /**
     * Edita un evento existente.
     *
     * @param eventoIdParam Identificador del evento.
     * @param tituloParam Título del evento.
     * @param descripcionParam Descripción del evento.
     * @param fechaInicioParam Fecha y hora de inicio.
     * @param fechaFinParam Fecha y hora de fin.
     * @param aforoParam Aforo máximo.
     * @param queLlevarParam Qué llevar al evento.
     * @param esVirtualParam Si es evento virtual.
     * @param privadoParam Si es un evento privado.
     * @param ubicacionId ID de la ubicación (para eventos presenciales).
     * @return El evento actualizado.
     */
    @Transactional
    public Evento editarEvento(
            final Long eventoIdParam,
            final String tituloParam,
            final String descripcionParam,
            final LocalDateTime fechaInicioParam,
            final LocalDateTime fechaFinParam,
            final Integer aforoParam,
            final String queLlevarParam,
            final Boolean esVirtualParam,
            final Boolean privadoParam,
            final Long ubicacionId,
            final Boolean visibleMapaParam) {

        validarAforo(aforoParam);
        validarFechaInicioNoPasada(fechaInicioParam);

        // Validar que la fecha de inicio sea anterior a la fecha de fin
        if (fechaFinParam != null && fechaInicioParam.isAfter(fechaFinParam)) {
            throw new IllegalArgumentException(
                    "La fecha de inicio no puede ser posterior a la fecha de fin");
        }

        final Evento evento =
                eventoRepository
                        .findById(eventoIdParam)
                        .orElseThrow(() -> new RuntimeException("Evento no encontrado"));

        final String tituloAnterior = evento.getTitulo();
        final String descripcionAnterior = evento.getDescripcion();
        final LocalDateTime fechaHoraAnterior = evento.getFechaHora();
        final LocalDateTime fechaFinAnterior = evento.getFechaFin();
        final Integer aforoAnterior = evento.getAforo();
        final String queLlevarAnterior = evento.getQueLlevar();
        final Boolean esVirtualAnterior = evento.getEsVirtual();
        final Boolean privadoAnterior = evento.getPrivado();
        final Long ubicacionIdAnterior =
                evento.getUbicacion() != null ? evento.getUbicacion().getId() : null;
        final Boolean visibleMapaAnterior = evento.getVisibleMapa();

        validarEventoNoIniciado(evento);

        evento.editar(
                tituloParam,
                descripcionParam,
                fechaInicioParam,
                fechaFinParam,
                aforoParam,
                queLlevarParam,
                esVirtualParam,
                privadoParam);

        // Actualizar ubicación
        if (ubicacionId != null) {
            final Ubicacion ubicacion =
                    ubicacionRepository
                            .findById(ubicacionId)
                            .orElseThrow(() -> new RuntimeException("Ubicación no encontrada"));
            evento.setUbicacion(ubicacion);
        } else if (Boolean.TRUE.equals(esVirtualParam)) {
            evento.setUbicacion(null);
        }
        try {
            googleCalendarService.sincronizarActualizacion(evento);
        } catch (Exception e) {
            // no se cancela si no se conecta

        }

        if (visibleMapaParam != null) {
            evento.setVisibleMapa(visibleMapaParam);
        }

        final Evento eventoActualizado = eventoRepository.save(evento);

        final Long ubicacionIdActual =
                eventoActualizado.getUbicacion() != null
                        ? eventoActualizado.getUbicacion().getId()
                        : null;
        final boolean huboCambios =
                !Objects.equals(tituloAnterior, eventoActualizado.getTitulo())
                        || !Objects.equals(descripcionAnterior, eventoActualizado.getDescripcion())
                        || !Objects.equals(fechaHoraAnterior, eventoActualizado.getFechaHora())
                        || !Objects.equals(fechaFinAnterior, eventoActualizado.getFechaFin())
                        || !Objects.equals(aforoAnterior, eventoActualizado.getAforo())
                        || !Objects.equals(queLlevarAnterior, eventoActualizado.getQueLlevar())
                        || !Objects.equals(esVirtualAnterior, eventoActualizado.getEsVirtual())
                        || !Objects.equals(privadoAnterior, eventoActualizado.getPrivado())
                        || !Objects.equals(ubicacionIdAnterior, ubicacionIdActual)
                        || !Objects.equals(visibleMapaAnterior, eventoActualizado.getVisibleMapa());

        if (huboCambios) {
            // Construir resumen de cambios
            StringBuilder cambios = new StringBuilder();
            if (!Objects.equals(tituloAnterior, eventoActualizado.getTitulo())) {
                cambios.append(
                        "Título: '"
                                + tituloAnterior
                                + "' → '"
                                + eventoActualizado.getTitulo()
                                + "'\n");
            }
            if (!Objects.equals(descripcionAnterior, eventoActualizado.getDescripcion())) {
                cambios.append("Descripción modificada\n");
            }
            if (!Objects.equals(fechaHoraAnterior, eventoActualizado.getFechaHora())) {
                cambios.append(
                        "Fecha de inicio: '"
                                + fechaHoraAnterior
                                + "' → '"
                                + eventoActualizado.getFechaHora()
                                + "'\n");
            }
            if (!Objects.equals(fechaFinAnterior, eventoActualizado.getFechaFin())) {
                cambios.append(
                        "Fecha de fin: '"
                                + fechaFinAnterior
                                + "' → '"
                                + eventoActualizado.getFechaFin()
                                + "'\n");
            }
            if (!Objects.equals(aforoAnterior, eventoActualizado.getAforo())) {
                cambios.append(
                        "Aforo: '"
                                + aforoAnterior
                                + "' → '"
                                + eventoActualizado.getAforo()
                                + "'\n");
            }
            if (!Objects.equals(queLlevarAnterior, eventoActualizado.getQueLlevar())) {
                String antes =
                        (queLlevarAnterior == null || queLlevarAnterior.trim().isEmpty())
                                ? "vacío"
                                : queLlevarAnterior;
                String despues =
                        (eventoActualizado.getQueLlevar() == null
                                        || eventoActualizado.getQueLlevar().trim().isEmpty())
                                ? "vacío"
                                : eventoActualizado.getQueLlevar();
                cambios.append("Qué llevar: '" + antes + "' → '" + despues + "'\n");
            }
            if (!Objects.equals(esVirtualAnterior, eventoActualizado.getEsVirtual())) {
                cambios.append(
                        "Tipo: "
                                + (eventoActualizado.getEsVirtual() ? "Virtual" : "Presencial")
                                + "\n");
            }
            if (!Objects.equals(privadoAnterior, eventoActualizado.getPrivado())) {
                cambios.append(
                        "Privacidad: "
                                + (eventoActualizado.getPrivado() ? "Privado" : "Público")
                                + "\n");
            }
            if (!Objects.equals(ubicacionIdAnterior, ubicacionIdActual)) {
                cambios.append("Ubicación modificada\n");
            }
            if (!Objects.equals(visibleMapaAnterior, eventoActualizado.getVisibleMapa())) {
                cambios.append(
                        "Visibilidad en mapa: "
                                + (eventoActualizado.getVisibleMapa() ? "Sí" : "No")
                                + "\n");
            }
            String resumenCambios =
                    cambios.length() > 0 ? cambios.toString() : "Detalles no disponibles.";

            // Notificar a todos los miembros de la comunidad (excepto el creador)
            Comunidad comunidad = eventoActualizado.getComunidad();
            List<Long> miembrosIds =
                    miembroComunidadRepository.findUsuarioIdsByComunidadId(comunidad.getId());
            for (Long usuarioId : miembrosIds) {
                if (!usuarioId.equals(eventoActualizado.getCreador().getId())) {
                    Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
                    if (usuario != null) {
                        Notificacion notificacion =
                                Notificacion.builder()
                                        .usuario(usuario)
                                        .titulo(
                                                "Evento actualizado en la comunidad "
                                                        + comunidad.getNombre())
                                        .mensaje(
                                                "El evento '"
                                                        + eventoActualizado.getTitulo()
                                                        + "' ha sido actualizado.\n\nCambios:\n"
                                                        + resumenCambios)
                                        .tipo("EVENTO")
                                        .leida(false)
                                        .comunidadId(comunidad.getId())
                                        .comunidadNombre(comunidad.getNombre())
                                        .comunidadImagenUrl(comunidad.getImagenUrl())
                                        .eventoId(eventoActualizado.getId())
                                        .build();
                        notificacionService.crearYNotificar(notificacion);
                    }
                }
            }
            notificarCambioEvento(eventoActualizado);
        }

        return eventoActualizado;
    }

    private void notificarCambioEvento(final Evento evento) {
        final List<AsistenciaEvento> asistencias =
                asistenciaEventoRepository.findByEventoIdAndEstado(
                        evento.getId(), EstadoAsistencia.CONFIRMADA);

        if (asistencias == null || asistencias.isEmpty()) {
            log.info(
                    "Evento {} actualizado sin asistentes confirmados para notificar",
                    evento.getId());
            return;
        }

        int notificados = 0;

        for (final AsistenciaEvento asistencia : asistencias) {
            final Usuario asistente = asistencia.getUsuario();
            if (asistente == null || asistente.getId() == null || asistente.getEmail() == null) {
                continue;
            }

            if (evento.getCreador() != null
                    && evento.getCreador().getId() != null
                    && evento.getCreador().getId().equals(asistente.getId())) {
                continue;
            }

            try {
                final PreferenciasNotificacion preferencias =
                        preferenciasNotificacionService.getOrCreate(asistente.getId());
                final boolean puedeRecibir =
                        Boolean.TRUE.equals(preferencias.getEmailsActivados())
                                && Boolean.TRUE.equals(preferencias.getNotificarCambiosDeEventos());

                if (!puedeRecibir) {
                    continue;
                }

                emailService.sendEventUpdatedEmail(asistente, evento);
                notificados++;
            } catch (Exception e) {
                log.warn(
                        "No se pudo notificar cambio de evento {} al usuario {}: {}",
                        evento.getId(),
                        asistente.getId(),
                        e.getMessage());
            }
        }

        log.info(
                "Evento {} actualizado: {} asistentes confirmados, {} correos enviados",
                evento.getId(),
                asistencias.size(),
                notificados);
    }

    // ===============================
    // CANCELAR EVENTO
    // ===============================

    /**
     * Cancela un evento existente.
     *
     * @param eventoIdParam Identificador del evento.
     * @param motivoParam Motivo de la cancelación.
     * @return El evento cancelado.
     */
    @Transactional
    public Evento cancelarEvento(final Long eventoIdParam, final String motivoParam) {
        final Evento evento =
                eventoRepository
                        .findById(eventoIdParam)
                        .orElseThrow(() -> new RuntimeException("Evento no encontrado"));

        validarEventoNoIniciado(evento);

        evento.cancelar(motivoParam);

        try {
            googleCalendarService.sincronizarCancelacion(evento);
        } catch (Exception e) {
            // log.warn("Error sincronizando con Google Calendar al cancelar: {}",
            // e.getMessage());
        }
        return eventoRepository.save(evento);
    }

    private void validarEventoNoIniciado(final Evento evento) {
        if (evento.getFechaHora() != null && !evento.getFechaHora().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException(
                    "No se puede modificar o cancelar un evento que ya ha comenzado");
        }
    }

    private void validarAforo(final Integer aforo) {
        if (aforo == null || aforo < 1 || aforo > 999) {
            throw new IllegalArgumentException("El aforo debe ser un numero entre 1 y 999");
        }
    }

    private void validarFechaInicioNoPasada(final LocalDateTime fechaInicio) {
        if (fechaInicio == null) {
            throw new IllegalArgumentException("La fecha de inicio es obligatoria");
        }

        if (fechaInicio.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "La fecha y hora de inicio no puede ser anterior a la fecha y hora actual");
        }
    }

    // ===============================
    // OBTENER EVENTO
    // ===============================

    /**
     * Obtiene un evento por su ID, verificando permisos de visibilidad.
     *
     * @param eventoIdParam Identificador del evento.
     * @param usuarioId Identificador del usuario que solicita (puede ser null).
     * @return El evento encontrado.
     */
    public Evento obtenerEvento(final Long eventoIdParam, final Long usuarioId) {
        final Evento evento =
                eventoRepository
                        .findById(eventoIdParam)
                        .orElseThrow(() -> new RuntimeException("Evento no encontrado"));

        if (Boolean.TRUE.equals(evento.getPrivado())) {
            if (!puedeVerEventoPrivado(evento, usuarioId)) {
                throw new RuntimeException("No tienes permiso para ver este evento privado");
            }
        }

        return evento;
    }

    /**
     * Obtiene un evento por su ID sin verificar visibilidad (uso interno).
     *
     * @param eventoIdParam Identificador del evento.
     * @return El evento encontrado.
     */
    public Evento obtenerEventoInterno(final Long eventoIdParam) {
        return eventoRepository
                .findById(eventoIdParam)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado"));
    }

    /**
     * Obtiene todos los eventos públicos.
     *
     * @return Lista de eventos públicos.
     */
    public List<Evento> obtenerEventosPublicos() {
        return eventoRepository.findPublicEvents(LocalDateTime.now());
    }

    /**
     * Obtiene todos los eventos visibles en el mapa (públicos, no cancelados). Opcionalmente filtra
     * por radio de distancia respecto a una ubicación.
     *
     * @param lat Latitud del centro de búsqueda (opcional).
     * @param lon Longitud del centro de búsqueda (opcional).
     * @param radioKm Radio de búsqueda en kilómetros (opcional).
     * @return Lista de eventos visibles en mapa.
     */
    public List<Evento> obtenerEventosEnMapa(
            final Double lat, final Double lon, final Double radioKm) {
        List<Evento> eventos = eventoRepository.findVisibleOnMap(LocalDateTime.now());
        if (lat == null || lon == null || radioKm == null) {
            return eventos;
        }
        return eventos.stream()
                .filter(
                        evento -> {
                            Ubicacion ubicacion = evento.getUbicacion();
                            if (ubicacion == null
                                    || ubicacion.getLatitud() == null
                                    || ubicacion.getLongitud() == null) {
                                return false;
                            }
                            double distancia =
                                    calcularDistanciaKm(
                                            lat,
                                            lon,
                                            ubicacion.getLatitud(),
                                            ubicacion.getLongitud());
                            return distancia <= radioKm;
                        })
                .collect(Collectors.toList());
    }

    /**
     * Obtiene los nombres de ubicaciones con eventos activos dentro de un radio dado.
     *
     * @param lat Latitud del centro de búsqueda.
     * @param lon Longitud del centro de búsqueda.
     * @param radioKm Radio de búsqueda en kilómetros.
     * @return Lista de nombres de ubicaciones recomendadas.
     */
    @Transactional(readOnly = true)
    public List<String> obtenerUbicacionesRecomendadas(
            final Double lat, final Double lon, final Double radioKm) {
        if (lat == null || lon == null || radioKm == null) {
            return List.of();
        }
        List<Evento> eventos = eventoRepository.findVisibleOnMap(LocalDateTime.now());
        return eventos.stream()
                .filter(
                        e ->
                                e.getUbicacion() != null
                                        && e.getUbicacion().getLatitud() != null
                                        && e.getUbicacion().getLongitud() != null)
                .filter(
                        e ->
                                calcularDistanciaKm(
                                                lat,
                                                lon,
                                                e.getUbicacion().getLatitud(),
                                                e.getUbicacion().getLongitud())
                                        <= radioKm)
                .map(e -> e.getUbicacion().getNombre())
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Calcula la distancia en kilómetros entre dos puntos geográficos usando la fórmula de
     * Haversine.
     */
    private double calcularDistanciaKm(
            final double lat1, final double lon1, final double lat2, final double lon2) {
        final double radioTierra = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a =
                Math.sin(dLat / 2) * Math.sin(dLat / 2)
                        + Math.cos(Math.toRadians(lat1))
                                * Math.cos(Math.toRadians(lat2))
                                * Math.sin(dLon / 2)
                                * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return radioTierra * c;
    }

    /**
     * Genera un enlace virtual para un evento.
     *
     * @param eventoIdParam Identificador del evento.
     * @return El enlace virtual generado.
     */
    @Transactional
    public String generarEnlaceVirtual(final Long eventoIdParam) {
        final Evento evento =
                eventoRepository
                        .findById(eventoIdParam)
                        .orElseThrow(() -> new RuntimeException("Evento no encontrado"));

        return evento.generarEnlaceVirtual();
    }

    // ===============================
    // EVENTOS POR COMUNIDAD
    // ===============================

    // ===============================
    // VINCULAR TAREA CLASSROOM
    // ===============================

    /**
     * Vincula una tarea de Google Classroom a un evento.
     *
     * @param eventoId Identificador del evento.
     * @param usuarioId Identificador del usuario que solicita.
     * @param taskId ID de la tarea en Classroom.
     * @param title Título de la tarea en Classroom.
     * @param url URL de la tarea en Classroom.
     * @return El evento actualizado.
     */
    @Transactional
    public Evento vincularTareaClassroom(
            final Long eventoId,
            final Long usuarioId,
            final String taskId,
            final String title,
            final String url) {
        final Evento evento = obtenerEventoInterno(eventoId);

        if (!evento.getCreador().getId().equals(usuarioId)) {
            throw new RuntimeException("Solo el creador del evento puede vincular tareas");
        }

        if (evento.getComunidad() == null) {
            throw new RuntimeException(
                    "El evento debe pertenecer a una comunidad para vincular una tarea");
        }

        evento.setClassroomTaskId(taskId);
        evento.setClassroomTaskTitle(title);
        evento.setClassroomTaskUrl(url);

        return eventoRepository.save(evento);
    }

    /**
     * Desvincula una tarea de Google Classroom de un evento.
     *
     * @param eventoId Identificador del evento.
     * @param usuarioId Identificador del usuario que solicita.
     * @return El evento actualizado.
     */
    @Transactional
    public Evento desvincularTareaClassroom(final Long eventoId, final Long usuarioId) {
        final Evento evento = obtenerEventoInterno(eventoId);

        if (!evento.getCreador().getId().equals(usuarioId)) {
            throw new RuntimeException("Solo el creador del evento puede desvincular tareas");
        }

        evento.setClassroomTaskId(null);
        evento.setClassroomTaskTitle(null);
        evento.setClassroomTaskUrl(null);

        return eventoRepository.save(evento);
    }

    /**
     * Obtiene los eventos de una comunidad, filtrando eventos privados según permisos.
     *
     * @param comunidadId Identificador de la comunidad.
     * @param incluirCancelados Si se deben incluir los eventos cancelados.
     * @param usuarioId Identificador del usuario que solicita (puede ser null).
     * @return Lista de eventos de la comunidad visibles para el usuario.
     */
    public List<Evento> obtenerEventosPorComunidad(
            final Long comunidadId, final boolean incluirCancelados, final Long usuarioId) {
        List<Evento> eventos;
        if (incluirCancelados) {
            eventos = eventoRepository.findByComunidadId(comunidadId);
        } else {
            final LocalDateTime ahora = LocalDateTime.now();
            eventos =
                    eventoRepository.findByComunidadIdAndCanceladoFalseAndFuture(
                            comunidadId, ahora, ahora.minusHours(2), usuarioId);
        }
        return filtrarEventosPrivados(eventos, usuarioId);
    }

    /**
     * Verifica si un usuario puede ver un evento privado. Solo los miembros de la comunidad pueden
     * verlo.
     */
    private boolean puedeVerEventoPrivado(final Evento evento, final Long usuarioId) {
        if (usuarioId == null) {
            return false;
        }
        if (evento.getComunidad() != null) {
            return authorizationService.isMemberOf(usuarioId, evento.getComunidad().getId());
        }
        return false;
    }

    /** Filtra una lista de eventos eliminando los privados que el usuario no puede ver. */
    private List<Evento> filtrarEventosPrivados(final List<Evento> eventos, final Long usuarioId) {
        return eventos.stream()
                .filter(
                        evento ->
                                !Boolean.TRUE.equals(evento.getPrivado())
                                        || puedeVerEventoPrivado(evento, usuarioId))
                .collect(Collectors.toList());
    }
}
