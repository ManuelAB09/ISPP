package es.us.meerkat.backend.service.tutors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import es.us.meerkat.backend.dto.tutors.DisponibilidadTutorResponse;
import es.us.meerkat.backend.dto.tutors.HorarioOcupadoResponse;
import es.us.meerkat.backend.dto.tutors.SolicitudContratacionRequest;
import es.us.meerkat.backend.dto.tutors.SolicitudContratacionResponse;
import es.us.meerkat.backend.entity.tutors.EstadoSolicitudContratacion;
import es.us.meerkat.backend.entity.tutors.SolicitudContratacionDirecta;
import es.us.meerkat.backend.entity.tutors.Tutor;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.tutors.SolicitudContratacionDirectaRepository;
import es.us.meerkat.backend.repository.tutors.TutorRepository;
import es.us.meerkat.backend.repository.users.UsuarioRepository;
import es.us.meerkat.backend.service.emails.EmailService;
import es.us.meerkat.backend.service.google.GoogleCalendarService;
import es.us.meerkat.backend.service.subscriptions.PaymentService;

@ExtendWith(MockitoExtension.class)
class SolicitudContratacionServiceTest {

    @Mock private SolicitudContratacionDirectaRepository solicitudRepository;
    @Mock private TutorRepository tutorRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private SimpMessagingTemplate broker;
    @Mock private EmailService emailService;
    @Mock private GoogleCalendarService googleCalendarService;
    @Mock private PaymentService paymentService;
    @Mock private DisponibilidadService disponibilidadService;

    @InjectMocks private SolicitudContratacionService service;

    private Usuario buildUsuario(Long id, String email) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setEmail(email);
        return u;
    }

    private Tutor buildTutor(Long id, Usuario usuario) {
        Tutor t = new Tutor();
        t.setId(id);
        t.setUsuario(usuario);
        t.setVerificado(true);
        t.setTarifaHora(BigDecimal.valueOf(20));
        return t;
    }

    private SolicitudContratacionRequest buildRequest() {
        SolicitudContratacionRequest req = new SolicitudContratacionRequest();
        req.setDia(LocalDate.of(2027, 6, 15));
        req.setHoraInicio(LocalTime.of(10, 0));
        req.setHoraFin(LocalTime.of(11, 0));
        req.setModalidad("ONLINE");
        req.setMensaje("Test message");
        return req;
    }

    private DisponibilidadTutorResponse buildDisponibilidad(LocalTime inicio, LocalTime fin) {
        return DisponibilidadTutorResponse.builder()
                .horaInicio(inicio)
                .horaFin(fin)
                .modalidad("ONLINE")
                .build();
    }

    @Test
    void crearSolicitudShouldCreateAndNotifyTutor() {
        Usuario alumno = buildUsuario(1L, "alumno@test.es");
        Usuario tutorUsuario = buildUsuario(2L, "tutor@test.es");
        Tutor tutor = buildTutor(10L, tutorUsuario);
        SolicitudContratacionRequest request = buildRequest();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(alumno));
        when(tutorRepository.findById(10L)).thenReturn(Optional.of(tutor));
        when(solicitudRepository.findConflictingBookings(eq(10L), any(), any(), any()))
                .thenReturn(List.of());
        when(disponibilidadService.getDisponibilidadesPorFecha(eq(10L), any()))
                .thenReturn(List.of(buildDisponibilidad(LocalTime.of(9, 0), LocalTime.of(12, 0))));
        when(solicitudRepository.save(any(SolicitudContratacionDirecta.class)))
                .thenAnswer(
                        inv -> {
                            SolicitudContratacionDirecta s = inv.getArgument(0);
                            s.setId(100L);
                            return s;
                        });

        SolicitudContratacionResponse result = service.crearSolicitud(1L, 10L, request);

        assertThat(result).isNotNull();
        verify(solicitudRepository).save(any(SolicitudContratacionDirecta.class));
        verify(broker).convertAndSendToUser(eq("2"), eq("/queue/solicitud_contratacion"), any());
    }

    @Test
    void crearSolicitudShouldThrowWhenSelfHiring() {
        Usuario alumno = buildUsuario(1L, "alumno@test.es");
        Tutor tutor = buildTutor(10L, alumno);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(alumno));
        when(tutorRepository.findById(10L)).thenReturn(Optional.of(tutor));

        assertThatThrownBy(() -> service.crearSolicitud(1L, 10L, buildRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No puedes contratarte a ti mismo");
    }

    @Test
    void crearSolicitudShouldThrowWhenTutorNotVerified() {
        Usuario alumno = buildUsuario(1L, "alumno@test.es");
        Usuario tutorUsuario = buildUsuario(2L, "tutor@test.es");
        Tutor tutor = buildTutor(10L, tutorUsuario);
        tutor.setVerificado(false);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(alumno));
        when(tutorRepository.findById(10L)).thenReturn(Optional.of(tutor));

        assertThatThrownBy(() -> service.crearSolicitud(1L, 10L, buildRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("verificado");
    }

    @Test
    void crearSolicitudShouldThrowWhenEndTimeBeforeStartTime() {
        Usuario alumno = buildUsuario(1L, "alumno@test.es");
        Usuario tutorUsuario = buildUsuario(2L, "tutor@test.es");
        Tutor tutor = buildTutor(10L, tutorUsuario);

        SolicitudContratacionRequest request = buildRequest();
        request.setHoraInicio(LocalTime.of(11, 0));
        request.setHoraFin(LocalTime.of(10, 0));

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(alumno));
        when(tutorRepository.findById(10L)).thenReturn(Optional.of(tutor));

        assertThatThrownBy(() -> service.crearSolicitud(1L, 10L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("hora de fin");
    }

    @Test
    void crearSolicitudShouldThrowWhenInvalidModality() {
        Usuario alumno = buildUsuario(1L, "alumno@test.es");
        Usuario tutorUsuario = buildUsuario(2L, "tutor@test.es");
        Tutor tutor = buildTutor(10L, tutorUsuario);

        SolicitudContratacionRequest request = buildRequest();
        request.setModalidad("INVALIDA");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(alumno));
        when(tutorRepository.findById(10L)).thenReturn(Optional.of(tutor));

        assertThatThrownBy(() -> service.crearSolicitud(1L, 10L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Modalidad no válida");
    }

    @Test
    void crearSolicitudShouldThrowWhenScheduleConflict() {
        Usuario alumno = buildUsuario(1L, "alumno@test.es");
        Usuario tutorUsuario = buildUsuario(2L, "tutor@test.es");
        Tutor tutor = buildTutor(10L, tutorUsuario);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(alumno));
        when(tutorRepository.findById(10L)).thenReturn(Optional.of(tutor));
        when(solicitudRepository.findConflictingBookings(eq(10L), any(), any(), any()))
                .thenReturn(List.of(new SolicitudContratacionDirecta()));

        assertThatThrownBy(() -> service.crearSolicitud(1L, 10L, buildRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reserva confirmada");
    }

    @Test
    void aceptarSolicitudShouldChangeStatusAndNotifyAlumno() {
        Usuario alumno = buildUsuario(1L, "alumno@test.es");
        Usuario tutorUsuario = buildUsuario(2L, "tutor@test.es");
        Tutor tutor = buildTutor(10L, tutorUsuario);

        SolicitudContratacionDirecta solicitud =
                SolicitudContratacionDirecta.builder()
                        .id(100L)
                        .alumno(alumno)
                        .tutor(tutor)
                        .dia(LocalDate.of(2027, 6, 15))
                        .horaInicio(LocalTime.of(10, 0))
                        .horaFin(LocalTime.of(11, 0))
                        .estado(EstadoSolicitudContratacion.PENDIENTE)
                        .importeTotal(BigDecimal.valueOf(20))
                        .tarifaHora(BigDecimal.valueOf(20))
                        .build();

        when(solicitudRepository.findById(100L)).thenReturn(Optional.of(solicitud));
        when(solicitudRepository.findConflictingBookingsExcluding(
                        eq(10L), any(), any(), any(), eq(100L)))
                .thenReturn(List.of());
        when(solicitudRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SolicitudContratacionResponse result = service.aceptarSolicitud(100L, 2L);

        assertThat(result).isNotNull();
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitudContratacion.ACEPTADA);
        verify(broker)
                .convertAndSendToUser(
                        eq("1"), eq("/queue/solicitud_contratacion_respuesta"), any());
    }

    @Test
    void aceptarSolicitudShouldThrowWhenNotOwner() {
        Usuario tutorUsuario = buildUsuario(2L, "tutor@test.es");
        Tutor tutor = buildTutor(10L, tutorUsuario);

        SolicitudContratacionDirecta solicitud =
                SolicitudContratacionDirecta.builder()
                        .id(100L)
                        .tutor(tutor)
                        .estado(EstadoSolicitudContratacion.PENDIENTE)
                        .build();

        when(solicitudRepository.findById(100L)).thenReturn(Optional.of(solicitud));

        assertThatThrownBy(() -> service.aceptarSolicitud(100L, 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No tienes permiso");
    }

    @Test
    void aceptarSolicitudShouldThrowWhenNotPending() {
        Usuario tutorUsuario = buildUsuario(2L, "tutor@test.es");
        Tutor tutor = buildTutor(10L, tutorUsuario);

        SolicitudContratacionDirecta solicitud =
                SolicitudContratacionDirecta.builder()
                        .id(100L)
                        .tutor(tutor)
                        .estado(EstadoSolicitudContratacion.ACEPTADA)
                        .build();

        when(solicitudRepository.findById(100L)).thenReturn(Optional.of(solicitud));

        assertThatThrownBy(() -> service.aceptarSolicitud(100L, 2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya no está pendiente");
    }

    @Test
    void rechazarSolicitudShouldChangeStatusWithReason() {
        Usuario alumno = buildUsuario(1L, "alumno@test.es");
        Usuario tutorUsuario = buildUsuario(2L, "tutor@test.es");
        Tutor tutor = buildTutor(10L, tutorUsuario);

        SolicitudContratacionDirecta solicitud =
                SolicitudContratacionDirecta.builder()
                        .id(100L)
                        .alumno(alumno)
                        .tutor(tutor)
                        .estado(EstadoSolicitudContratacion.PENDIENTE)
                        .dia(LocalDate.of(2027, 6, 15))
                        .horaInicio(LocalTime.of(10, 0))
                        .horaFin(LocalTime.of(11, 0))
                        .importeTotal(BigDecimal.valueOf(20))
                        .tarifaHora(BigDecimal.valueOf(20))
                        .build();

        when(solicitudRepository.findById(100L)).thenReturn(Optional.of(solicitud));
        when(solicitudRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.rechazarSolicitud(100L, 2L, "No disponible");

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitudContratacion.RECHAZADA);
        assertThat(solicitud.getMotivoRechazo()).isEqualTo("No disponible");
    }

    @Test
    void marcarComoPagadaShouldChangeStatusAndNotifyTutor() {
        Usuario alumno = buildUsuario(1L, "alumno@test.es");
        Usuario tutorUsuario = buildUsuario(2L, "tutor@test.es");
        Tutor tutor = buildTutor(10L, tutorUsuario);

        SolicitudContratacionDirecta solicitud =
                SolicitudContratacionDirecta.builder()
                        .id(100L)
                        .alumno(alumno)
                        .tutor(tutor)
                        .estado(EstadoSolicitudContratacion.ACEPTADA)
                        .dia(LocalDate.now().plusDays(5))
                        .horaInicio(LocalTime.of(10, 0))
                        .horaFin(LocalTime.of(11, 0))
                        .importeTotal(BigDecimal.valueOf(20))
                        .tarifaHora(BigDecimal.valueOf(20))
                        .build();

        when(solicitudRepository.findById(100L)).thenReturn(Optional.of(solicitud));
        when(solicitudRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.marcarComoPagada(100L, 1L, null);

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitudContratacion.PAGADA);
        verify(broker)
                .convertAndSendToUser(eq("2"), eq("/queue/solicitud_contratacion_pagada"), any());
    }

    @Test
    void marcarComoPagadaShouldThrowWhenNotAlumno() {
        Usuario alumno = buildUsuario(1L, "alumno@test.es");
        SolicitudContratacionDirecta solicitud =
                SolicitudContratacionDirecta.builder()
                        .id(100L)
                        .alumno(alumno)
                        .estado(EstadoSolicitudContratacion.ACEPTADA)
                        .build();

        when(solicitudRepository.findById(100L)).thenReturn(Optional.of(solicitud));

        assertThatThrownBy(() -> service.marcarComoPagada(100L, 99L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No tienes permiso");
    }

    @Test
    void marcarComoPagadaShouldThrowWhenNotAccepted() {
        Usuario alumno = buildUsuario(1L, "alumno@test.es");
        SolicitudContratacionDirecta solicitud =
                SolicitudContratacionDirecta.builder()
                        .id(100L)
                        .alumno(alumno)
                        .estado(EstadoSolicitudContratacion.PENDIENTE)
                        .build();

        when(solicitudRepository.findById(100L)).thenReturn(Optional.of(solicitud));

        assertThatThrownBy(() -> service.marcarComoPagada(100L, 1L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("aceptada para poder pagarla");
    }

    @Test
    void obtenerSolicitudesDelTutorShouldReturnList() {
        Usuario tutorUsuario = buildUsuario(2L, "tutor@test.es");
        Tutor tutor = buildTutor(10L, tutorUsuario);

        when(tutorRepository.findByUsuarioId(2L)).thenReturn(Optional.of(tutor));
        when(solicitudRepository.findByTutorIdOrderByCreatedAtDesc(10L)).thenReturn(List.of());

        List<SolicitudContratacionResponse> result = service.obtenerSolicitudesDelTutor(2L);
        assertThat(result).isEmpty();
    }

    @Test
    void obtenerSolicitudesDelAlumnoShouldReturnList() {
        when(solicitudRepository.findByAlumnoIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        List<SolicitudContratacionResponse> result = service.obtenerSolicitudesDelAlumno(1L);
        assertThat(result).isEmpty();
    }

    @Test
    void reprogramarSolicitudShouldAllowUpToTwoDaysForPagada() {
        Usuario alumno = buildUsuario(1L, "alumno@test.es");
        Usuario tutorUsuario = buildUsuario(2L, "tutor@test.es");
        Tutor tutor = buildTutor(10L, tutorUsuario);

        // Clase original el día 15
        SolicitudContratacionDirecta solicitud =
                SolicitudContratacionDirecta.builder()
                        .id(100L)
                        .alumno(alumno)
                        .tutor(tutor)
                        .estado(EstadoSolicitudContratacion.PAGADA)
                        .dia(LocalDate.of(2027, 6, 15))
                        .horaInicio(LocalTime.of(10, 0))
                        .horaFin(LocalTime.of(11, 0))
                        .importeTotal(BigDecimal.valueOf(20))
                        .tarifaHora(BigDecimal.valueOf(20))
                        .build();

        when(solicitudRepository.findById(100L)).thenReturn(Optional.of(solicitud));
        when(solicitudRepository.findConflictingBookingsExcluding(
                        any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(disponibilidadService.getDisponibilidadesPorFecha(eq(10L), any()))
                .thenReturn(List.of(buildDisponibilidad(LocalTime.of(9, 0), LocalTime.of(12, 0))));
        when(solicitudRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Reprogramar 2 días después (día 17) con la misma duración
        SolicitudContratacionResponse result =
                service.reprogramarSolicitud(
                        100L,
                        2L,
                        LocalDate.of(2027, 6, 17),
                        LocalTime.of(10, 0),
                        LocalTime.of(11, 0));

        assertThat(solicitud.getEstado())
                .isEqualTo(EstadoSolicitudContratacion.REPROGRAMACION_PENDIENTE);
        assertThat(solicitud.getDia()).isEqualTo(LocalDate.of(2027, 6, 15));
        assertThat(solicitud.getReprogramacionDia()).isEqualTo(LocalDate.of(2027, 6, 17));
        assertThat(result.getDia()).isEqualTo(LocalDate.of(2027, 6, 15));
        assertThat(result.getReprogramacionDia()).isEqualTo("2027-06-17");
    }

    @Test
    void reprogramarSolicitudShouldThrowWhenPagadaGoesBackInTime() {
        Usuario tutorUsuario = buildUsuario(2L, "tutor@test.es");
        Tutor tutor = buildTutor(10L, tutorUsuario);

        SolicitudContratacionDirecta solicitud =
                SolicitudContratacionDirecta.builder()
                        .id(100L)
                        .tutor(tutor)
                        .estado(EstadoSolicitudContratacion.PAGADA)
                        .dia(LocalDate.of(2027, 6, 15))
                        .horaInicio(LocalTime.of(10, 0))
                        .horaFin(LocalTime.of(11, 0))
                        .build();

        when(solicitudRepository.findById(100L)).thenReturn(Optional.of(solicitud));

        // Intentar reprogramar 1 día antes
        assertThatThrownBy(
                        () ->
                                service.reprogramarSolicitud(
                                        100L,
                                        2L,
                                        LocalDate.of(2027, 6, 14),
                                        LocalTime.of(10, 0),
                                        LocalTime.of(11, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no se pueden adelantar");
    }

    @Test
    void reprogramarSolicitudShouldThrowWhenPagadaExceedsTwoDays() {
        Usuario tutorUsuario = buildUsuario(2L, "tutor@test.es");
        Tutor tutor = buildTutor(10L, tutorUsuario);

        SolicitudContratacionDirecta solicitud =
                SolicitudContratacionDirecta.builder()
                        .id(100L)
                        .tutor(tutor)
                        .estado(EstadoSolicitudContratacion.PAGADA)
                        .dia(LocalDate.of(2027, 6, 15))
                        .horaInicio(LocalTime.of(10, 0))
                        .horaFin(LocalTime.of(11, 0))
                        .build();

        when(solicitudRepository.findById(100L)).thenReturn(Optional.of(solicitud));

        // Intentar reprogramar 3 días después
        assertThatThrownBy(
                        () ->
                                service.reprogramarSolicitud(
                                        100L,
                                        2L,
                                        LocalDate.of(2027, 6, 18),
                                        LocalTime.of(10, 0),
                                        LocalTime.of(11, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("máximo de 2 días desde la fecha actual");
    }

    @Test
    void reprogramarSolicitudShouldAllowMultipleConsecutiveValidReschedules() {
        Usuario alumno = buildUsuario(1L, "alumno@test.es");
        Usuario tutorUsuario = buildUsuario(2L, "tutor@test.es");
        Tutor tutor = buildTutor(10L, tutorUsuario);

        SolicitudContratacionDirecta solicitud =
                SolicitudContratacionDirecta.builder()
                        .id(100L)
                        .alumno(alumno)
                        .tutor(tutor)
                        .estado(EstadoSolicitudContratacion.PAGADA)
                        .dia(LocalDate.of(2027, 6, 15))
                        .horaInicio(LocalTime.of(10, 0))
                        .horaFin(LocalTime.of(11, 0))
                        .importeTotal(BigDecimal.valueOf(20))
                        .tarifaHora(BigDecimal.valueOf(20))
                        .build();

        when(solicitudRepository.findById(100L)).thenReturn(Optional.of(solicitud));
        when(solicitudRepository.findConflictingBookingsExcluding(
                        any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(disponibilidadService.getDisponibilidadesPorFecha(eq(10L), any()))
                .thenReturn(List.of(buildDisponibilidad(LocalTime.of(9, 0), LocalTime.of(12, 0))));
        when(solicitudRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Primer aplazamiento: +2 días (Día 17)
        service.reprogramarSolicitud(
                100L, 2L, LocalDate.of(2027, 6, 17), LocalTime.of(10, 0), LocalTime.of(11, 0));

        // El alumno aprueba para aplicar la nueva fecha.
        service.aprobarReprogramacion(100L, 1L);
        assertThat(solicitud.getDia()).isEqualTo(LocalDate.of(2027, 6, 17));

        // Segundo aplazamiento: +2 días desde el día 17 (Día 19)
        SolicitudContratacionResponse result2 =
                service.reprogramarSolicitud(
                        100L,
                        2L,
                        LocalDate.of(2027, 6, 19),
                        LocalTime.of(10, 0),
                        LocalTime.of(11, 0));

        assertThat(solicitud.getDia()).isEqualTo(LocalDate.of(2027, 6, 17));
        assertThat(solicitud.getReprogramacionDia()).isEqualTo(LocalDate.of(2027, 6, 19));
        assertThat(result2.getDia()).isEqualTo(LocalDate.of(2027, 6, 17));
        assertThat(result2.getReprogramacionDia()).isEqualTo("2027-06-19");
    }

    @Test
    void reprogramarSolicitudShouldThrowOnSecondRescheduleIfExceedsTwoDays() {
        Usuario alumno = buildUsuario(1L, "alumno@test.es");
        Usuario tutorUsuario = buildUsuario(2L, "tutor@test.es");
        Tutor tutor = buildTutor(10L, tutorUsuario);

        SolicitudContratacionDirecta solicitud =
                SolicitudContratacionDirecta.builder()
                        .id(100L)
                        .alumno(alumno)
                        .tutor(tutor)
                        .estado(EstadoSolicitudContratacion.PAGADA)
                        .dia(LocalDate.of(2027, 6, 15))
                        .horaInicio(LocalTime.of(10, 0))
                        .horaFin(LocalTime.of(11, 0))
                        .importeTotal(BigDecimal.valueOf(20))
                        .tarifaHora(BigDecimal.valueOf(20))
                        .build();

        when(solicitudRepository.findById(100L)).thenReturn(Optional.of(solicitud));
        when(solicitudRepository.findConflictingBookingsExcluding(
                        any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(disponibilidadService.getDisponibilidadesPorFecha(eq(10L), any()))
                .thenReturn(List.of(buildDisponibilidad(LocalTime.of(9, 0), LocalTime.of(12, 0))));
        when(solicitudRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Primer aplazamiento: +2 días (Día 17) -> Legal
        service.reprogramarSolicitud(
                100L, 2L, LocalDate.of(2027, 6, 17), LocalTime.of(10, 0), LocalTime.of(11, 0));

        // El alumno aprueba para consolidar el cambio y permitir nueva reprogramación.
        service.aprobarReprogramacion(100L, 1L);
        assertThat(solicitud.getDia()).isEqualTo(LocalDate.of(2027, 6, 17));

        // Segundo aplazamiento: +3 días desde el día 17 (Día 20) -> Ilegal
        assertThatThrownBy(
                        () ->
                                service.reprogramarSolicitud(
                                        100L,
                                        2L,
                                        LocalDate.of(2027, 6, 20),
                                        LocalTime.of(10, 0),
                                        LocalTime.of(11, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("máximo de 2 días desde la fecha actual");
    }

    // ── obtenerSolicitudesPendientesDelTutor ──────────────────────────

    @Test
    void obtenerSolicitudesPendientesDelTutorShouldReturnMappedList() {
        Usuario tutorUser = buildUsuario(2L, "tutor@test.es");
        Tutor tutor = buildTutor(10L, tutorUser);
        Usuario alumno = buildUsuario(1L, "alumno@test.es");

        when(tutorRepository.findByUsuarioId(2L)).thenReturn(Optional.of(tutor));

        SolicitudContratacionDirecta sol =
                SolicitudContratacionDirecta.builder()
                        .id(100L)
                        .alumno(alumno)
                        .tutor(tutor)
                        .estado(EstadoSolicitudContratacion.PENDIENTE)
                        .dia(LocalDate.of(2027, 6, 15))
                        .horaInicio(LocalTime.of(10, 0))
                        .horaFin(LocalTime.of(11, 0))
                        .tarifaHora(BigDecimal.valueOf(20))
                        .importeTotal(BigDecimal.valueOf(20))
                        .build();

        when(solicitudRepository.findPendientesByTutorId(10L)).thenReturn(List.of(sol));

        List<SolicitudContratacionResponse> result =
                service.obtenerSolicitudesPendientesDelTutor(2L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(100L);
    }

    @Test
    void obtenerSolicitudesPendientesDelTutorShouldThrowWhenTutorNotFound() {
        when(tutorRepository.findByUsuarioId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerSolicitudesPendientesDelTutor(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Perfil de tutor no encontrado");
    }

    // ── getHorariosOcupados ──────────────────────────────────────────

    @Test
    void getHorariosOcupadosShouldReturnOccupiedSlots() {
        Usuario alumno = buildUsuario(1L, "alumno@test.es");
        Usuario tutorUser = buildUsuario(2L, "tutor@test.es");
        Tutor tutor = buildTutor(10L, tutorUser);

        SolicitudContratacionDirecta sol =
                SolicitudContratacionDirecta.builder()
                        .id(1L)
                        .alumno(alumno)
                        .tutor(tutor)
                        .estado(EstadoSolicitudContratacion.PAGADA)
                        .dia(LocalDate.of(2027, 6, 15))
                        .horaInicio(LocalTime.of(10, 0))
                        .horaFin(LocalTime.of(11, 0))
                        .tarifaHora(BigDecimal.valueOf(20))
                        .importeTotal(BigDecimal.valueOf(20))
                        .build();

        when(solicitudRepository.findActiveBookingsByTutorAndDate(10L, LocalDate.of(2027, 6, 15)))
                .thenReturn(List.of(sol));

        List<HorarioOcupadoResponse> result =
                service.getHorariosOcupados(10L, LocalDate.of(2027, 6, 15));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).horaInicio()).isEqualTo("10:00");
        assertThat(result.get(0).horaFin()).isEqualTo("11:00");
    }

    // ── obtenerSolicitudParaPago ─────────────────────────────────────

    @Test
    void obtenerSolicitudParaPagoShouldReturnWhenAccepted() {
        Usuario alumno = buildUsuario(1L, "alumno@test.es");
        Usuario tutorUser = buildUsuario(2L, "tutor@test.es");
        Tutor tutor = buildTutor(10L, tutorUser);

        SolicitudContratacionDirecta sol =
                SolicitudContratacionDirecta.builder()
                        .id(100L)
                        .alumno(alumno)
                        .tutor(tutor)
                        .estado(EstadoSolicitudContratacion.ACEPTADA)
                        .dia(LocalDate.of(2027, 6, 15))
                        .horaInicio(LocalTime.of(10, 0))
                        .horaFin(LocalTime.of(11, 0))
                        .tarifaHora(BigDecimal.valueOf(20))
                        .importeTotal(BigDecimal.valueOf(20))
                        .build();

        when(solicitudRepository.findById(100L)).thenReturn(Optional.of(sol));

        SolicitudContratacionResponse result = service.obtenerSolicitudParaPago(100L, 1L);

        assertThat(result.getId()).isEqualTo(100L);
    }

    @Test
    void obtenerSolicitudParaPagoShouldThrowWhenNotAlumno() {
        Usuario alumno = buildUsuario(1L, "alumno@test.es");
        Usuario tutorUser = buildUsuario(2L, "tutor@test.es");
        Tutor tutor = buildTutor(10L, tutorUser);

        SolicitudContratacionDirecta sol =
                SolicitudContratacionDirecta.builder()
                        .id(100L)
                        .alumno(alumno)
                        .tutor(tutor)
                        .estado(EstadoSolicitudContratacion.ACEPTADA)
                        .dia(LocalDate.of(2027, 6, 15))
                        .horaInicio(LocalTime.of(10, 0))
                        .horaFin(LocalTime.of(11, 0))
                        .tarifaHora(BigDecimal.valueOf(20))
                        .importeTotal(BigDecimal.valueOf(20))
                        .build();

        when(solicitudRepository.findById(100L)).thenReturn(Optional.of(sol));

        assertThatThrownBy(() -> service.obtenerSolicitudParaPago(100L, 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No tienes permiso para esta operación");
    }

    @Test
    void obtenerSolicitudParaPagoShouldThrowWhenNotAccepted() {
        Usuario alumno = buildUsuario(1L, "alumno@test.es");
        Usuario tutorUser = buildUsuario(2L, "tutor@test.es");
        Tutor tutor = buildTutor(10L, tutorUser);

        SolicitudContratacionDirecta sol =
                SolicitudContratacionDirecta.builder()
                        .id(100L)
                        .alumno(alumno)
                        .tutor(tutor)
                        .estado(EstadoSolicitudContratacion.PENDIENTE)
                        .dia(LocalDate.of(2027, 6, 15))
                        .horaInicio(LocalTime.of(10, 0))
                        .horaFin(LocalTime.of(11, 0))
                        .tarifaHora(BigDecimal.valueOf(20))
                        .importeTotal(BigDecimal.valueOf(20))
                        .build();

        when(solicitudRepository.findById(100L)).thenReturn(Optional.of(sol));

        assertThatThrownBy(() -> service.obtenerSolicitudParaPago(100L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La solicitud debe estar aceptada para poder pagarla");
    }

    @Test
    void obtenerSolicitudParaPagoShouldThrowWhenDateInPast() {
        Usuario alumno = buildUsuario(1L, "alumno@test.es");
        Usuario tutorUser = buildUsuario(2L, "tutor@test.es");
        Tutor tutor = buildTutor(10L, tutorUser);

        SolicitudContratacionDirecta sol =
                SolicitudContratacionDirecta.builder()
                        .id(100L)
                        .alumno(alumno)
                        .tutor(tutor)
                        .estado(EstadoSolicitudContratacion.ACEPTADA)
                        .dia(LocalDate.of(2020, 1, 1))
                        .horaInicio(LocalTime.of(10, 0))
                        .horaFin(LocalTime.of(11, 0))
                        .tarifaHora(BigDecimal.valueOf(20))
                        .importeTotal(BigDecimal.valueOf(20))
                        .build();

        when(solicitudRepository.findById(100L)).thenReturn(Optional.of(sol));

        assertThatThrownBy(() -> service.obtenerSolicitudParaPago(100L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya ha pasado");
    }

    // ── calificarSolicitud ──────────────────────────────────────────

    @Test
    void calificarSolicitudShouldSetCalificacionAndSave() {
        Usuario alumno = buildUsuario(1L, "alumno@test.es");
        Usuario tutorUser = buildUsuario(2L, "tutor@test.es");
        Tutor tutor = buildTutor(10L, tutorUser);

        SolicitudContratacionDirecta sol =
                SolicitudContratacionDirecta.builder()
                        .id(100L)
                        .alumno(alumno)
                        .tutor(tutor)
                        .estado(EstadoSolicitudContratacion.COMPLETADA)
                        .dia(LocalDate.of(2027, 6, 15))
                        .horaInicio(LocalTime.of(10, 0))
                        .horaFin(LocalTime.of(11, 0))
                        .tarifaHora(BigDecimal.valueOf(20))
                        .importeTotal(BigDecimal.valueOf(20))
                        .build();

        when(solicitudRepository.findById(100L)).thenReturn(Optional.of(sol));
        when(solicitudRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SolicitudContratacionResponse result = service.calificarSolicitud(100L, 1L, 5, "Excelente");

        assertThat(sol.getCalificacion()).isEqualTo(5);
        assertThat(sol.getComentarioAlumno()).isEqualTo("Excelente");
        verify(solicitudRepository).save(sol);
    }

    @Test
    void calificarSolicitudShouldThrowWhenNotAlumno() {
        Usuario alumno = buildUsuario(1L, "alumno@test.es");
        Usuario tutorUser = buildUsuario(2L, "tutor@test.es");
        Tutor tutor = buildTutor(10L, tutorUser);

        SolicitudContratacionDirecta sol =
                SolicitudContratacionDirecta.builder()
                        .id(100L)
                        .alumno(alumno)
                        .tutor(tutor)
                        .estado(EstadoSolicitudContratacion.COMPLETADA)
                        .dia(LocalDate.of(2027, 6, 15))
                        .horaInicio(LocalTime.of(10, 0))
                        .horaFin(LocalTime.of(11, 0))
                        .tarifaHora(BigDecimal.valueOf(20))
                        .importeTotal(BigDecimal.valueOf(20))
                        .build();

        when(solicitudRepository.findById(100L)).thenReturn(Optional.of(sol));

        assertThatThrownBy(() -> service.calificarSolicitud(100L, 99L, 5, "ok"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Solo el alumno puede calificar esta clase");
    }

    @Test
    void calificarSolicitudShouldThrowWhenInvalidRating() {
        Usuario alumno = buildUsuario(1L, "alumno@test.es");
        Usuario tutorUser = buildUsuario(2L, "tutor@test.es");
        Tutor tutor = buildTutor(10L, tutorUser);

        SolicitudContratacionDirecta sol =
                SolicitudContratacionDirecta.builder()
                        .id(100L)
                        .alumno(alumno)
                        .tutor(tutor)
                        .estado(EstadoSolicitudContratacion.COMPLETADA)
                        .dia(LocalDate.of(2027, 6, 15))
                        .horaInicio(LocalTime.of(10, 0))
                        .horaFin(LocalTime.of(11, 0))
                        .tarifaHora(BigDecimal.valueOf(20))
                        .importeTotal(BigDecimal.valueOf(20))
                        .build();

        when(solicitudRepository.findById(100L)).thenReturn(Optional.of(sol));

        assertThatThrownBy(() -> service.calificarSolicitud(100L, 1L, 6, "too high"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entre 1 y 5");
    }

    // ── cancelarSolicitud (por tutor) ────────────────────────────────

    @Test
    void cancelarSolicitudShouldCancelWhenPendiente() {
        Usuario alumno = buildUsuario(1L, "alumno@test.es");
        Usuario tutorUser = buildUsuario(2L, "tutor@test.es");
        Tutor tutor = buildTutor(10L, tutorUser);

        SolicitudContratacionDirecta sol =
                SolicitudContratacionDirecta.builder()
                        .id(100L)
                        .alumno(alumno)
                        .tutor(tutor)
                        .estado(EstadoSolicitudContratacion.PENDIENTE)
                        .dia(LocalDate.of(2027, 6, 15))
                        .horaInicio(LocalTime.of(10, 0))
                        .horaFin(LocalTime.of(11, 0))
                        .tarifaHora(BigDecimal.valueOf(20))
                        .importeTotal(BigDecimal.valueOf(20))
                        .build();

        when(solicitudRepository.findById(100L)).thenReturn(Optional.of(sol));
        when(solicitudRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SolicitudContratacionResponse result = service.cancelarSolicitud(100L, 2L, "No disponible");

        assertThat(sol.getEstado()).isEqualTo(EstadoSolicitudContratacion.CANCELADA_TUTOR);
        assertThat(sol.getMotivoRechazo()).isEqualTo("No disponible");
        verify(solicitudRepository).save(sol);
        verify(broker)
                .convertAndSendToUser(
                        eq("1"), eq("/queue/solicitud_contratacion_respuesta"), any());
    }

    @Test
    void cancelarSolicitudShouldRefundWhenPagada() throws Exception {
        Usuario alumno = buildUsuario(1L, "alumno@test.es");
        Usuario tutorUser = buildUsuario(2L, "tutor@test.es");
        Tutor tutor = buildTutor(10L, tutorUser);

        SolicitudContratacionDirecta sol =
                SolicitudContratacionDirecta.builder()
                        .id(100L)
                        .alumno(alumno)
                        .tutor(tutor)
                        .estado(EstadoSolicitudContratacion.PAGADA)
                        .dia(LocalDate.of(2027, 6, 15))
                        .horaInicio(LocalTime.of(10, 0))
                        .horaFin(LocalTime.of(11, 0))
                        .tarifaHora(BigDecimal.valueOf(20))
                        .importeTotal(BigDecimal.valueOf(20))
                        .stripePaymentIntentId("pi_test")
                        .build();

        when(solicitudRepository.findById(100L)).thenReturn(Optional.of(sol));
        when(solicitudRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.cancelarSolicitud(100L, 2L, "Urgencia");

        verify(paymentService)
                .reembolsarPago(
                        eq("pi_test"), any(Usuario.class), any(Tutor.class), any(BigDecimal.class));
        assertThat(sol.getEstado()).isEqualTo(EstadoSolicitudContratacion.CANCELADA_TUTOR);
    }

    @Test
    void cancelarSolicitudShouldThrowWhenNotTutor() {
        Usuario alumno = buildUsuario(1L, "alumno@test.es");
        Usuario tutorUser = buildUsuario(2L, "tutor@test.es");
        Tutor tutor = buildTutor(10L, tutorUser);

        SolicitudContratacionDirecta sol =
                SolicitudContratacionDirecta.builder()
                        .id(100L)
                        .alumno(alumno)
                        .tutor(tutor)
                        .estado(EstadoSolicitudContratacion.PENDIENTE)
                        .dia(LocalDate.of(2027, 6, 15))
                        .horaInicio(LocalTime.of(10, 0))
                        .horaFin(LocalTime.of(11, 0))
                        .tarifaHora(BigDecimal.valueOf(20))
                        .importeTotal(BigDecimal.valueOf(20))
                        .build();

        when(solicitudRepository.findById(100L)).thenReturn(Optional.of(sol));

        assertThatThrownBy(() -> service.cancelarSolicitud(100L, 99L, "motivo"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No tienes permiso para gestionar esta solicitud");
    }

    @Test
    void cancelarSolicitudShouldThrowWhenBadState() {
        Usuario alumno = buildUsuario(1L, "alumno@test.es");
        Usuario tutorUser = buildUsuario(2L, "tutor@test.es");
        Tutor tutor = buildTutor(10L, tutorUser);

        SolicitudContratacionDirecta sol =
                SolicitudContratacionDirecta.builder()
                        .id(100L)
                        .alumno(alumno)
                        .tutor(tutor)
                        .estado(EstadoSolicitudContratacion.COMPLETADA)
                        .dia(LocalDate.of(2027, 6, 15))
                        .horaInicio(LocalTime.of(10, 0))
                        .horaFin(LocalTime.of(11, 0))
                        .tarifaHora(BigDecimal.valueOf(20))
                        .importeTotal(BigDecimal.valueOf(20))
                        .build();

        when(solicitudRepository.findById(100L)).thenReturn(Optional.of(sol));

        assertThatThrownBy(() -> service.cancelarSolicitud(100L, 2L, "motivo"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no se puede cancelar");
    }

    // ── cancelarPorAlumno ────────────────────────────────────────────

    @Test
    void cancelarPorAlumnoShouldCancelWhenAceptada() {
        Usuario alumno = buildUsuario(1L, "alumno@test.es");
        Usuario tutorUser = buildUsuario(2L, "tutor@test.es");
        Tutor tutor = buildTutor(10L, tutorUser);

        SolicitudContratacionDirecta sol =
                SolicitudContratacionDirecta.builder()
                        .id(100L)
                        .alumno(alumno)
                        .tutor(tutor)
                        .estado(EstadoSolicitudContratacion.ACEPTADA)
                        .dia(LocalDate.of(2027, 6, 15))
                        .horaInicio(LocalTime.of(10, 0))
                        .horaFin(LocalTime.of(11, 0))
                        .tarifaHora(BigDecimal.valueOf(20))
                        .importeTotal(BigDecimal.valueOf(20))
                        .build();

        when(solicitudRepository.findById(100L)).thenReturn(Optional.of(sol));
        when(solicitudRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SolicitudContratacionResponse result =
                service.cancelarPorAlumno(100L, 1L, "Cambio de planes");

        assertThat(sol.getEstado()).isEqualTo(EstadoSolicitudContratacion.CANCELADA_ALUMNO);
        assertThat(sol.getMotivoRechazo()).isEqualTo("Cambio de planes");
        verify(solicitudRepository).save(sol);
        verify(broker)
                .convertAndSendToUser(
                        eq("2"), eq("/queue/solicitud_contratacion_respuesta"), any());
    }

    @Test
    void cancelarPorAlumnoShouldThrowWhenNotAlumno() {
        Usuario alumno = buildUsuario(1L, "alumno@test.es");
        Usuario tutorUser = buildUsuario(2L, "tutor@test.es");
        Tutor tutor = buildTutor(10L, tutorUser);

        SolicitudContratacionDirecta sol =
                SolicitudContratacionDirecta.builder()
                        .id(100L)
                        .alumno(alumno)
                        .tutor(tutor)
                        .estado(EstadoSolicitudContratacion.ACEPTADA)
                        .dia(LocalDate.of(2027, 6, 15))
                        .horaInicio(LocalTime.of(10, 0))
                        .horaFin(LocalTime.of(11, 0))
                        .tarifaHora(BigDecimal.valueOf(20))
                        .importeTotal(BigDecimal.valueOf(20))
                        .build();

        when(solicitudRepository.findById(100L)).thenReturn(Optional.of(sol));

        assertThatThrownBy(() -> service.cancelarPorAlumno(100L, 99L, "motivo"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No tienes permiso para esta operación");
    }

    @Test
    void cancelarPorAlumnoShouldThrowWhenBadState() {
        Usuario alumno = buildUsuario(1L, "alumno@test.es");
        Usuario tutorUser = buildUsuario(2L, "tutor@test.es");
        Tutor tutor = buildTutor(10L, tutorUser);

        SolicitudContratacionDirecta sol =
                SolicitudContratacionDirecta.builder()
                        .id(100L)
                        .alumno(alumno)
                        .tutor(tutor)
                        .estado(EstadoSolicitudContratacion.COMPLETADA)
                        .dia(LocalDate.of(2027, 6, 15))
                        .horaInicio(LocalTime.of(10, 0))
                        .horaFin(LocalTime.of(11, 0))
                        .tarifaHora(BigDecimal.valueOf(20))
                        .importeTotal(BigDecimal.valueOf(20))
                        .build();

        when(solicitudRepository.findById(100L)).thenReturn(Optional.of(sol));

        assertThatThrownBy(() -> service.cancelarPorAlumno(100L, 1L, "motivo"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no se puede cancelar");
    }

    // ── rechazarReprogramacion ───────────────────────────────────────

    @Test
    void rechazarReprogramacionShouldRestoreEstadoAnterior() {
        Usuario alumno = buildUsuario(1L, "alumno@test.es");
        Usuario tutorUser = buildUsuario(2L, "tutor@test.es");
        Tutor tutor = buildTutor(10L, tutorUser);

        SolicitudContratacionDirecta sol =
                SolicitudContratacionDirecta.builder()
                        .id(100L)
                        .alumno(alumno)
                        .tutor(tutor)
                        .estado(EstadoSolicitudContratacion.REPROGRAMACION_PENDIENTE)
                        .estadoAnterior(EstadoSolicitudContratacion.PAGADA)
                        .dia(LocalDate.of(2027, 6, 15))
                        .horaInicio(LocalTime.of(10, 0))
                        .horaFin(LocalTime.of(11, 0))
                        .tarifaHora(BigDecimal.valueOf(20))
                        .importeTotal(BigDecimal.valueOf(20))
                        .reprogramacionDia(LocalDate.of(2027, 6, 20))
                        .reprogramacionHoraInicio(LocalTime.of(14, 0))
                        .reprogramacionHoraFin(LocalTime.of(15, 0))
                        .build();

        when(solicitudRepository.findById(100L)).thenReturn(Optional.of(sol));
        when(solicitudRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SolicitudContratacionResponse result = service.rechazarReprogramacion(100L, 1L);

        assertThat(sol.getEstado()).isEqualTo(EstadoSolicitudContratacion.PAGADA);
        assertThat(sol.getReprogramacionDia()).isNull();
        assertThat(sol.getReprogramacionHoraInicio()).isNull();
        assertThat(sol.getReprogramacionHoraFin()).isNull();
        assertThat(sol.getEstadoAnterior()).isNull();
        verify(solicitudRepository).save(sol);
    }

    @Test
    void rechazarReprogramacionShouldThrowWhenNotReprogramacionPendiente() {
        Usuario alumno = buildUsuario(1L, "alumno@test.es");
        Usuario tutorUser = buildUsuario(2L, "tutor@test.es");
        Tutor tutor = buildTutor(10L, tutorUser);

        SolicitudContratacionDirecta sol =
                SolicitudContratacionDirecta.builder()
                        .id(100L)
                        .alumno(alumno)
                        .tutor(tutor)
                        .estado(EstadoSolicitudContratacion.PAGADA)
                        .dia(LocalDate.of(2027, 6, 15))
                        .horaInicio(LocalTime.of(10, 0))
                        .horaFin(LocalTime.of(11, 0))
                        .tarifaHora(BigDecimal.valueOf(20))
                        .importeTotal(BigDecimal.valueOf(20))
                        .build();

        when(solicitudRepository.findById(100L)).thenReturn(Optional.of(sol));

        assertThatThrownBy(() -> service.rechazarReprogramacion(100L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No hay reprogramación pendiente");
    }

    // ── aprobarReprogramacion ────────────────────────────────────────

    @Test
    void aprobarReprogramacionShouldApplyNewScheduleAndRecalculate() {
        Usuario alumno = buildUsuario(1L, "alumno@test.es");
        Usuario tutorUser = buildUsuario(2L, "tutor@test.es");
        Tutor tutor = buildTutor(10L, tutorUser);

        SolicitudContratacionDirecta sol =
                SolicitudContratacionDirecta.builder()
                        .id(100L)
                        .alumno(alumno)
                        .tutor(tutor)
                        .estado(EstadoSolicitudContratacion.REPROGRAMACION_PENDIENTE)
                        .estadoAnterior(EstadoSolicitudContratacion.PAGADA)
                        .dia(LocalDate.of(2027, 6, 15))
                        .horaInicio(LocalTime.of(10, 0))
                        .horaFin(LocalTime.of(11, 0))
                        .tarifaHora(BigDecimal.valueOf(30))
                        .importeTotal(BigDecimal.valueOf(30))
                        .reprogramacionDia(LocalDate.of(2027, 6, 20))
                        .reprogramacionHoraInicio(LocalTime.of(14, 0))
                        .reprogramacionHoraFin(LocalTime.of(16, 0))
                        .build();

        when(solicitudRepository.findById(100L)).thenReturn(Optional.of(sol));
        when(solicitudRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SolicitudContratacionResponse result = service.aprobarReprogramacion(100L, 1L);

        assertThat(sol.getDia()).isEqualTo(LocalDate.of(2027, 6, 20));
        assertThat(sol.getHoraInicio()).isEqualTo(LocalTime.of(14, 0));
        assertThat(sol.getHoraFin()).isEqualTo(LocalTime.of(16, 0));
        // 2 hours * 30 = 60
        assertThat(sol.getImporteTotal()).isEqualByComparingTo(BigDecimal.valueOf(60));
        assertThat(sol.getEstado()).isEqualTo(EstadoSolicitudContratacion.PAGADA);
        assertThat(sol.getReprogramacionDia()).isNull();
        verify(solicitudRepository).save(sol);
    }

    @Test
    void aprobarReprogramacionShouldThrowWhenNotReprogramacionPendiente() {
        Usuario alumno = buildUsuario(1L, "alumno@test.es");
        Usuario tutorUser = buildUsuario(2L, "tutor@test.es");
        Tutor tutor = buildTutor(10L, tutorUser);

        SolicitudContratacionDirecta sol =
                SolicitudContratacionDirecta.builder()
                        .id(100L)
                        .alumno(alumno)
                        .tutor(tutor)
                        .estado(EstadoSolicitudContratacion.ACEPTADA)
                        .dia(LocalDate.of(2027, 6, 15))
                        .horaInicio(LocalTime.of(10, 0))
                        .horaFin(LocalTime.of(11, 0))
                        .tarifaHora(BigDecimal.valueOf(20))
                        .importeTotal(BigDecimal.valueOf(20))
                        .build();

        when(solicitudRepository.findById(100L)).thenReturn(Optional.of(sol));

        assertThatThrownBy(() -> service.aprobarReprogramacion(100L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No hay reprogramación pendiente");
    }
}
