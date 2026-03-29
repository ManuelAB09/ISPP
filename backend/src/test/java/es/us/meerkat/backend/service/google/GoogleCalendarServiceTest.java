package es.us.meerkat.backend.service.google;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.FreeBusyCalendar;
import com.google.api.services.calendar.model.FreeBusyResponse;
import com.google.api.services.calendar.model.TimePeriod;

import es.us.meerkat.backend.config.GoogleCalendarConfig;
import es.us.meerkat.backend.dto.google.UpdateCalendarPreferenciasRequest;
import es.us.meerkat.backend.entity.communities.Comunidad;
import es.us.meerkat.backend.entity.events.Evento;
import es.us.meerkat.backend.entity.events.TipoEvento;
import es.us.meerkat.backend.entity.google.GoogleCalendarBooking;
import es.us.meerkat.backend.entity.google.GoogleCalendarEvento;
import es.us.meerkat.backend.entity.google.GoogleCalendarToken;
import es.us.meerkat.backend.entity.maps.Ubicacion;
import es.us.meerkat.backend.entity.tutors.SolicitudContratacionDirecta;
import es.us.meerkat.backend.entity.tutors.Tutor;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.google.GoogleCalendarBookingRepository;
import es.us.meerkat.backend.repository.google.GoogleCalendarEventoRepository;
import es.us.meerkat.backend.repository.google.GoogleCalendarTokenRepository;
import es.us.meerkat.backend.repository.users.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class GoogleCalendarServiceTest {

    @Mock private GoogleCalendarConfig calendarConfig;
    @Mock private GoogleCalendarTokenRepository tokenRepository;
    @Mock private GoogleCalendarEventoRepository calendarEventoRepository;
    @Mock private GoogleCalendarBookingRepository calendarBookingRepository;
    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks private GoogleCalendarService service;

    private Usuario usuario;
    private GoogleCalendarToken token;
    private Comunidad comunidad;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "appUrl", "http://localhost:3000");

        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNombre("Test User");

        comunidad = new Comunidad();
        comunidad.setId(10L);
        comunidad.setNombre("Test Community");

        token = new GoogleCalendarToken();
        token.setId(1L);
        token.setUsuario(usuario);
        token.setAccessToken("access-token");
        token.setRefreshToken("refresh-token");
        token.setExpiresAt(LocalDateTime.now().plusHours(1));
        token.setSincronizacionActiva(true);
    }

    // ============= desconectar =============

    @Test
    void desconectar_shouldDeleteAllRecordsForUser() {
        service.desconectar(1L);
        verify(calendarEventoRepository).deleteByUsuarioId(1L);
        verify(calendarBookingRepository).deleteByUsuarioId(1L);
        verify(tokenRepository).deleteByUsuarioId(1L);
    }

    // ============= obtenerEstado =============

    @Test
    void obtenerEstado_noToken_shouldReturnDisconnected() {
        when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.empty());
        var result = service.obtenerEstado(1L);
        assertThat(result.getConectado()).isFalse();
        assertThat(result.getSincronizacionActiva()).isFalse();
    }

    @Test
    void obtenerEstado_withToken_shouldReturnConnected() {
        when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));
        var result = service.obtenerEstado(1L);
        assertThat(result.getConectado()).isTrue();
        assertThat(result.getSincronizacionActiva()).isTrue();
    }

    @Test
    void obtenerEstado_withTipos_shouldParseTipos() {
        token.setTiposEventoSincronizados("REUNION,EXAMEN");
        when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));
        var result = service.obtenerEstado(1L);
        assertThat(result.getTiposSincronizados()).contains(TipoEvento.REUNION, TipoEvento.EXAMEN);
    }

    @Test
    void obtenerEstado_nullTipos_shouldReturnEmptyList() {
        token.setTiposEventoSincronizados(null);
        when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));
        var result = service.obtenerEstado(1L);
        assertThat(result.getTiposSincronizados()).isEmpty();
    }

    @Test
    void obtenerEstado_blankTipos_shouldReturnEmptyList() {
        token.setTiposEventoSincronizados("  ");
        when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));
        var result = service.obtenerEstado(1L);
        assertThat(result.getTiposSincronizados()).isEmpty();
    }

    // ============= actualizarPreferencias =============

    @Test
    void actualizarPreferencias_updateSyncActive() {
        when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));
        UpdateCalendarPreferenciasRequest req = new UpdateCalendarPreferenciasRequest();
        req.setSincronizacionActiva(false);
        service.actualizarPreferencias(1L, req);
        verify(tokenRepository).save(argThat(t -> !t.getSincronizacionActiva()));
    }

    @Test
    void actualizarPreferencias_updateTipos() {
        when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));
        UpdateCalendarPreferenciasRequest req = new UpdateCalendarPreferenciasRequest();
        req.setTiposSincronizados(List.of(TipoEvento.REUNION, TipoEvento.CLASE));
        service.actualizarPreferencias(1L, req);
        verify(tokenRepository)
                .save(
                        argThat(
                                t ->
                                        t.getTiposEventoSincronizados() != null
                                                && t.getTiposEventoSincronizados()
                                                        .contains("REUNION")));
    }

    @Test
    void actualizarPreferencias_emptyTipos_shouldSetNull() {
        when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));
        UpdateCalendarPreferenciasRequest req = new UpdateCalendarPreferenciasRequest();
        req.setTiposSincronizados(List.of());
        service.actualizarPreferencias(1L, req);
        verify(tokenRepository).save(argThat(t -> t.getTiposEventoSincronizados() == null));
    }

    @Test
    void actualizarPreferencias_noToken_shouldThrow() {
        when(tokenRepository.findByUsuarioId(99L)).thenReturn(Optional.empty());
        UpdateCalendarPreferenciasRequest req = new UpdateCalendarPreferenciasRequest();
        assertThatThrownBy(() -> service.actualizarPreferencias(99L, req))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void actualizarPreferencias_nullFields_shouldNotModify() {
        token.setSincronizacionActiva(true);
        token.setTiposEventoSincronizados("EXAMEN");
        when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));
        UpdateCalendarPreferenciasRequest req = new UpdateCalendarPreferenciasRequest();
        // both fields null → no changes
        service.actualizarPreferencias(1L, req);
        verify(tokenRepository)
                .save(
                        argThat(
                                t ->
                                        t.getSincronizacionActiva()
                                                && "EXAMEN"
                                                        .equals(t.getTiposEventoSincronizados())));
    }

    // ============= sincronizarCreacion =============

    @Test
    void sincronizarCreacion_noComunidad_shouldDoNothing() {
        Evento evento = new Evento();
        evento.setId(1L);
        service.sincronizarCreacion(evento);
        verify(calendarEventoRepository, never()).save(any());
    }

    @Test
    void sincronizarCreacion_noMembersWithCalendar_shouldDoNothing() {
        Evento evento = createTestEvento();
        when(usuarioRepository.findMiembrosConCalendarActivoByComunidadId(10L))
                .thenReturn(Collections.emptyList());
        service.sincronizarCreacion(evento);
        verify(calendarEventoRepository, never()).save(any());
    }

    @Test
    void sincronizarCreacion_withMembers_shouldCreateEvents() throws Exception {
        Evento evento = createTestEvento();
        when(usuarioRepository.findMiembrosConCalendarActivoByComunidadId(10L))
                .thenReturn(List.of(usuario));
        when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));

        Calendar mockCalendar = mock(Calendar.class);
        Calendar.Events mockEvents = mock(Calendar.Events.class);
        Calendar.Events.Insert mockInsert = mock(Calendar.Events.Insert.class);

        when(calendarConfig.buildCalendarClient(anyString(), anyString())).thenReturn(mockCalendar);
        when(mockCalendar.events()).thenReturn(mockEvents);
        when(mockEvents.insert(eq("primary"), any(Event.class))).thenReturn(mockInsert);
        when(mockInsert.execute()).thenReturn(new Event().setId("gcal-123"));

        service.sincronizarCreacion(evento);
        verify(calendarEventoRepository)
                .save(argThat(m -> "gcal-123".equals(m.getGoogleEventId())));
    }

    @Test
    void sincronizarCreacion_memberThrowsException_shouldNotPropagateError() {
        Evento evento = createTestEvento();
        when(usuarioRepository.findMiembrosConCalendarActivoByComunidadId(10L))
                .thenReturn(List.of(usuario));
        when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.empty());

        // Should not throw — error is caught in the forEach
        assertThatCode(() -> service.sincronizarCreacion(evento)).doesNotThrowAnyException();
    }

    // ============= sincronizarActualizacion =============

    @Test
    void sincronizarActualizacion_noMapeos_shouldDoNothing() {
        Evento evento = new Evento();
        evento.setId(1L);
        when(calendarEventoRepository.findByEventoId(1L)).thenReturn(List.of());
        service.sincronizarActualizacion(evento);
        verify(calendarEventoRepository, never()).save(any());
    }

    @Test
    void sincronizarActualizacion_withMapeos_shouldUpdateEvents() throws Exception {
        Evento evento = createTestEvento();
        GoogleCalendarEvento mapeo = new GoogleCalendarEvento();
        mapeo.setGoogleEventId("gcal-existing");
        mapeo.setEvento(evento);
        mapeo.setUsuario(usuario);
        when(calendarEventoRepository.findByEventoId(1L)).thenReturn(List.of(mapeo));
        when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));

        Calendar mockCalendar = mock(Calendar.class);
        Calendar.Events mockEvents = mock(Calendar.Events.class);
        Calendar.Events.Update mockUpdate = mock(Calendar.Events.Update.class);

        when(calendarConfig.buildCalendarClient(anyString(), anyString())).thenReturn(mockCalendar);
        when(mockCalendar.events()).thenReturn(mockEvents);
        when(mockEvents.update(eq("primary"), eq("gcal-existing"), any(Event.class)))
                .thenReturn(mockUpdate);
        when(mockUpdate.execute()).thenReturn(new Event().setId("gcal-existing"));

        service.sincronizarActualizacion(evento);
        verify(calendarEventoRepository).save(mapeo);
    }

    // ============= sincronizarCancelacion =============

    @Test
    void sincronizarCancelacion_noMapeos_shouldDoNothing() {
        Evento evento = new Evento();
        evento.setId(1L);
        when(calendarEventoRepository.findByEventoId(1L)).thenReturn(List.of());
        service.sincronizarCancelacion(evento);
        verify(calendarEventoRepository, never()).delete(any(GoogleCalendarEvento.class));
    }

    @Test
    void sincronizarCancelacion_withMapeos_shouldDeleteEvents() throws Exception {
        Evento evento = new Evento();
        evento.setId(1L);
        GoogleCalendarEvento mapeo = new GoogleCalendarEvento();
        mapeo.setGoogleEventId("gcal-to-delete");
        mapeo.setUsuario(usuario);
        when(calendarEventoRepository.findByEventoId(1L)).thenReturn(List.of(mapeo));
        when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));

        Calendar mockCalendar = mock(Calendar.class);
        Calendar.Events mockEvents = mock(Calendar.Events.class);
        Calendar.Events.Delete mockDelete = mock(Calendar.Events.Delete.class);

        when(calendarConfig.buildCalendarClient(anyString(), anyString())).thenReturn(mockCalendar);
        when(mockCalendar.events()).thenReturn(mockEvents);
        when(mockEvents.delete(eq("primary"), eq("gcal-to-delete"))).thenReturn(mockDelete);

        service.sincronizarCancelacion(evento);
        verify(calendarEventoRepository).delete(mapeo);
    }

    // ============= sincronizarParaUsuario =============

    @Test
    void sincronizarParaUsuario_noToken_shouldReturn() {
        Evento evento = new Evento();
        evento.setId(1L);
        when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.empty());
        service.sincronizarParaUsuario(evento, usuario);
        verify(calendarEventoRepository, never()).save(any());
    }

    @Test
    void sincronizarParaUsuario_syncInactive_shouldReturn() {
        token.setSincronizacionActiva(false);
        Evento evento = new Evento();
        evento.setId(1L);
        when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));
        service.sincronizarParaUsuario(evento, usuario);
        verify(calendarEventoRepository, never()).save(any());
    }

    @Test
    void sincronizarParaUsuario_alreadySynced_shouldReturn() {
        Evento evento = new Evento();
        evento.setId(1L);
        when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));
        when(calendarEventoRepository.findByEventoIdAndUsuarioId(1L, 1L))
                .thenReturn(Optional.of(new GoogleCalendarEvento()));
        service.sincronizarParaUsuario(evento, usuario);
        verify(calendarEventoRepository, never()).save(any());
    }

    @Test
    void sincronizarParaUsuario_success_shouldCreateEvent() throws Exception {
        Evento evento = createTestEvento();
        when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));
        when(calendarEventoRepository.findByEventoIdAndUsuarioId(1L, 1L))
                .thenReturn(Optional.empty());

        Calendar mockCalendar = mock(Calendar.class);
        Calendar.Events mockEvents = mock(Calendar.Events.class);
        Calendar.Events.Insert mockInsert = mock(Calendar.Events.Insert.class);

        when(calendarConfig.buildCalendarClient(anyString(), anyString())).thenReturn(mockCalendar);
        when(mockCalendar.events()).thenReturn(mockEvents);
        when(mockEvents.insert(eq("primary"), any(Event.class))).thenReturn(mockInsert);
        when(mockInsert.execute()).thenReturn(new Event().setId("gcal-new"));

        service.sincronizarParaUsuario(evento, usuario);
        verify(calendarEventoRepository)
                .save(argThat(m -> "gcal-new".equals(m.getGoogleEventId())));
    }

    @Test
    void sincronizarParaUsuario_typNotSynced_shouldNotCreate() throws Exception {
        token.setTiposEventoSincronizados("EXAMEN");
        Evento evento = createTestEvento();
        evento.setTipoEvento(TipoEvento.REUNION);

        when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));
        when(calendarEventoRepository.findByEventoIdAndUsuarioId(1L, 1L))
                .thenReturn(Optional.empty());

        service.sincronizarParaUsuario(evento, usuario);
        verify(calendarEventoRepository, never()).save(any());
    }

    // ============= desincronizarParaUsuario =============

    @Test
    void desincronizarParaUsuario_noMapeo_shouldReturn() {
        Evento evento = new Evento();
        evento.setId(1L);
        when(calendarEventoRepository.findByEventoIdAndUsuarioId(1L, 1L))
                .thenReturn(Optional.empty());
        service.desincronizarParaUsuario(evento, usuario);
        verify(calendarEventoRepository, never()).delete(any(GoogleCalendarEvento.class));
    }

    @Test
    void desincronizarParaUsuario_withMapeo_shouldDeleteEvent() throws Exception {
        Evento evento = new Evento();
        evento.setId(1L);
        GoogleCalendarEvento mapeo = new GoogleCalendarEvento();
        mapeo.setGoogleEventId("gcal-del");
        mapeo.setUsuario(usuario);
        when(calendarEventoRepository.findByEventoIdAndUsuarioId(1L, 1L))
                .thenReturn(Optional.of(mapeo));
        when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));

        Calendar mockCalendar = mock(Calendar.class);
        Calendar.Events mockEvents = mock(Calendar.Events.class);
        Calendar.Events.Delete mockDelete = mock(Calendar.Events.Delete.class);

        when(calendarConfig.buildCalendarClient(anyString(), anyString())).thenReturn(mockCalendar);
        when(mockCalendar.events()).thenReturn(mockEvents);
        when(mockEvents.delete(eq("primary"), eq("gcal-del"))).thenReturn(mockDelete);

        service.desincronizarParaUsuario(evento, usuario);
        verify(calendarEventoRepository).delete(mapeo);
    }

    // ============= obtenerBusyIntervals =============

    @Test
    void obtenerBusyIntervals_noToken_shouldReturnEmpty() throws Exception {
        when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.empty());
        var result =
                service.obtenerBusyIntervals(
                        1L, LocalDateTime.now(), LocalDateTime.now().plusDays(1));
        assertThat(result).isEmpty();
    }

    @Test
    void obtenerBusyIntervals_syncInactive_shouldReturnEmpty() throws Exception {
        token.setSincronizacionActiva(false);
        when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));
        var result =
                service.obtenerBusyIntervals(
                        1L, LocalDateTime.now(), LocalDateTime.now().plusDays(1));
        assertThat(result).isEmpty();
    }

    @Test
    void obtenerBusyIntervals_withBusyPeriods_shouldReturnSlots() throws Exception {
        when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));

        Calendar mockCalendar = mock(Calendar.class);
        Calendar.Freebusy mockFreebusy = mock(Calendar.Freebusy.class);
        Calendar.Freebusy.Query mockQuery = mock(Calendar.Freebusy.Query.class);

        when(calendarConfig.buildCalendarClient(anyString(), anyString())).thenReturn(mockCalendar);
        when(mockCalendar.freebusy()).thenReturn(mockFreebusy);
        when(mockFreebusy.query(any())).thenReturn(mockQuery);

        long now = System.currentTimeMillis();
        TimePeriod busy = new TimePeriod();
        busy.setStart(new com.google.api.client.util.DateTime(now));
        busy.setEnd(new com.google.api.client.util.DateTime(now + 3600000));

        FreeBusyCalendar fbCal = new FreeBusyCalendar();
        fbCal.setBusy(List.of(busy));

        FreeBusyResponse fbResponse = new FreeBusyResponse();
        fbResponse.setCalendars(java.util.Map.of("primary", fbCal));

        when(mockQuery.execute()).thenReturn(fbResponse);

        var result =
                service.obtenerBusyIntervals(
                        1L, LocalDateTime.now().minusHours(1), LocalDateTime.now().plusDays(1));
        assertThat(result).hasSize(1);
        assertThat(result.get(0).isAvailable()).isFalse();
    }

    // ============= sincronizarBookingParaUsuario =============

    @Test
    void sincronizarBookingParaUsuario_noToken_shouldReturn() {
        SolicitudContratacionDirecta solicitud =
                SolicitudContratacionDirecta.builder().id(1L).build();
        when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.empty());
        service.sincronizarBookingParaUsuario(solicitud, usuario);
        verify(calendarBookingRepository, never()).save(any());
    }

    @Test
    void sincronizarBookingParaUsuario_syncInactive_shouldReturn() {
        token.setSincronizacionActiva(false);
        SolicitudContratacionDirecta solicitud =
                SolicitudContratacionDirecta.builder().id(1L).build();
        when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));
        service.sincronizarBookingParaUsuario(solicitud, usuario);
        verify(calendarBookingRepository, never()).save(any());
    }

    @Test
    void sincronizarBookingParaUsuario_alreadySynced_shouldReturn() {
        SolicitudContratacionDirecta solicitud =
                SolicitudContratacionDirecta.builder().id(1L).build();
        when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));
        when(calendarBookingRepository.findBySolicitudIdAndUsuarioId(1L, 1L))
                .thenReturn(Optional.of(new GoogleCalendarBooking()));
        service.sincronizarBookingParaUsuario(solicitud, usuario);
        verify(calendarBookingRepository, never()).save(any());
    }

    @Test
    void sincronizarBookingParaUsuario_success_shouldCreateBooking() throws Exception {
        Tutor tutor = new Tutor();
        Usuario tutorUser = new Usuario();
        tutorUser.setId(2L);
        tutorUser.setNombre("Tutor Name");
        tutor.setUsuario(tutorUser);

        SolicitudContratacionDirecta solicitud =
                SolicitudContratacionDirecta.builder()
                        .id(1L)
                        .alumno(usuario)
                        .tutor(tutor)
                        .dia(java.time.LocalDate.now().plusDays(1))
                        .horaInicio(java.time.LocalTime.of(10, 0))
                        .horaFin(java.time.LocalTime.of(11, 0))
                        .modalidad("ONLINE")
                        .importeTotal(java.math.BigDecimal.valueOf(25))
                        .build();

        when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));
        when(calendarBookingRepository.findBySolicitudIdAndUsuarioId(1L, 1L))
                .thenReturn(Optional.empty());

        Calendar mockCalendar = mock(Calendar.class);
        Calendar.Events mockEvents = mock(Calendar.Events.class);
        Calendar.Events.Insert mockInsert = mock(Calendar.Events.Insert.class);

        when(calendarConfig.buildCalendarClient(anyString(), anyString())).thenReturn(mockCalendar);
        when(mockCalendar.events()).thenReturn(mockEvents);
        when(mockEvents.insert(eq("primary"), any(Event.class))).thenReturn(mockInsert);
        when(mockInsert.execute()).thenReturn(new Event().setId("gcal-booking-1"));

        service.sincronizarBookingParaUsuario(solicitud, usuario);
        verify(calendarBookingRepository)
                .save(argThat(b -> "gcal-booking-1".equals(b.getGoogleEventId())));
    }

    @Test
    void sincronizarBookingParaUsuario_withMessage_shouldNotThrow() throws Exception {
        Tutor tutor = new Tutor();
        Usuario tutorUser = new Usuario();
        tutorUser.setId(2L);
        tutorUser.setNombre("Tutor Name");
        tutor.setUsuario(tutorUser);

        SolicitudContratacionDirecta solicitud =
                SolicitudContratacionDirecta.builder()
                        .id(2L)
                        .alumno(usuario)
                        .tutor(tutor)
                        .dia(java.time.LocalDate.now().plusDays(1))
                        .horaInicio(java.time.LocalTime.of(14, 0))
                        .horaFin(java.time.LocalTime.of(15, 0))
                        .modalidad("PRESENCIAL")
                        .importeTotal(java.math.BigDecimal.valueOf(30))
                        .mensaje("Please bring notes")
                        .build();

        when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));
        when(calendarBookingRepository.findBySolicitudIdAndUsuarioId(2L, 1L))
                .thenReturn(Optional.empty());

        Calendar mockCalendar = mock(Calendar.class);
        Calendar.Events mockEvents = mock(Calendar.Events.class);
        Calendar.Events.Insert mockInsert = mock(Calendar.Events.Insert.class);

        when(calendarConfig.buildCalendarClient(anyString(), anyString())).thenReturn(mockCalendar);
        when(mockCalendar.events()).thenReturn(mockEvents);
        when(mockEvents.insert(eq("primary"), any(Event.class))).thenReturn(mockInsert);
        when(mockInsert.execute()).thenReturn(new Event().setId("gcal-booking-2"));

        service.sincronizarBookingParaUsuario(solicitud, usuario);
        verify(calendarBookingRepository).save(any());
    }

    // ============= desincronizarBooking =============

    @Test
    void desincronizarBooking_noMapeos_shouldDoNothing() {
        SolicitudContratacionDirecta solicitud =
                SolicitudContratacionDirecta.builder().id(1L).build();
        when(calendarBookingRepository.findBySolicitudId(1L)).thenReturn(List.of());
        service.desincronizarBooking(solicitud);
        verify(calendarBookingRepository, never()).delete(any(GoogleCalendarBooking.class));
    }

    @Test
    void desincronizarBooking_withMapeos_shouldDeleteBookings() throws Exception {
        SolicitudContratacionDirecta solicitud =
                SolicitudContratacionDirecta.builder().id(1L).build();
        GoogleCalendarBooking mapeo = new GoogleCalendarBooking();
        mapeo.setGoogleEventId("gcal-booking-del");
        mapeo.setUsuario(usuario);
        when(calendarBookingRepository.findBySolicitudId(1L)).thenReturn(List.of(mapeo));
        when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));

        Calendar mockCalendar = mock(Calendar.class);
        Calendar.Events mockEvents = mock(Calendar.Events.class);
        Calendar.Events.Delete mockDelete = mock(Calendar.Events.Delete.class);

        when(calendarConfig.buildCalendarClient(anyString(), anyString())).thenReturn(mockCalendar);
        when(mockCalendar.events()).thenReturn(mockEvents);
        when(mockEvents.delete(eq("primary"), eq("gcal-booking-del"))).thenReturn(mockDelete);

        service.desincronizarBooking(solicitud);
        verify(calendarBookingRepository).delete(mapeo);
    }

    // ============= sincronizarCreacion: buildGcalEvent branches =============

    @Test
    void sincronizarCreacion_virtualEventWithLink_shouldSetLocation() throws Exception {
        Evento evento = createTestEvento();
        evento.setEsVirtual(true);
        evento.setEnlaceVirtual("https://zoom.us/meeting");
        evento.setTipoEvento(TipoEvento.REUNION);

        when(usuarioRepository.findMiembrosConCalendarActivoByComunidadId(10L))
                .thenReturn(List.of(usuario));
        when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));

        Calendar mockCalendar = mock(Calendar.class);
        Calendar.Events mockEvents = mock(Calendar.Events.class);
        Calendar.Events.Insert mockInsert = mock(Calendar.Events.Insert.class);

        when(calendarConfig.buildCalendarClient(anyString(), anyString())).thenReturn(mockCalendar);
        when(mockCalendar.events()).thenReturn(mockEvents);
        when(mockEvents.insert(eq("primary"), any(Event.class))).thenReturn(mockInsert);
        when(mockInsert.execute()).thenReturn(new Event().setId("gcal-v"));

        service.sincronizarCreacion(evento);
        verify(calendarEventoRepository).save(any());
    }

    @Test
    void sincronizarCreacion_physicalEventWithUbicacion_shouldSetLocation() throws Exception {
        Evento evento = createTestEvento();
        evento.setEsVirtual(false);
        Ubicacion ubi =
                Ubicacion.builder()
                        .nombre("Biblioteca")
                        .direccion("Calle 123")
                        .latitud(37.0)
                        .longitud(-5.0)
                        .build();
        evento.setUbicacion(ubi);

        when(usuarioRepository.findMiembrosConCalendarActivoByComunidadId(10L))
                .thenReturn(List.of(usuario));
        when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));

        Calendar mockCalendar = mock(Calendar.class);
        Calendar.Events mockEvents = mock(Calendar.Events.class);
        Calendar.Events.Insert mockInsert = mock(Calendar.Events.Insert.class);

        when(calendarConfig.buildCalendarClient(anyString(), anyString())).thenReturn(mockCalendar);
        when(mockCalendar.events()).thenReturn(mockEvents);
        when(mockEvents.insert(eq("primary"), any(Event.class))).thenReturn(mockInsert);
        when(mockInsert.execute()).thenReturn(new Event().setId("gcal-phy"));

        service.sincronizarCreacion(evento);
        verify(calendarEventoRepository).save(any());
    }

    @Test
    void sincronizarCreacion_eventWithQueLlevar_shouldIncludeInDescription() throws Exception {
        Evento evento = createTestEvento();
        evento.setQueLlevar("Laptop and notebook");

        when(usuarioRepository.findMiembrosConCalendarActivoByComunidadId(10L))
                .thenReturn(List.of(usuario));
        when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));

        Calendar mockCalendar = mock(Calendar.class);
        Calendar.Events mockEvents = mock(Calendar.Events.class);
        Calendar.Events.Insert mockInsert = mock(Calendar.Events.Insert.class);

        when(calendarConfig.buildCalendarClient(anyString(), anyString())).thenReturn(mockCalendar);
        when(mockCalendar.events()).thenReturn(mockEvents);
        when(mockEvents.insert(eq("primary"), any(Event.class))).thenReturn(mockInsert);
        when(mockInsert.execute()).thenReturn(new Event().setId("gcal-q"));

        service.sincronizarCreacion(evento);
        verify(calendarEventoRepository).save(any());
    }

    @Test
    void sincronizarCreacion_eventNullTipo_shouldDefaultToOtro() throws Exception {
        Evento evento = createTestEvento();
        evento.setTipoEvento(null);

        when(usuarioRepository.findMiembrosConCalendarActivoByComunidadId(10L))
                .thenReturn(List.of(usuario));
        when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));

        Calendar mockCalendar = mock(Calendar.class);
        Calendar.Events mockEvents = mock(Calendar.Events.class);
        Calendar.Events.Insert mockInsert = mock(Calendar.Events.Insert.class);

        when(calendarConfig.buildCalendarClient(anyString(), anyString())).thenReturn(mockCalendar);
        when(mockCalendar.events()).thenReturn(mockEvents);
        when(mockEvents.insert(eq("primary"), any(Event.class))).thenReturn(mockInsert);
        when(mockInsert.execute()).thenReturn(new Event().setId("gcal-otro"));

        service.sincronizarCreacion(evento);
        verify(calendarEventoRepository).save(any());
    }

    @Test
    void sincronizarCreacion_eventNoFechaFin_shouldUseOneHourAfter() throws Exception {
        Evento evento = createTestEvento();
        evento.setFechaFin(null); // no end date

        when(usuarioRepository.findMiembrosConCalendarActivoByComunidadId(10L))
                .thenReturn(List.of(usuario));
        when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));

        Calendar mockCalendar = mock(Calendar.class);
        Calendar.Events mockEvents = mock(Calendar.Events.class);
        Calendar.Events.Insert mockInsert = mock(Calendar.Events.Insert.class);

        when(calendarConfig.buildCalendarClient(anyString(), anyString())).thenReturn(mockCalendar);
        when(mockCalendar.events()).thenReturn(mockEvents);
        when(mockEvents.insert(eq("primary"), any(Event.class))).thenReturn(mockInsert);
        when(mockInsert.execute()).thenReturn(new Event().setId("gcal-nofin"));

        service.sincronizarCreacion(evento);
        verify(calendarEventoRepository).save(any());
    }

    @Test
    void sincronizarCreacion_ubicacionNoDireccion_shouldUseNameOnly() throws Exception {
        Evento evento = createTestEvento();
        evento.setEsVirtual(false);
        Ubicacion ubi =
                Ubicacion.builder()
                        .nombre("Plaza Central")
                        .direccion(null)
                        .latitud(37.0)
                        .longitud(-5.0)
                        .build();
        evento.setUbicacion(ubi);

        when(usuarioRepository.findMiembrosConCalendarActivoByComunidadId(10L))
                .thenReturn(List.of(usuario));
        when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));

        Calendar mockCalendar = mock(Calendar.class);
        Calendar.Events mockEvents = mock(Calendar.Events.class);
        Calendar.Events.Insert mockInsert = mock(Calendar.Events.Insert.class);

        when(calendarConfig.buildCalendarClient(anyString(), anyString())).thenReturn(mockCalendar);
        when(mockCalendar.events()).thenReturn(mockEvents);
        when(mockEvents.insert(eq("primary"), any(Event.class))).thenReturn(mockInsert);
        when(mockInsert.execute()).thenReturn(new Event().setId("gcal-nodir"));

        service.sincronizarCreacion(evento);
        verify(calendarEventoRepository).save(any());
    }

    // ============= helper =============

    private Evento createTestEvento() {
        Evento evento = new Evento();
        evento.setId(1L);
        evento.setTitulo("Test Event");
        evento.setDescripcion("A test event description");
        evento.setTipoEvento(TipoEvento.CLASE);
        evento.setFechaHora(LocalDateTime.now().plusDays(1));
        evento.setFechaFin(LocalDateTime.now().plusDays(1).plusHours(2));
        evento.setComunidad(comunidad);
        return evento;
    }
}
