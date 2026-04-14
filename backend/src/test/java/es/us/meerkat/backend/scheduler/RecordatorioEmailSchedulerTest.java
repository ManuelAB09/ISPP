package es.us.meerkat.backend.scheduler;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import es.us.meerkat.backend.entity.notifications.TipoRecordatorio;
import es.us.meerkat.backend.service.emails.RecordatorioEmailService;

@ExtendWith(MockitoExtension.class)
class RecordatorioEmailSchedulerTest {

    @Mock private RecordatorioEmailService recordatorioEmailService;

    @InjectMocks private RecordatorioEmailScheduler scheduler;

    // ============= recordatorios24h =============

    @Test
    void recordatorios24h_success_shouldCallService() {
        scheduler.recordatorios24h();
        verify(recordatorioEmailService).procesarRecordatorios(TipoRecordatorio.HORAS_24);
    }

    @Test
    void recordatorios24h_serviceThrows_shouldNotPropagate() {
        doThrow(new RuntimeException("DB error"))
                .when(recordatorioEmailService)
                .procesarRecordatorios(TipoRecordatorio.HORAS_24);
        assertThatCode(() -> scheduler.recordatorios24h()).doesNotThrowAnyException();
    }

    // ============= recordatorios1h =============

    @Test
    void recordatorios1h_success_shouldCallService() {
        scheduler.recordatorios1h();
        verify(recordatorioEmailService).procesarRecordatorios(TipoRecordatorio.HORA_1);
    }

    @Test
    void recordatorios1h_serviceThrows_shouldNotPropagate() {
        doThrow(new RuntimeException("DB error"))
                .when(recordatorioEmailService)
                .procesarRecordatorios(TipoRecordatorio.HORA_1);
        assertThatCode(() -> scheduler.recordatorios1h()).doesNotThrowAnyException();
    }

    // ============= recordatorios30min =============

    @Test
    void recordatorios30min_success_shouldCallService() {
        scheduler.recordatorios30min();
        verify(recordatorioEmailService).procesarRecordatorios(TipoRecordatorio.MINUTOS_30);
    }

    @Test
    void recordatorios30min_serviceThrows_shouldNotPropagate() {
        doThrow(new RuntimeException("DB error"))
                .when(recordatorioEmailService)
                .procesarRecordatorios(TipoRecordatorio.MINUTOS_30);
        assertThatCode(() -> scheduler.recordatorios30min()).doesNotThrowAnyException();
    }

    // ============= limpiezaDiaria =============

    @Test
    void limpiezaDiaria_success_shouldCallService() {
        scheduler.limpiezaDiaria();
        verify(recordatorioEmailService).limpiarRecordatoriosAntiguos();
    }

    @Test
    void limpiezaDiaria_serviceThrows_shouldNotPropagate() {
        doThrow(new RuntimeException("DB error"))
                .when(recordatorioEmailService)
                .limpiarRecordatoriosAntiguos();
        assertThatCode(() -> scheduler.limpiezaDiaria()).doesNotThrowAnyException();
    }
}
