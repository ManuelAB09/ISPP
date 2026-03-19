package es.us.meerkat.backend.service;

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

import es.us.meerkat.backend.dto.SolicitudContratacionRequest;
import es.us.meerkat.backend.dto.SolicitudContratacionResponse;
import es.us.meerkat.backend.entity.EstadoSolicitudContratacion;
import es.us.meerkat.backend.entity.SolicitudContratacionDirecta;
import es.us.meerkat.backend.entity.Tutor;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.SolicitudContratacionDirectaRepository;
import es.us.meerkat.backend.repository.TutorRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class SolicitudContratacionServiceTest {

    @Mock private SolicitudContratacionDirectaRepository solicitudRepository;
    @Mock private TutorRepository tutorRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private SimpMessagingTemplate broker;
    @Mock private EmailService emailService;

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
        req.setDia(LocalDate.of(2025, 6, 15));
        req.setHoraInicio(LocalTime.of(10, 0));
        req.setHoraFin(LocalTime.of(11, 0));
        req.setModalidad("ONLINE");
        req.setMensaje("Test message");
        return req;
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
        verify(broker)
                .convertAndSendToUser(
                        eq("tutor@test.es"), eq("/queue/solicitud_contratacion"), any());
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
                        .dia(LocalDate.of(2025, 6, 15))
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
                        eq("alumno@test.es"), eq("/queue/solicitud_contratacion_respuesta"), any());
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
                        .dia(LocalDate.of(2025, 6, 15))
                        .horaInicio(LocalTime.of(10, 0))
                        .horaFin(LocalTime.of(11, 0))
                        .importeTotal(BigDecimal.valueOf(20))
                        .tarifaHora(BigDecimal.valueOf(20))
                        .build();

        when(solicitudRepository.findById(100L)).thenReturn(Optional.of(solicitud));
        when(solicitudRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SolicitudContratacionResponse result = service.rechazarSolicitud(100L, 2L, "No disponible");

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

        service.marcarComoPagada(100L, 1L);

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitudContratacion.PAGADA);
        verify(broker)
                .convertAndSendToUser(
                        eq("tutor@test.es"), eq("/queue/solicitud_contratacion_pagada"), any());
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

        assertThatThrownBy(() -> service.marcarComoPagada(100L, 99L))
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

        assertThatThrownBy(() -> service.marcarComoPagada(100L, 1L))
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
}
