package es.us.meerkat.backend.service.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import es.us.meerkat.backend.entity.events.Evento;
import es.us.meerkat.backend.repository.emails.RecordatorioEmailRepository;
import es.us.meerkat.backend.repository.events.AsistenciaEventoRepository;
import es.us.meerkat.backend.repository.events.EventoRepository;
import es.us.meerkat.backend.repository.google.GoogleCalendarEventoRepository;
import es.us.meerkat.backend.repository.notifications.AlarmaPersonalizadaRepository;
import es.us.meerkat.backend.repository.notifications.AlertaEventoRepository;
import es.us.meerkat.backend.repository.zoom.ZoomMeetingRepository;

@ExtendWith(MockitoExtension.class)
class EventoCleanupServiceTest {

    @Mock private EventoRepository eventoRepository;
    @Mock private AsistenciaEventoRepository asistenciaEventoRepository;
    @Mock private AlertaEventoRepository alertaEventoRepository;
    @Mock private AlarmaPersonalizadaRepository alarmaPersonalizadaRepository;
    @Mock private GoogleCalendarEventoRepository googleCalendarEventoRepository;
    @Mock private RecordatorioEmailRepository recordatorioEmailRepository;
    @Mock private ZoomMeetingRepository zoomMeetingRepository;

    @InjectMocks private EventoCleanupService eventoCleanupService;

    @Test
    void eliminarEventosPasadosShouldReturnZeroWhenNoExpiredEvents() {
        when(eventoRepository.findEventosPasadosParaEliminar(any(LocalDateTime.class)))
                .thenReturn(List.of());

        int result = eventoCleanupService.eliminarEventosPasados();

        assertThat(result).isZero();
        verify(eventoRepository, never()).delete(any());
    }

    @Test
    void eliminarEventosPasadosShouldDeleteAllRelatedEntities() {
        Evento evento = new Evento();
        evento.setId(1L);

        when(eventoRepository.findEventosPasadosParaEliminar(any(LocalDateTime.class)))
                .thenReturn(List.of(evento));

        int result = eventoCleanupService.eliminarEventosPasados();

        assertThat(result).isEqualTo(1);
        verify(asistenciaEventoRepository).deleteByEventoId(1L);
        verify(alertaEventoRepository).deleteByEventoId(1L);
        verify(alarmaPersonalizadaRepository).deleteByEventoId(1L);
        verify(googleCalendarEventoRepository).deleteByEventoId(1L);
        verify(recordatorioEmailRepository).deleteByEventoId(1L);
        verify(zoomMeetingRepository).deleteByEventoId(1L);
        verify(eventoRepository).delete(evento);
    }

    @Test
    void eliminarEventosPasadosShouldDeleteMultipleEvents() {
        Evento evento1 = new Evento();
        evento1.setId(1L);
        Evento evento2 = new Evento();
        evento2.setId(2L);

        when(eventoRepository.findEventosPasadosParaEliminar(any(LocalDateTime.class)))
                .thenReturn(List.of(evento1, evento2));

        int result = eventoCleanupService.eliminarEventosPasados();

        assertThat(result).isEqualTo(2);
        verify(asistenciaEventoRepository).deleteByEventoId(1L);
        verify(asistenciaEventoRepository).deleteByEventoId(2L);
        verify(eventoRepository).delete(evento1);
        verify(eventoRepository).delete(evento2);
    }
}
