package es.us.meerkat.backend.scheduler;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import es.us.meerkat.backend.service.events.AlarmaPersonalizadaService;

@ExtendWith(MockitoExtension.class)
class AlarmaSchedulerTest {

    @Mock private AlarmaPersonalizadaService alarmaPersonalizadaService;

    @InjectMocks private AlarmaScheduler scheduler;

    // ============= dispararAlarmas =============

    @Test
    void dispararAlarmas_success_shouldCallService() {
        scheduler.dispararAlarmas();
        verify(alarmaPersonalizadaService).dispararAlarmasPendientes();
    }

    @Test
    void dispararAlarmas_serviceThrows_shouldNotPropagate() {
        doThrow(new RuntimeException("error"))
                .when(alarmaPersonalizadaService)
                .dispararAlarmasPendientes();
        assertThatCode(() -> scheduler.dispararAlarmas()).doesNotThrowAnyException();
    }

    // ============= limpiezaDiaria =============

    @Test
    void limpiezaDiaria_success_shouldCallService() {
        scheduler.limpiezaDiaria();
        verify(alarmaPersonalizadaService).limpiarAlarmasAntiguas();
    }

    @Test
    void limpiezaDiaria_serviceThrows_shouldNotPropagate() {
        doThrow(new RuntimeException("error"))
                .when(alarmaPersonalizadaService)
                .limpiarAlarmasAntiguas();
        assertThatCode(() -> scheduler.limpiezaDiaria()).doesNotThrowAnyException();
    }
}
