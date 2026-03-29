package es.us.meerkat.backend.service.emails;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import es.us.meerkat.backend.entity.communities.Anuncio;
import es.us.meerkat.backend.entity.communities.Comunidad;
import es.us.meerkat.backend.entity.events.Evento;
import es.us.meerkat.backend.entity.events.TipoEvento;
import es.us.meerkat.backend.entity.notifications.TipoRecordatorio;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.events.AsistenciaEventoRepository;
import jakarta.mail.internet.MimeMessage;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock private JavaMailSender mailSender;
    @Mock private MimeMessage mimeMessage;
    @Mock private AsistenciaEventoRepository asistenciaEventoRepository;

    @InjectMocks private EmailService emailService;

    @BeforeEach
    void setUp() throws Exception {
        setField("from", "test@meerkat.es");
        setField("appName", "Meerkat");
        setField("appUrl", "http://localhost:3000");
        setField("sendgridApiKey", "");
    }

    private void setField(String name, Object value) throws Exception {
        Field f = EmailService.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(emailService, value);
    }

    private Usuario buildUsuario(Long id) {
        return Usuario.builder()
                .id(id)
                .nombre("User " + id)
                .email("u" + id + "@test.es")
                .password("p")
                .build();
    }

    // ================================================================
    // sendPasswordResetEmail
    // ================================================================

    @Test
    void sendPasswordResetEmailShouldCreateAndSendMimeMessage() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendPasswordResetEmail(
                "user@test.es", "Test User", "http://localhost:3000/reset-password?token=abc123");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendPasswordResetEmailShouldRethrowOnFailure() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(
                        () ->
                                emailService.sendPasswordResetEmail(
                                        "user@test.es",
                                        "Test User",
                                        "http://localhost:3000/reset-password?token=abc123"))
                .isInstanceOf(RuntimeException.class);
    }

    // ================================================================
    // sendSimpleEmail
    // ================================================================

    @Test
    void sendSimpleEmailShouldSendMessage() {
        emailService.sendSimpleEmail("to@test.es", "Subject", "Body text");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendSimpleEmailShouldThrowOnFailure() {
        doThrow(new RuntimeException("SMTP error"))
                .when(mailSender)
                .send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> emailService.sendSimpleEmail("to@test.es", "Subject", "Body"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No se pudo enviar el email");
    }

    // ================================================================
    // sendVerificationEmail
    // ================================================================

    @Test
    void sendVerificationEmailShouldCreateAndSendMimeMessage() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendVerificationEmail(
                "user@test.es", "Test User", "token123", "https://app.test/verify");

        verify(mailSender).send(mimeMessage);
    }

    // ================================================================
    // sendInstitutionInviteEmail
    // ================================================================

    @Test
    void sendInstitutionInviteEmailShouldSendForNewAccount() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendInstitutionInviteEmail(
                "user@test.es", "ACME Corp", "Premium", true, "tok123", "http://front");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendInstitutionInviteEmailShouldSendForExistingAccount() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendInstitutionInviteEmail(
                "user@test.es", "ACME Corp", "Premium", false, null, "http://front");

        verify(mailSender).send(mimeMessage);
    }

    // ================================================================
    // sendBookingConfirmationEmail
    // ================================================================

    @Test
    void sendBookingConfirmationEmailShouldSendToBothParties() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendBookingConfirmationEmail(
                "alumno@t.es",
                "Alumno",
                "tutor@t.es",
                "Tutor",
                LocalDate.of(2025, 6, 15),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                "Online",
                BigDecimal.valueOf(25));

        verify(mailSender, times(2)).send(mimeMessage);
    }

    // ================================================================
    // sendBookingCancellationEmail
    // ================================================================

    @Test
    void sendBookingCancellationEmailShouldIncludeMotivo() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendBookingCancellationEmail(
                "alumno@t.es",
                "Alumno",
                "Tutor",
                LocalDate.of(2025, 6, 15),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                "Enfermedad");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendBookingCancellationEmailShouldHandleNullMotivo() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendBookingCancellationEmail(
                "alumno@t.es",
                "Alumno",
                "Tutor",
                LocalDate.of(2025, 6, 15),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                null);

        verify(mailSender).send(mimeMessage);
    }

    // ================================================================
    // sendAlumnoCancelledConfirmationEmail
    // ================================================================

    @Test
    void sendAlumnoCancelledConfirmationEmailShouldSend() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendAlumnoCancelledConfirmationEmail(
                "alumno@t.es",
                "Alumno",
                "Tutor",
                LocalDate.of(2025, 6, 15),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                "Motivo personal");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendAlumnoCancelledConfirmationEmailShouldHandleBlankMotivo() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendAlumnoCancelledConfirmationEmail(
                "alumno@t.es",
                "Alumno",
                "Tutor",
                LocalDate.of(2025, 6, 15),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                "  ");

        verify(mailSender).send(mimeMessage);
    }

    // ================================================================
    // sendTutorNotificationAlumnoCancelledEmail
    // ================================================================

    @Test
    void sendTutorNotificationAlumnoCancelledEmailShouldSend() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendTutorNotificationAlumnoCancelledEmail(
                "tutor@t.es",
                "Tutor",
                "Alumno",
                LocalDate.of(2025, 6, 15),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                "No puedo asistir");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendTutorNotificationAlumnoCancelledEmailShouldHandleNullMotivo() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendTutorNotificationAlumnoCancelledEmail(
                "tutor@t.es",
                "Tutor",
                "Alumno",
                LocalDate.of(2025, 6, 15),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                null);

        verify(mailSender).send(mimeMessage);
    }

    // ================================================================
    // sendBookingRescheduledEmail
    // ================================================================

    @Test
    void sendBookingRescheduledEmailShouldSend() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendBookingRescheduledEmail(
                "alumno@t.es",
                "Alumno",
                "Tutor",
                LocalDate.of(2025, 6, 15),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                LocalDate.of(2025, 6, 17),
                LocalTime.of(12, 0),
                LocalTime.of(13, 0));

        verify(mailSender).send(mimeMessage);
    }

    // ================================================================
    // sendBookingReminderEmail
    // ================================================================

    @Test
    void sendBookingReminderEmailShouldSend() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendBookingReminderEmail(
                "alumno@t.es",
                "Alumno",
                "Tutor",
                LocalDate.of(2025, 6, 15),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                "Presencial");

        verify(mailSender).send(mimeMessage);
    }

    // ================================================================
    // sendBookingExpiredEmail
    // ================================================================

    @Test
    void sendBookingExpiredEmailShouldSend() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendBookingExpiredEmail(
                "alumno@t.es",
                "Alumno",
                "Tutor",
                LocalDate.of(2025, 6, 15),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0));

        verify(mailSender).send(mimeMessage);
    }

    // ================================================================
    // sendEventUpdatedEmail
    // ================================================================

    @Test
    void sendEventUpdatedEmailShouldSendWhenValid() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        Usuario usuario = buildUsuario(1L);
        Evento evento = new Evento();
        evento.setId(1L);
        evento.setTitulo("Mi Clase");
        evento.setEsVirtual(false);

        emailService.sendEventUpdatedEmail(usuario, evento);

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendEventUpdatedEmailShouldReturnEarlyWhenUsuarioNull() {
        emailService.sendEventUpdatedEmail(null, new Evento());

        verify(mailSender, never()).createMimeMessage();
    }

    @Test
    void sendEventUpdatedEmailShouldReturnEarlyWhenEmailNull() {
        Usuario usuario = Usuario.builder().id(1L).nombre("X").password("p").build();
        emailService.sendEventUpdatedEmail(usuario, new Evento());

        verify(mailSender, never()).createMimeMessage();
    }

    @Test
    void sendEventUpdatedEmailShouldReturnEarlyWhenEventoNull() {
        emailService.sendEventUpdatedEmail(buildUsuario(1L), null);

        verify(mailSender, never()).createMimeMessage();
    }

    @Test
    void sendEventUpdatedEmailShouldHandleVirtualEvent() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        Usuario usuario = buildUsuario(1L);
        Evento evento = new Evento();
        evento.setId(1L);
        evento.setTitulo("Online Class");
        evento.setEsVirtual(true);
        evento.setComunidad(Comunidad.builder().id(1L).nombre("MiCom").build());

        emailService.sendEventUpdatedEmail(usuario, evento);

        verify(mailSender).send(mimeMessage);
    }

    // ================================================================
    // sendCommunityAccessRequestEmail
    // ================================================================

    @Test
    void sendCommunityAccessRequestEmailShouldSend() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        Usuario dueno = buildUsuario(1L);
        Comunidad comunidad = Comunidad.builder().id(1L).nombre("MiCom").build();
        Usuario solicitante = buildUsuario(2L);

        emailService.sendCommunityAccessRequestEmail(
                dueno, comunidad, solicitante, "Quiero unirme");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendCommunityAccessRequestEmailShouldHandleNullMensaje() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        Usuario dueno = buildUsuario(1L);
        Comunidad comunidad = Comunidad.builder().id(1L).nombre("MiCom").build();
        Usuario solicitante = buildUsuario(2L);

        emailService.sendCommunityAccessRequestEmail(dueno, comunidad, solicitante, null);

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendCommunityAccessRequestEmailShouldReturnEarlyWhenDuenoNull() {
        emailService.sendCommunityAccessRequestEmail(
                null, Comunidad.builder().id(1L).nombre("C").build(), buildUsuario(2L), "msg");

        verify(mailSender, never()).createMimeMessage();
    }

    @Test
    void sendCommunityAccessRequestEmailShouldReturnEarlyWhenComunidadNull() {
        emailService.sendCommunityAccessRequestEmail(
                buildUsuario(1L), null, buildUsuario(2L), "msg");

        verify(mailSender, never()).createMimeMessage();
    }

    // ================================================================
    // sendCommunityMessageEmail
    // ================================================================

    @Test
    void sendCommunityMessageEmailShouldSend() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        Usuario dest = buildUsuario(1L);
        Comunidad comunidad = Comunidad.builder().id(1L).nombre("MiCom").build();
        Usuario remitente = buildUsuario(2L);

        emailService.sendCommunityMessageEmail(dest, comunidad, remitente, "Hello!");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendCommunityMessageEmailShouldTruncateLongContent() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        Usuario dest = buildUsuario(1L);
        Comunidad comunidad = Comunidad.builder().id(1L).nombre("MiCom").build();
        Usuario remitente = buildUsuario(2L);
        String longContent = "A".repeat(300);

        emailService.sendCommunityMessageEmail(dest, comunidad, remitente, longContent);

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendCommunityMessageEmailShouldReturnEarlyWhenDestinatarioNull() {
        emailService.sendCommunityMessageEmail(
                null, Comunidad.builder().id(1L).nombre("C").build(), buildUsuario(2L), "msg");

        verify(mailSender, never()).createMimeMessage();
    }

    @Test
    void sendCommunityMessageEmailShouldReturnEarlyWhenBlankEmail() {
        Usuario dest = Usuario.builder().id(1L).nombre("X").email("  ").password("p").build();
        emailService.sendCommunityMessageEmail(
                dest, Comunidad.builder().id(1L).nombre("C").build(), buildUsuario(2L), "msg");

        verify(mailSender, never()).createMimeMessage();
    }

    @Test
    void sendCommunityMessageEmailShouldHandleNullContent() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        Usuario dest = buildUsuario(1L);
        Comunidad comunidad = Comunidad.builder().id(1L).nombre("MiCom").build();
        Usuario remitente = buildUsuario(2L);

        emailService.sendCommunityMessageEmail(dest, comunidad, remitente, null);

        verify(mailSender).send(mimeMessage);
    }

    // ================================================================
    // sendCommunityMentionEmail
    // ================================================================

    @Test
    void sendCommunityMentionEmailShouldSend() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        Usuario dest = buildUsuario(1L);
        Comunidad comunidad = Comunidad.builder().id(1L).nombre("MiCom").build();
        Usuario remitente = buildUsuario(2L);

        emailService.sendCommunityMentionEmail(dest, comunidad, remitente, "Hey @you!");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendCommunityMentionEmailShouldReturnEarlyWhenDestinatarioNull() {
        emailService.sendCommunityMentionEmail(
                null, Comunidad.builder().id(1L).nombre("C").build(), buildUsuario(2L), "msg");

        verify(mailSender, never()).createMimeMessage();
    }

    @Test
    void sendCommunityMentionEmailShouldReturnEarlyWhenRemitenteNull() {
        emailService.sendCommunityMentionEmail(
                buildUsuario(1L), Comunidad.builder().id(1L).nombre("C").build(), null, "msg");

        verify(mailSender, never()).createMimeMessage();
    }

    // ================================================================
    // sendCommunityAnnouncementEmail
    // ================================================================

    @Test
    void sendCommunityAnnouncementEmailShouldSend() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        Usuario dest = buildUsuario(1L);
        Comunidad comunidad = Comunidad.builder().id(1L).nombre("MiCom").build();
        Usuario autor = buildUsuario(2L);
        Anuncio anuncio = new Anuncio();
        anuncio.setTitulo("Aviso importante");
        anuncio.setContenido("Contenido del anuncio");

        emailService.sendCommunityAnnouncementEmail(dest, comunidad, autor, anuncio);

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendCommunityAnnouncementEmailShouldReturnEarlyWhenDestinatarioNull() {
        emailService.sendCommunityAnnouncementEmail(
                null,
                Comunidad.builder().id(1L).nombre("C").build(),
                buildUsuario(2L),
                new Anuncio());

        verify(mailSender, never()).createMimeMessage();
    }

    @Test
    void sendCommunityAnnouncementEmailShouldReturnEarlyWhenAnuncioNull() {
        emailService.sendCommunityAnnouncementEmail(
                buildUsuario(1L),
                Comunidad.builder().id(1L).nombre("C").build(),
                buildUsuario(2L),
                null);

        verify(mailSender, never()).createMimeMessage();
    }

    @Test
    void sendCommunityAnnouncementEmailShouldHandleNullAutor() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        Usuario dest = buildUsuario(1L);
        Comunidad comunidad = Comunidad.builder().id(1L).nombre("MiCom").build();
        Anuncio anuncio = new Anuncio();
        anuncio.setTitulo("Aviso");
        anuncio.setContenido("Contenido");

        emailService.sendCommunityAnnouncementEmail(dest, comunidad, null, anuncio);

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendCommunityAnnouncementEmailShouldTruncateLongContent() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        Usuario dest = buildUsuario(1L);
        Comunidad comunidad = Comunidad.builder().id(1L).nombre("MiCom").build();
        Anuncio anuncio = new Anuncio();
        anuncio.setTitulo("Aviso");
        anuncio.setContenido("X".repeat(300));

        emailService.sendCommunityAnnouncementEmail(dest, comunidad, buildUsuario(2L), anuncio);

        verify(mailSender).send(mimeMessage);
    }

    // ================================================================
    // enviarRecordatorio
    // ================================================================

    @Test
    void enviarRecordatorioShouldSendMimeEmail() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(asistenciaEventoRepository.findByEventoIdAndEstado(anyLong(), any()))
                .thenReturn(Collections.emptyList());

        Evento evento = new Evento();
        evento.setId(1L);
        evento.setTitulo("Reunión");
        evento.setTipoEvento(TipoEvento.REUNION);
        evento.setFechaHora(LocalDateTime.now().plusDays(1));
        evento.setFechaFin(LocalDateTime.now().plusDays(1).plusHours(1));
        evento.setEsVirtual(false);

        Usuario usuario = buildUsuario(1L);

        emailService.enviarRecordatorio(usuario, evento, TipoRecordatorio.HORAS_24);

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void enviarRecordatorioShouldHandleVirtualEvent() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(asistenciaEventoRepository.findByEventoIdAndEstado(anyLong(), any()))
                .thenReturn(Collections.emptyList());

        Evento evento = new Evento();
        evento.setId(2L);
        evento.setTitulo("Clase Online");
        evento.setTipoEvento(TipoEvento.CLASE);
        evento.setFechaHora(LocalDateTime.now().plusHours(1));
        evento.setEsVirtual(true);

        emailService.enviarRecordatorio(buildUsuario(1L), evento, TipoRecordatorio.HORA_1);

        verify(mailSender).send(mimeMessage);
    }

    // ================================================================
    // enviarRecordatorioAlarma
    // ================================================================

    @Test
    void enviarRecordatorioAlarmaShouldSendMimeEmail() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(asistenciaEventoRepository.findByEventoIdAndEstado(anyLong(), any()))
                .thenReturn(Collections.emptyList());

        Evento evento = new Evento();
        evento.setId(3L);
        evento.setTitulo("Examen Final");
        evento.setTipoEvento(TipoEvento.EXAMEN);
        evento.setFechaHora(LocalDateTime.now().plusDays(2));
        evento.setFechaFin(LocalDateTime.now().plusDays(2).plusHours(2));
        evento.setEsVirtual(false);
        evento.setDescripcion("Examen importante");
        evento.setQueLlevar("Calculadora");

        emailService.enviarRecordatorioAlarma(buildUsuario(1L), evento, "Mi alarma personalizada");

        verify(mailSender).send(mimeMessage);
    }

    // ================================================================
    // sendEventUpdatedEmail — event with ubicacion
    // ================================================================

    @Test
    void sendEventUpdatedEmailShouldHandleEventWithUbicacion() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        Evento evento = new Evento();
        evento.setId(4L);
        evento.setTitulo("Tutoría Presencial");
        evento.setTipoEvento(TipoEvento.TUTORIA);
        evento.setFechaHora(LocalDateTime.now().plusDays(1));
        evento.setFechaFin(LocalDateTime.now().plusDays(1).plusHours(1));
        evento.setEsVirtual(false);
        es.us.meerkat.backend.entity.maps.Ubicacion ubicacion =
                new es.us.meerkat.backend.entity.maps.Ubicacion();
        ubicacion.setNombre("Aula 101");
        ubicacion.setDireccion("Edificio A");
        evento.setUbicacion(ubicacion);
        Comunidad comunidad = Comunidad.builder().id(1L).nombre("MiCom").build();
        evento.setComunidad(comunidad);

        Usuario usuario = buildUsuario(1L);
        emailService.sendEventUpdatedEmail(usuario, evento);

        verify(mailSender).send(mimeMessage);
    }
}
