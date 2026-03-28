package es.us.meerkat.backend.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import es.us.meerkat.backend.controller.notifications.NotificacionesController;
import es.us.meerkat.backend.service.notifications.PreferenciasNotificacionService;

@ExtendWith(MockitoExtension.class)
class NotificacionesControllerTest {

    @Mock private PreferenciasNotificacionService preferenciasService;

    @InjectMocks private NotificacionesController controller;

    @Test
    void obtenerPreferenciasShouldThrowUnauthorizedWhenUserIsNull() {
        ResponseStatusException ex =
                assertThrows(
                        ResponseStatusException.class, () -> controller.obtenerPreferencias(null));

        org.assertj.core.api.Assertions.assertThat(ex.getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void actualizarPreferenciasShouldThrowUnauthorizedWhenUserIsNull() {
        ResponseStatusException ex =
                assertThrows(
                        ResponseStatusException.class,
                        () -> controller.actualizarPreferencias(null, null));

        org.assertj.core.api.Assertions.assertThat(ex.getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
