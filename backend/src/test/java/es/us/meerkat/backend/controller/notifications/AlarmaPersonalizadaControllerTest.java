package es.us.meerkat.backend.controller.notifications;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import es.us.meerkat.backend.controller.notifications.AlarmaPersonalizadaController;
import es.us.meerkat.backend.service.events.AlarmaPersonalizadaService;

@ExtendWith(MockitoExtension.class)
class AlarmaPersonalizadaControllerTest {

    @Mock private AlarmaPersonalizadaService alarmaService;

    @InjectMocks private AlarmaPersonalizadaController controller;

    @Test
    void listarAlarmasDeEventoShouldThrowUnauthorizedWhenUserIsNull() {
        ResponseStatusException ex =
                assertThrows(
                        ResponseStatusException.class,
                        () -> controller.listarAlarmasDeEvento(1L, null));

        org.assertj.core.api.Assertions.assertThat(ex.getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
