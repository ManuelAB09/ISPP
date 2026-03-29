package es.us.meerkat.backend.scheduler;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import es.us.meerkat.backend.entity.tutors.EstadoSolicitudContratacion;
import es.us.meerkat.backend.entity.tutors.SolicitudContratacionDirecta;
import es.us.meerkat.backend.entity.tutors.Tutor;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.tutors.SolicitudContratacionDirectaRepository;
import es.us.meerkat.backend.service.emails.EmailService;

@ExtendWith(MockitoExtension.class)
class BookingReminderSchedulerTest {

    @Mock private SolicitudContratacionDirectaRepository solicitudRepository;
    @Mock private EmailService emailService;

    @InjectMocks private BookingReminderScheduler scheduler;

    private Usuario alumno;
    private Usuario tutorUser;
    private Tutor tutor;

    @BeforeEach
    void setUp() {
        alumno = new Usuario();
        alumno.setId(1L);
        alumno.setNombre("Alumno Test");
        alumno.setEmail("alumno@test.com");

        tutorUser = new Usuario();
        tutorUser.setId(2L);
        tutorUser.setNombre("Tutor Test");
        tutorUser.setEmail("tutor@test.com");

        tutor = new Tutor();
        tutor.setId(1L);
        tutor.setUsuario(tutorUser);
    }

    // ============= enviarRecordatorios =============

    @Test
    void enviarRecordatorios_noBookings_shouldDoNothing() {
        when(solicitudRepository.findBookingsForDate(any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        scheduler.enviarRecordatorios();
        verify(emailService, never())
                .sendBookingReminderEmail(
                        anyString(), anyString(), anyString(), any(), any(), any(), anyString());
    }

    @Test
    void enviarRecordatorios_withBooking_shouldSendEmailToBothParties() {
        SolicitudContratacionDirecta solicitud = buildSolicitud("ONLINE");
        when(solicitudRepository.findBookingsForDate(any(LocalDate.class)))
                .thenReturn(List.of(solicitud));

        scheduler.enviarRecordatorios();

        verify(emailService, times(2))
                .sendBookingReminderEmail(
                        anyString(), anyString(), anyString(), any(), any(), any(), eq("ONLINE"));
    }

    @Test
    void enviarRecordatorios_nullModalidad_shouldDefaultToOnline() {
        SolicitudContratacionDirecta solicitud = buildSolicitud(null);
        when(solicitudRepository.findBookingsForDate(any(LocalDate.class)))
                .thenReturn(List.of(solicitud));

        scheduler.enviarRecordatorios();

        verify(emailService, times(2))
                .sendBookingReminderEmail(
                        anyString(), anyString(), anyString(), any(), any(), any(), eq("ONLINE"));
    }

    @Test
    void enviarRecordatorios_emailThrows_shouldContinue() {
        SolicitudContratacionDirecta s1 = buildSolicitud("ONLINE");
        SolicitudContratacionDirecta s2 = buildSolicitud("PRESENCIAL");
        when(solicitudRepository.findBookingsForDate(any(LocalDate.class)))
                .thenReturn(List.of(s1, s2));
        doThrow(new RuntimeException("SMTP error"))
                .when(emailService)
                .sendBookingReminderEmail(
                        eq("alumno@test.com"),
                        anyString(),
                        anyString(),
                        any(),
                        any(),
                        any(),
                        anyString());

        assertThatCode(() -> scheduler.enviarRecordatorios()).doesNotThrowAnyException();
    }

    // ============= expirarSolicitudesVencidas =============

    @Test
    void expirarSolicitudes_noneExpired_shouldReturnEarly() {
        when(solicitudRepository.findExpiredAcceptedBookings(any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(solicitudRepository.findExpiredPendingBookings(any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        scheduler.expirarSolicitudesVencidas();
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void expirarSolicitudes_acceptedExpired_shouldSetCancelAndSendEmail() {
        SolicitudContratacionDirecta solicitud = buildSolicitud("ONLINE");
        solicitud.setEstado(EstadoSolicitudContratacion.ACEPTADA);

        when(solicitudRepository.findExpiredAcceptedBookings(any(LocalDate.class)))
                .thenReturn(List.of(solicitud));
        when(solicitudRepository.findExpiredPendingBookings(any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        scheduler.expirarSolicitudesVencidas();

        verify(solicitudRepository).save(solicitud);
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitudContratacion.CANCELADA_TUTOR);
        assertThat(solicitud.getMotivoRechazo()).contains("Expirada automáticamente");
        verify(emailService)
                .sendBookingExpiredEmail(
                        eq("alumno@test.com"),
                        eq("Alumno Test"),
                        eq("Tutor Test"),
                        any(),
                        any(),
                        any());
    }

    @Test
    void expirarSolicitudes_pendingExpired_shouldSetCancelNoEmail() {
        SolicitudContratacionDirecta solicitud = buildSolicitud("ONLINE");
        solicitud.setEstado(EstadoSolicitudContratacion.PENDIENTE);

        when(solicitudRepository.findExpiredAcceptedBookings(any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(solicitudRepository.findExpiredPendingBookings(any(LocalDate.class)))
                .thenReturn(List.of(solicitud));

        scheduler.expirarSolicitudesVencidas();

        verify(solicitudRepository).save(solicitud);
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitudContratacion.CANCELADA_TUTOR);
        verify(emailService, never())
                .sendBookingExpiredEmail(
                        anyString(), anyString(), anyString(), any(), any(), any());
    }

    @Test
    void expirarSolicitudes_emailFails_shouldStillContinue() {
        SolicitudContratacionDirecta solicitud = buildSolicitud("ONLINE");
        solicitud.setEstado(EstadoSolicitudContratacion.ACEPTADA);

        when(solicitudRepository.findExpiredAcceptedBookings(any(LocalDate.class)))
                .thenReturn(List.of(solicitud));
        when(solicitudRepository.findExpiredPendingBookings(any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        doThrow(new RuntimeException("SMTP error"))
                .when(emailService)
                .sendBookingExpiredEmail(
                        anyString(), anyString(), anyString(), any(), any(), any());

        assertThatCode(() -> scheduler.expirarSolicitudesVencidas()).doesNotThrowAnyException();
    }

    // ============= helper =============

    private SolicitudContratacionDirecta buildSolicitud(String modalidad) {
        return SolicitudContratacionDirecta.builder()
                .id(1L)
                .alumno(alumno)
                .tutor(tutor)
                .dia(LocalDate.now().plusDays(1))
                .horaInicio(LocalTime.of(10, 0))
                .horaFin(LocalTime.of(11, 0))
                .modalidad(modalidad)
                .tarifaHora(BigDecimal.TEN)
                .importeTotal(BigDecimal.TEN)
                .build();
    }
}
