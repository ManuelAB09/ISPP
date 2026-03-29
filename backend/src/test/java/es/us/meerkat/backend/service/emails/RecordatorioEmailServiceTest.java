package es.us.meerkat.backend.service.emails;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import es.us.meerkat.backend.entity.communities.Comunidad;
import es.us.meerkat.backend.entity.emails.RecordatorioEmail;
import es.us.meerkat.backend.entity.events.Evento;
import es.us.meerkat.backend.entity.notifications.PreferenciasNotificacion;
import es.us.meerkat.backend.entity.notifications.TipoRecordatorio;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.emails.RecordatorioEmailRepository;
import es.us.meerkat.backend.repository.events.EventoRepository;
import es.us.meerkat.backend.repository.users.UsuarioRepository;
import es.us.meerkat.backend.service.notifications.PreferenciasNotificacionService;

@ExtendWith(MockitoExtension.class)
class RecordatorioEmailServiceTest {

    @Mock private EventoRepository eventoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private RecordatorioEmailRepository recordatorioRepository;
    @Mock private PreferenciasNotificacionService preferenciasService;
    @Mock private EmailService emailService;

    @InjectMocks private RecordatorioEmailService recordatorioEmailService;

    // ================================================================
    // procesarRecordatorios
    // ================================================================

    @Test
    void procesarRecordatoriosShouldSendEmailForEligibleUsers() throws Exception {
        Comunidad comunidad = Comunidad.builder().id(10L).nombre("C").build();
        Evento evento = new Evento();
        evento.setId(1L);
        evento.setComunidad(comunidad);

        Usuario usuario =
                Usuario.builder().id(5L).nombre("U").email("u@t.com").password("p").build();

        PreferenciasNotificacion prefs = new PreferenciasNotificacion();
        prefs.setEmailsActivados(true);
        prefs.setRecordatorio24h(true);

        when(eventoRepository.findEventosInRange(
                        any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(evento));
        when(usuarioRepository.findMiembrosByComunidadId(10L)).thenReturn(List.of(usuario));
        when(recordatorioRepository.findByEventoIdAndUsuarioIdAndTipo(
                        1L, 5L, TipoRecordatorio.HORAS_24))
                .thenReturn(Optional.empty());
        when(preferenciasService.getOrCreate(5L)).thenReturn(prefs);

        recordatorioEmailService.procesarRecordatorios(TipoRecordatorio.HORAS_24);

        verify(emailService).enviarRecordatorio(usuario, evento, TipoRecordatorio.HORAS_24);
        verify(recordatorioRepository).save(any(RecordatorioEmail.class));
    }

    @Test
    void procesarRecordatoriosShouldSkipWhenAlreadySent() throws Exception {
        Comunidad comunidad = Comunidad.builder().id(10L).nombre("C").build();
        Evento evento = new Evento();
        evento.setId(1L);
        evento.setComunidad(comunidad);

        Usuario usuario =
                Usuario.builder().id(5L).nombre("U").email("u@t.com").password("p").build();

        when(eventoRepository.findEventosInRange(
                        any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(evento));
        when(usuarioRepository.findMiembrosByComunidadId(10L)).thenReturn(List.of(usuario));
        when(recordatorioRepository.findByEventoIdAndUsuarioIdAndTipo(
                        1L, 5L, TipoRecordatorio.HORA_1))
                .thenReturn(Optional.of(new RecordatorioEmail()));

        recordatorioEmailService.procesarRecordatorios(TipoRecordatorio.HORA_1);

        verify(emailService, never()).enviarRecordatorio(any(), any(), any());
    }

    @Test
    void procesarRecordatoriosShouldSkipWhenUserDoesNotWantReminder() throws Exception {
        Comunidad comunidad = Comunidad.builder().id(10L).nombre("C").build();
        Evento evento = new Evento();
        evento.setId(1L);
        evento.setComunidad(comunidad);

        Usuario usuario =
                Usuario.builder().id(5L).nombre("U").email("u@t.com").password("p").build();

        PreferenciasNotificacion prefs = new PreferenciasNotificacion();
        prefs.setEmailsActivados(true);
        prefs.setRecordatorio30min(false);

        when(eventoRepository.findEventosInRange(
                        any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(evento));
        when(usuarioRepository.findMiembrosByComunidadId(10L)).thenReturn(List.of(usuario));
        when(recordatorioRepository.findByEventoIdAndUsuarioIdAndTipo(
                        1L, 5L, TipoRecordatorio.MINUTOS_30))
                .thenReturn(Optional.empty());
        when(preferenciasService.getOrCreate(5L)).thenReturn(prefs);

        recordatorioEmailService.procesarRecordatorios(TipoRecordatorio.MINUTOS_30);

        verify(emailService, never()).enviarRecordatorio(any(), any(), any());
    }

    @Test
    void procesarRecordatoriosShouldSkipEventWithNullComunidad() {
        Evento evento = new Evento();
        evento.setId(1L);
        evento.setComunidad(null);

        when(eventoRepository.findEventosInRange(
                        any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(evento));

        recordatorioEmailService.procesarRecordatorios(TipoRecordatorio.HORAS_24);

        verify(usuarioRepository, never()).findMiembrosByComunidadId(any());
    }

    @Test
    void procesarRecordatoriosShouldHandleEmailSendingFailure() throws Exception {
        Comunidad comunidad = Comunidad.builder().id(10L).nombre("C").build();
        Evento evento = new Evento();
        evento.setId(1L);
        evento.setComunidad(comunidad);

        Usuario usuario =
                Usuario.builder().id(5L).nombre("U").email("u@t.com").password("p").build();

        PreferenciasNotificacion prefs = new PreferenciasNotificacion();
        prefs.setEmailsActivados(true);
        prefs.setRecordatorio24h(true);

        when(eventoRepository.findEventosInRange(
                        any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(evento));
        when(usuarioRepository.findMiembrosByComunidadId(10L)).thenReturn(List.of(usuario));
        when(recordatorioRepository.findByEventoIdAndUsuarioIdAndTipo(
                        1L, 5L, TipoRecordatorio.HORAS_24))
                .thenReturn(Optional.empty());
        when(preferenciasService.getOrCreate(5L)).thenReturn(prefs);
        doThrow(new RuntimeException("SMTP error"))
                .when(emailService)
                .enviarRecordatorio(any(), any(), any());

        recordatorioEmailService.procesarRecordatorios(TipoRecordatorio.HORAS_24);

        verify(recordatorioRepository).save(any(RecordatorioEmail.class));
    }

    @Test
    void procesarRecordatoriosShouldDoNothingWhenNoEventsInRange() throws Exception {
        when(eventoRepository.findEventosInRange(
                        any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());

        recordatorioEmailService.procesarRecordatorios(TipoRecordatorio.HORAS_24);

        verify(usuarioRepository, never()).findMiembrosByComunidadId(any());
        verify(emailService, never()).enviarRecordatorio(any(), any(), any());
    }

    // ================================================================
    // limpiarRecordatoriosAntiguos
    // ================================================================

    @Test
    void limpiarRecordatoriosAntiguosShouldCallDeleteWithCorrectDate() {
        recordatorioEmailService.limpiarRecordatoriosAntiguos();

        verify(recordatorioRepository).deleteOldRecordatorios(any(LocalDateTime.class));
    }
}
