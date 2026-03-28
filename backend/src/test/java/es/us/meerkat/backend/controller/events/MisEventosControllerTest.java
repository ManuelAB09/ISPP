package es.us.meerkat.backend.controller.events;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import es.us.meerkat.backend.controller.events.MisEventosController;
import es.us.meerkat.backend.service.events.AlarmaPersonalizadaService;
import es.us.meerkat.backend.service.events.MisEventosService;

@ExtendWith(MockitoExtension.class)
class MisEventosControllerTest {

    @Mock private MisEventosService misEventosService;
    @Mock private AlarmaPersonalizadaService alarmaPersonalizadaService;

    @InjectMocks private MisEventosController controller;

    @Test
    void contarAlertasNoLeidasShouldThrowUnauthorizedWhenUserIsNull() {
        ResponseStatusException ex =
                assertThrows(
                        ResponseStatusException.class,
                        () -> controller.contarAlertasNoLeidas(null));

        org.assertj.core.api.Assertions.assertThat(ex.getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void misAlarmasPendientesShouldThrowUnauthorizedWhenUserIsNull() {
        ResponseStatusException ex =
                assertThrows(
                        ResponseStatusException.class, () -> controller.misAlarmasPendientes(null));

        org.assertj.core.api.Assertions.assertThat(ex.getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
