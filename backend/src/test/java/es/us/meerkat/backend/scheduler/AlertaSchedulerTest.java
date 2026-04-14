package es.us.meerkat.backend.scheduler;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import es.us.meerkat.backend.entity.notifications.TipoAlerta;
import es.us.meerkat.backend.service.events.MisEventosService;

@ExtendWith(MockitoExtension.class)
class AlertaSchedulerTest {

    @Mock private MisEventosService misEventosService;

    @InjectMocks private AlertaScheduler scheduler;

    // ============= generarAlertasInminentes =============

    @Test
    void generarAlertasInminentes_success_shouldCallService() {
        scheduler.generarAlertasInminentes();
        verify(misEventosService)
                .generarAlertasParaRango(
                        any(LocalDateTime.class),
                        any(LocalDateTime.class),
                        eq(TipoAlerta.INMINENTE_15MIN));
    }

    @Test
    void generarAlertasInminentes_serviceThrows_shouldNotPropagate() {
        doThrow(new RuntimeException("error"))
                .when(misEventosService)
                .generarAlertasParaRango(any(), any(), eq(TipoAlerta.INMINENTE_15MIN));
        assertThatCode(() -> scheduler.generarAlertasInminentes()).doesNotThrowAnyException();
    }

    // ============= generarAlertas24H =============

    @Test
    void generarAlertas24H_success_shouldCallService() {
        scheduler.generarAlertas24H();
        verify(misEventosService)
                .generarAlertasParaRango(
                        any(LocalDateTime.class),
                        any(LocalDateTime.class),
                        eq(TipoAlerta.PROXIMA_24H));
    }

    @Test
    void generarAlertas24H_serviceThrows_shouldNotPropagate() {
        doThrow(new RuntimeException("error"))
                .when(misEventosService)
                .generarAlertasParaRango(any(), any(), eq(TipoAlerta.PROXIMA_24H));
        assertThatCode(() -> scheduler.generarAlertas24H()).doesNotThrowAnyException();
    }

    // ============= limpiarAlertasAntiguas =============

    @Test
    void limpiarAlertasAntiguas_success_shouldCallService() {
        scheduler.limpiarAlertasAntiguas();
        verify(misEventosService).limpiarAlertasAntiguas();
    }

    @Test
    void limpiarAlertasAntiguas_serviceThrows_shouldNotPropagate() {
        doThrow(new RuntimeException("error")).when(misEventosService).limpiarAlertasAntiguas();
        assertThatCode(() -> scheduler.limpiarAlertasAntiguas()).doesNotThrowAnyException();
    }
}
