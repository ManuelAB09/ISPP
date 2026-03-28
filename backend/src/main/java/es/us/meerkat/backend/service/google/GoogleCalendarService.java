package es.us.meerkat.backend.service.google;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Event.Reminders;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;
import com.google.api.services.calendar.model.FreeBusyRequest;
import com.google.api.services.calendar.model.FreeBusyRequestItem;
import com.google.api.services.calendar.model.FreeBusyResponse;
import com.google.api.services.calendar.model.TimePeriod;

import es.us.meerkat.backend.config.GoogleCalendarConfig;
import es.us.meerkat.backend.dto.google.GoogleCalendarStatusResponse;
import es.us.meerkat.backend.dto.google.UpdateCalendarPreferenciasRequest;
import es.us.meerkat.backend.dto.tutors.AvailabilitySlot;
import es.us.meerkat.backend.entity.Evento;
import es.us.meerkat.backend.entity.GoogleCalendarBooking;
import es.us.meerkat.backend.entity.GoogleCalendarEvento;
import es.us.meerkat.backend.entity.GoogleCalendarToken;
import es.us.meerkat.backend.entity.SolicitudContratacionDirecta;
import es.us.meerkat.backend.entity.TipoEvento;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.google.GoogleCalendarBookingRepository;
import es.us.meerkat.backend.repository.google.GoogleCalendarEventoRepository;
import es.us.meerkat.backend.repository.google.GoogleCalendarTokenRepository;
import es.us.meerkat.backend.repository.users.UsuarioRepository;
import es.us.meerkat.backend.service.events.EventoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servicio que gestiona toda la integración con Google Calendar.
 *
 * <p>Responsabilidades:
 *
 * <ol>
 *   <li>Flujo OAuth: generar URL de autorización y procesar el callback.
 *   <li>Gestión de tokens: renovar accessToken cuando caduca.
 *   <li>CRUD en Google Calendar: crear, actualizar y eliminar eventos.
 *   <li>Gestión de preferencias: qué tipos sincronizar, activar/desactivar.
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleCalendarService {

    private static final String CALENDAR_PRIMARY = "primary";
    private static final String ZONA_HORARIA = "Europe/Madrid";

    private final GoogleCalendarConfig calendarConfig;
    private final GoogleCalendarTokenRepository tokenRepository;
    private final GoogleCalendarEventoRepository calendarEventoRepository;
    private final GoogleCalendarBookingRepository calendarBookingRepository;
    private final UsuarioRepository usuarioRepository;

    @Value("${app.url:http://localhost:3000}")
    private String appUrl;

    // ===============================
    // OAUTH 2.0
    // ===============================

    /**
     * Genera la URL de autorización de Google a la que hay que redirigir al usuario. El frontend
     * recibe esta URL y redirige al usuario (o abre una ventana emergente).
     *
     * @param usuarioId ID del usuario, se pasa como state para recuperarlo en el callback.
     * @return URL de autorización de Google.
     */
    public String generarUrlAutorizacion(final Long usuarioId) throws Exception {
        final GoogleAuthorizationCodeFlow flow = buildFlow();
        final GoogleAuthorizationCodeRequestUrl url =
                flow.newAuthorizationUrl()
                        .setRedirectUri(calendarConfig.getRedirectUri())
                        .setState(String.valueOf(usuarioId))
                        .set("access_type", "offline") // para obtener refreshToken
                        .set("prompt", "consent"); // fuerza refreshToken en cada autorización

        return url.build();
    }

    /**
     * Procesa el callback de Google OAuth. Intercambia el código de autorización por tokens y los
     * guarda.
     *
     * @param code Código de autorización recibido de Google.
     * @param usuarioId ID del usuario extraído del state.
     */
    @Transactional
    public void procesarCallback(final String code, final Long usuarioId) throws Exception {
        final GoogleAuthorizationCodeFlow flow = buildFlow();
        final GoogleTokenResponse tokenResponse =
                flow.newTokenRequest(code)
                        .setRedirectUri(calendarConfig.getRedirectUri())
                        .execute();

        final Usuario usuario =
                usuarioRepository
                        .findById(usuarioId)
                        .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Buscar token existente o crear uno nuevo
        final GoogleCalendarToken token =
                tokenRepository
                        .findByUsuarioId(usuarioId)
                        .orElseGet(
                                () -> {
                                    final GoogleCalendarToken t = new GoogleCalendarToken();
                                    t.setUsuario(usuario);
                                    return t;
                                });

        token.setAccessToken(tokenResponse.getAccessToken());
        // Google solo devuelve refreshToken la primera vez (o si se forzó con
        // prompt=consent)
        if (tokenResponse.getRefreshToken() != null) {
            token.setRefreshToken(tokenResponse.getRefreshToken());
        }
        token.setExpiresAt(LocalDateTime.now().plusSeconds(tokenResponse.getExpiresInSeconds()));
        token.setSincronizacionActiva(true);

        tokenRepository.save(token);
        log.info("Google Calendar conectado para usuario {}", usuarioId);
    }

    /**
     * Desconecta Google Calendar del usuario. Elimina tokens y todos los mapeos de eventos.
     *
     * @param usuarioId ID del usuario.
     */
    @Transactional
    public void desconectar(final Long usuarioId) {
        calendarEventoRepository.deleteByUsuarioId(usuarioId);
        calendarBookingRepository.deleteByUsuarioId(usuarioId);
        tokenRepository.deleteByUsuarioId(usuarioId);
        log.info("Google Calendar desconectado para usuario {}", usuarioId);
    }

    // ===============================
    // ESTADO Y PREFERENCIAS
    // ===============================

    /** Devuelve el estado actual de la conexión y preferencias del usuario. */
    public GoogleCalendarStatusResponse obtenerEstado(final Long usuarioId) {
        final Optional<GoogleCalendarToken> tokenOpt = tokenRepository.findByUsuarioId(usuarioId);

        if (tokenOpt.isEmpty()) {
            return GoogleCalendarStatusResponse.builder()
                    .conectado(false)
                    .sincronizacionActiva(false)
                    .build();
        }

        final GoogleCalendarToken token = tokenOpt.get();
        return GoogleCalendarStatusResponse.builder()
                .conectado(true)
                .sincronizacionActiva(token.getSincronizacionActiva())
                .tiposSincronizados(parseTipos(token.getTiposEventoSincronizados()))
                .ultimaSincronizacion(token.getUpdatedAt())
                .build();
    }

    /**
     * Actualiza las preferencias de sincronización del usuario.
     *
     * @param usuarioId ID del usuario.
     * @param request Nuevas preferencias.
     */
    @Transactional
    public GoogleCalendarStatusResponse actualizarPreferencias(
            final Long usuarioId, final UpdateCalendarPreferenciasRequest request) {

        final GoogleCalendarToken token =
                tokenRepository
                        .findByUsuarioId(usuarioId)
                        .orElseThrow(
                                () -> new RuntimeException("Google Calendar no está conectado"));

        if (request.getSincronizacionActiva() != null) {
            token.setSincronizacionActiva(request.getSincronizacionActiva());
        }
        if (request.getTiposSincronizados() != null) {
            token.setTiposEventoSincronizados(
                    request.getTiposSincronizados().isEmpty()
                            ? null // null = todos
                            : request.getTiposSincronizados().stream()
                                    .map(Enum::name)
                                    .collect(Collectors.joining(",")));
        }

        tokenRepository.save(token);
        return obtenerEstado(usuarioId);
    }

    // ===============================
    // SINCRONIZACIÓN DE EVENTOS
    // ===============================

    /**
     * Crea un evento en Google Calendar para todos los usuarios que tienen la sincronización activa
     * y quieren sincronizar ese tipo de evento.
     *
     * <p>Llamado desde {@link EventoService} tras crear un evento.
     *
     * @param evento Evento de la plataforma recién creado.
     */
    @Transactional
    public void sincronizarCreacion(final Evento evento) {
        final List<Usuario> miembros = obtenerMiembrosConCalendar(evento);
        miembros.forEach(
                usuario -> {
                    try {
                        crearEventoParaUsuario(evento, usuario);
                    } catch (Exception e) {
                        log.error(
                                "Error creando evento en GCal para usuario {}: {}",
                                usuario.getId(),
                                e.getMessage());
                    }
                });
    }

    /**
     * Actualiza un evento en Google Calendar para todos los usuarios que lo tienen sincronizado.
     *
     * <p>Llamado desde {@link EventoService} tras editar un evento.
     *
     * @param evento Evento de la plataforma ya actualizado.
     */
    @Transactional
    public void sincronizarActualizacion(final Evento evento) {
        final List<GoogleCalendarEvento> mapeos =
                calendarEventoRepository.findByEventoId(evento.getId());

        mapeos.forEach(
                mapeo -> {
                    try {
                        actualizarEventoParaUsuario(evento, mapeo);
                    } catch (Exception e) {
                        log.error(
                                "Error actualizando evento en GCal para usuario {}: {}",
                                mapeo.getUsuario().getId(),
                                e.getMessage());
                    }
                });
    }

    /**
     * Cancela (elimina) un evento en Google Calendar para todos los usuarios.
     *
     * <p>Llamado desde {@link EventoService} al cancelar un evento.
     *
     * @param evento Evento cancelado.
     */
    @Transactional
    public void sincronizarCancelacion(final Evento evento) {
        final List<GoogleCalendarEvento> mapeos =
                calendarEventoRepository.findByEventoId(evento.getId());

        mapeos.forEach(
                mapeo -> {
                    try {
                        eliminarEventoParaUsuario(mapeo);
                    } catch (Exception e) {
                        log.error(
                                "Error eliminando evento en GCal para usuario {}: {}",
                                mapeo.getUsuario().getId(),
                                e.getMessage());
                    }
                });
    }

    /**
     * Sincroniza un evento en Google Calendar para un usuario concreto. Usado cuando un usuario
     * confirma asistencia a un evento. Si el usuario no tiene GCal conectado o ya lo tiene
     * sincronizado, no hace nada.
     *
     * @param evento Evento de la plataforma.
     * @param usuario Usuario que confirma asistencia.
     */
    @Transactional
    public void sincronizarParaUsuario(final Evento evento, final Usuario usuario) {
        final Optional<GoogleCalendarToken> tokenOpt =
                tokenRepository.findByUsuarioId(usuario.getId());

        if (tokenOpt.isEmpty() || !Boolean.TRUE.equals(tokenOpt.get().getSincronizacionActiva())) {
            return;
        }

        if (calendarEventoRepository
                .findByEventoIdAndUsuarioId(evento.getId(), usuario.getId())
                .isPresent()) {
            return;
        }

        try {
            crearEventoParaUsuario(evento, usuario);
        } catch (Exception e) {
            log.error(
                    "Error sincronizando evento {} en GCal para usuario {}: {}",
                    evento.getId(),
                    usuario.getId(),
                    e.getMessage());
        }
    }

    /**
     * Elimina un evento de Google Calendar para un usuario concreto. Usado cuando un usuario
     * cancela su asistencia a un evento.
     *
     * @param evento Evento de la plataforma.
     * @param usuario Usuario que cancela asistencia.
     */
    @Transactional
    public void desincronizarParaUsuario(final Evento evento, final Usuario usuario) {
        final Optional<GoogleCalendarEvento> mapeoOpt =
                calendarEventoRepository.findByEventoIdAndUsuarioId(
                        evento.getId(), usuario.getId());

        if (mapeoOpt.isEmpty()) {
            return;
        }

        try {
            eliminarEventoParaUsuario(mapeoOpt.get());
        } catch (Exception e) {
            log.error(
                    "Error desincronizando evento {} de GCal para usuario {}: {}",
                    evento.getId(),
                    usuario.getId(),
                    e.getMessage());
        }
    }

    // ===============================
    // OPERACIONES INDIVIDUALES
    // ===============================

    private void crearEventoParaUsuario(final Evento evento, final Usuario usuario)
            throws Exception {

        final GoogleCalendarToken token =
                tokenRepository
                        .findByUsuarioId(usuario.getId())
                        .orElseThrow(() -> new RuntimeException("Token no encontrado"));

        if (!token.sincronizaTipo(
                evento.getTipoEvento() != null ? evento.getTipoEvento() : TipoEvento.OTRO)) {
            return;
        }

        final Calendar calendar = getCalendarClient(token);
        final Event gcalEvent = buildGcalEvent(evento);
        final Event creado = calendar.events().insert(CALENDAR_PRIMARY, gcalEvent).execute();

        // Guardar el mapeo
        final GoogleCalendarEvento mapeo = new GoogleCalendarEvento();
        mapeo.setGoogleEventId(creado.getId());
        mapeo.setEvento(evento);
        mapeo.setUsuario(usuario);
        calendarEventoRepository.save(mapeo);

        log.info(
                "Evento {} creado en GCal para usuario {} → gcalId={}",
                evento.getId(),
                usuario.getId(),
                creado.getId());
    }

    private void actualizarEventoParaUsuario(final Evento evento, final GoogleCalendarEvento mapeo)
            throws Exception {

        final GoogleCalendarToken token =
                tokenRepository
                        .findByUsuarioId(mapeo.getUsuario().getId())
                        .orElseThrow(() -> new RuntimeException("Token no encontrado"));

        final Calendar calendar = getCalendarClient(token);
        final Event gcalEvent = buildGcalEvent(evento);

        calendar.events().update(CALENDAR_PRIMARY, mapeo.getGoogleEventId(), gcalEvent).execute();

        calendarEventoRepository.save(mapeo); // actualiza lastSyncAt via @PreUpdate
        log.info(
                "Evento {} actualizado en GCal gcalId={}",
                evento.getId(),
                mapeo.getGoogleEventId());
    }

    private void eliminarEventoParaUsuario(final GoogleCalendarEvento mapeo) throws Exception {
        final GoogleCalendarToken token =
                tokenRepository
                        .findByUsuarioId(mapeo.getUsuario().getId())
                        .orElseThrow(() -> new RuntimeException("Token no encontrado"));

        final Calendar calendar = getCalendarClient(token);

        try {
            calendar.events().delete(CALENDAR_PRIMARY, mapeo.getGoogleEventId()).execute();
        } catch (Exception e) {
            // Si el evento ya no existe en GCal, ignoramos el error
            log.warn(
                    "Evento gcalId={} no encontrado en GCal al intentar eliminar: {}",
                    mapeo.getGoogleEventId(),
                    e.getMessage());
        }

        calendarEventoRepository.delete(mapeo);
        log.info("Evento gcalId={} eliminado de GCal", mapeo.getGoogleEventId());
    }

    // ===============================
    // CONSTRUCCIÓN DEL EVENTO GCAL
    // ===============================

    /**
     * Construye el objeto Event de Google Calendar a partir de un evento de la plataforma. Incluye
     * título, descripción, ubicación/enlace, fechas y recordatorios.
     */
    private Event buildGcalEvent(final Evento evento) {
        final TipoEvento tipo =
                evento.getTipoEvento() != null ? evento.getTipoEvento() : TipoEvento.OTRO;

        // Título con icono y tipo
        final String titulo =
                String.format("%s [%s] %s", tipo.getIcono(), tipo.getNombre(), evento.getTitulo());

        // Descripción completa
        final StringBuilder desc = new StringBuilder();
        if (evento.getComunidad() != null) {
            desc.append("Comunidad: ").append(evento.getComunidad().getNombre()).append("\n\n");
        }
        if (evento.getDescripcion() != null && !evento.getDescripcion().isBlank()) {
            desc.append(evento.getDescripcion()).append("\n\n");
        }
        if (evento.getQueLlevar() != null && !evento.getQueLlevar().isBlank()) {
            desc.append("🎒 Qué llevar: ").append(evento.getQueLlevar()).append("\n");
        }
        if (Boolean.TRUE.equals(evento.getEsVirtual()) && evento.getEnlaceVirtual() != null) {
            desc.append("💻 Enlace: ").append(evento.getEnlaceVirtual());
        }

        final Event gcalEvent =
                new Event().setSummary(titulo).setDescription(desc.toString().trim());

        // Fechas
        final ZoneId zona = ZoneId.of(ZONA_HORARIA);
        if (evento.getFechaHora() != null) {
            gcalEvent.setStart(toEventDateTime(evento.getFechaHora(), zona));
        }
        if (evento.getFechaFin() != null) {
            gcalEvent.setEnd(toEventDateTime(evento.getFechaFin(), zona));
        } else if (evento.getFechaHora() != null) {
            // Si no hay fin, poner 1 hora después
            gcalEvent.setEnd(toEventDateTime(evento.getFechaHora().plusHours(1), zona));
        }

        // Ubicación: enlace virtual o dirección física
        if (Boolean.TRUE.equals(evento.getEsVirtual()) && evento.getEnlaceVirtual() != null) {
            gcalEvent.setLocation(evento.getEnlaceVirtual());
        } else if (evento.getUbicacion() != null) {
            final String loc =
                    evento.getUbicacion().getDireccion() != null
                            ? evento.getUbicacion().getNombre()
                                    + ", "
                                    + evento.getUbicacion().getDireccion()
                            : evento.getUbicacion().getNombre();
            gcalEvent.setLocation(loc);
        }

        // Recordatorios: 24h y 1h antes (email + popup)
        final EventReminder reminder24h =
                new EventReminder().setMethod("email").setMinutes(24 * 60);
        final EventReminder reminder1h = new EventReminder().setMethod("popup").setMinutes(60);
        final EventReminder reminder15min = new EventReminder().setMethod("popup").setMinutes(15);

        gcalEvent.setReminders(
                new Reminders()
                        .setUseDefault(false)
                        .setOverrides(List.of(reminder24h, reminder1h, reminder15min)));

        return gcalEvent;
    }

    // ===============================
    // HELPERS
    // ===============================

    /** Renueva el accessToken si ha caducado y devuelve el cliente Calendar listo para usar. */
    private Calendar getCalendarClient(final GoogleCalendarToken token) throws Exception {
        if (token.accessTokenCaducado()) {
            renovarAccessToken(token);
        }
        return calendarConfig.buildCalendarClient(token.getAccessToken(), token.getRefreshToken());
    }

    /** Renueva el accessToken usando el refreshToken. */
    @Transactional
    protected void renovarAccessToken(final GoogleCalendarToken token) throws Exception {
        final com.google.api.client.auth.oauth2.TokenResponse response =
                new com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest(
                                GoogleNetHttpTransport.newTrustedTransport(),
                                GsonFactory.getDefaultInstance(),
                                token.getRefreshToken(),
                                calendarConfig.getClientId(),
                                calendarConfig.getClientSecret())
                        .execute();

        token.setAccessToken(response.getAccessToken());
        token.setExpiresAt(LocalDateTime.now().plusSeconds(response.getExpiresInSeconds()));
        tokenRepository.save(token);
        log.debug("AccessToken renovado para usuario {}", token.getUsuario().getId());
    }

    private GoogleAuthorizationCodeFlow buildFlow() throws Exception {
        return new GoogleAuthorizationCodeFlow.Builder(
                        GoogleNetHttpTransport.newTrustedTransport(),
                        GsonFactory.getDefaultInstance(),
                        calendarConfig.getClientId(),
                        calendarConfig.getClientSecret(),
                        Collections.singletonList(GoogleCalendarConfig.CALENDAR_SCOPE))
                .setAccessType("offline")
                .build();
    }

    private EventDateTime toEventDateTime(final LocalDateTime ldt, final ZoneId zona) {
        final ZonedDateTime zdt = ldt.atZone(zona);
        return new EventDateTime()
                .setDateTime(
                        new com.google.api.client.util.DateTime(
                                java.util.Date.from(zdt.toInstant())))
                .setTimeZone(ZONA_HORARIA);
    }

    private List<Usuario> obtenerMiembrosConCalendar(final Evento evento) {
        if (evento.getComunidad() == null) {
            return Collections.emptyList();
        }

        return usuarioRepository.findMiembrosConCalendarActivoByComunidadId(
                evento.getComunidad().getId());
    }

    private List<TipoEvento> parseTipos(final String csv) {
        if (csv == null || csv.isBlank()) {
            return Collections.emptyList();
        }

        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(TipoEvento::valueOf)
                .collect(Collectors.toList());
    }

    /**
     * Consulta los intervalos "ocupados" (busy) del calendario del usuario entre dos instantes.
     * Devuelve pares [start,end] como LocalDateTime en la zona configurada.
     */
    public List<AvailabilitySlot> obtenerBusyIntervals(
            final Long usuarioId, final LocalDateTime desde, final LocalDateTime hasta)
            throws Exception {

        final Optional<GoogleCalendarToken> tokenOpt = tokenRepository.findByUsuarioId(usuarioId);
        if (tokenOpt.isEmpty() || !Boolean.TRUE.equals(tokenOpt.get().getSincronizacionActiva())) {
            return Collections.emptyList();
        }

        final GoogleCalendarToken token = tokenOpt.get();
        final Calendar calendar = getCalendarClient(token);

        // Construir FreeBusy request
        FreeBusyRequest fbRequest = new FreeBusyRequest();

        FreeBusyRequestItem item = new FreeBusyRequestItem();
        item.setId(CALENDAR_PRIMARY);
        fbRequest.setTimeMin(
                new DateTime(Date.from(desde.atZone(ZoneId.of(ZONA_HORARIA)).toInstant())));
        fbRequest.setTimeMax(
                new DateTime(Date.from(hasta.atZone(ZoneId.of(ZONA_HORARIA)).toInstant())));
        fbRequest.setItems(List.of(item));

        FreeBusyResponse fbResponse = calendar.freebusy().query(fbRequest).execute();

        final List<TimePeriod> busy = fbResponse.getCalendars().get(CALENDAR_PRIMARY).getBusy();

        return busy.stream()
                .map(
                        tp -> {
                            Instant s = Instant.ofEpochMilli(tp.getStart().getValue());
                            Instant e = Instant.ofEpochMilli(tp.getEnd().getValue());
                            LocalDateTime lds = LocalDateTime.ofInstant(s, ZoneId.of(ZONA_HORARIA));
                            LocalDateTime lde = LocalDateTime.ofInstant(e, ZoneId.of(ZONA_HORARIA));
                            return new AvailabilitySlot(lds, lde, false);
                        })
                .collect(Collectors.toList());
    }

    // ===============================
    // SINCRONIZACIÓN DE CLASES RESERVADAS
    // ===============================

    /**
     * Sincroniza una clase reservada (solicitud pagada) en Google Calendar para un usuario
     * concreto. Si el usuario no tiene GCal conectado o ya lo tiene sincronizado, no hace nada.
     *
     * @param solicitud Solicitud de contratación directa pagada.
     * @param usuario Usuario (alumno o tutor) para quien crear el evento.
     */
    @Transactional
    public void sincronizarBookingParaUsuario(
            final SolicitudContratacionDirecta solicitud, final Usuario usuario) {

        final Optional<GoogleCalendarToken> tokenOpt =
                tokenRepository.findByUsuarioId(usuario.getId());

        if (tokenOpt.isEmpty() || !Boolean.TRUE.equals(tokenOpt.get().getSincronizacionActiva())) {
            return;
        }

        if (calendarBookingRepository
                .findBySolicitudIdAndUsuarioId(solicitud.getId(), usuario.getId())
                .isPresent()) {
            return;
        }

        try {
            crearBookingParaUsuario(solicitud, usuario);
        } catch (Exception e) {
            log.error(
                    "Error sincronizando booking {} en GCal para usuario {}: {}",
                    solicitud.getId(),
                    usuario.getId(),
                    e.getMessage());
        }
    }

    /**
     * Elimina los eventos de Google Calendar de una clase cancelada para todos los usuarios
     * sincronizados.
     *
     * @param solicitud Solicitud cancelada.
     */
    @Transactional
    public void desincronizarBooking(final SolicitudContratacionDirecta solicitud) {
        final List<GoogleCalendarBooking> mapeos =
                calendarBookingRepository.findBySolicitudId(solicitud.getId());

        mapeos.forEach(
                mapeo -> {
                    try {
                        eliminarBookingParaUsuario(mapeo);
                    } catch (Exception e) {
                        log.error(
                                "Error eliminando booking gcal para usuario {}: {}",
                                mapeo.getUsuario().getId(),
                                e.getMessage());
                    }
                });
    }

    private void crearBookingParaUsuario(
            final SolicitudContratacionDirecta solicitud, final Usuario usuario) throws Exception {

        final GoogleCalendarToken token =
                tokenRepository
                        .findByUsuarioId(usuario.getId())
                        .orElseThrow(() -> new RuntimeException("Token no encontrado"));

        final Calendar calendar = getCalendarClient(token);
        final Event gcalEvent = buildBookingGcalEvent(solicitud, usuario);
        final Event creado = calendar.events().insert(CALENDAR_PRIMARY, gcalEvent).execute();

        final GoogleCalendarBooking mapeo = new GoogleCalendarBooking();
        mapeo.setGoogleEventId(creado.getId());
        mapeo.setSolicitud(solicitud);
        mapeo.setUsuario(usuario);
        calendarBookingRepository.save(mapeo);

        log.info(
                "Booking {} creado en GCal para usuario {} → gcalId={}",
                solicitud.getId(),
                usuario.getId(),
                creado.getId());
    }

    private void eliminarBookingParaUsuario(final GoogleCalendarBooking mapeo) throws Exception {
        final GoogleCalendarToken token =
                tokenRepository
                        .findByUsuarioId(mapeo.getUsuario().getId())
                        .orElseThrow(() -> new RuntimeException("Token no encontrado"));

        final Calendar calendar = getCalendarClient(token);

        try {
            calendar.events().delete(CALENDAR_PRIMARY, mapeo.getGoogleEventId()).execute();
        } catch (Exception e) {
            log.warn(
                    "Booking gcalId={} no encontrado al eliminar: {}",
                    mapeo.getGoogleEventId(),
                    e.getMessage());
        }

        calendarBookingRepository.delete(mapeo);
        log.info("Booking gcalId={} eliminado de GCal", mapeo.getGoogleEventId());
    }

    /** Construye el evento de Google Calendar para una clase reservada. */
    private Event buildBookingGcalEvent(
            final SolicitudContratacionDirecta solicitud, final Usuario usuario) {

        final boolean esAlumno = solicitud.getAlumno().getId().equals(usuario.getId());
        final String otraParte =
                esAlumno
                        ? solicitud.getTutor().getUsuario().getNombre()
                        : solicitud.getAlumno().getNombre();
        final String rol = esAlumno ? "con tu profesor" : "con tu alumno";

        final String titulo = String.format("📚 Clase particular %s %s", rol, otraParte);

        final StringBuilder desc = new StringBuilder();
        desc.append("Clase particular reservada en MeerKatters\n\n");
        desc.append("👤 ")
                .append(esAlumno ? "Profesor" : "Alumno")
                .append(": ")
                .append(otraParte)
                .append("\n");
        desc.append("📍 Modalidad: ").append(solicitud.getModalidad()).append("\n");
        desc.append("💰 Importe: ").append(solicitud.getImporteTotal()).append("€\n");
        if (solicitud.getMensaje() != null && !solicitud.getMensaje().isBlank()) {
            desc.append("📝 Nota: ").append(solicitud.getMensaje()).append("\n");
        }

        final Event gcalEvent =
                new Event().setSummary(titulo).setDescription(desc.toString().trim());

        // Fechas: usar dia + horaInicio / horaFin
        final ZoneId zona = ZoneId.of(ZONA_HORARIA);
        final java.time.LocalDateTime inicio =
                java.time.LocalDateTime.of(solicitud.getDia(), solicitud.getHoraInicio());
        final java.time.LocalDateTime fin =
                java.time.LocalDateTime.of(solicitud.getDia(), solicitud.getHoraFin());

        gcalEvent.setStart(toEventDateTime(inicio, zona));
        gcalEvent.setEnd(toEventDateTime(fin, zona));

        // Recordatorios: 24h, 1h y 15 min antes
        final EventReminder reminder24h =
                new EventReminder().setMethod("email").setMinutes(24 * 60);
        final EventReminder reminder1h = new EventReminder().setMethod("popup").setMinutes(60);
        final EventReminder reminder15min = new EventReminder().setMethod("popup").setMinutes(15);

        gcalEvent.setReminders(
                new Reminders()
                        .setUseDefault(false)
                        .setOverrides(List.of(reminder24h, reminder1h, reminder15min)));

        return gcalEvent;
    }
}
