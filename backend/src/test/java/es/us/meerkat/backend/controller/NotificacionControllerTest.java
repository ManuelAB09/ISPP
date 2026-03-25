package es.us.meerkat.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import es.us.meerkat.backend.service.NotificacionService;

@ExtendWith(MockitoExtension.class)
class NotificacionControllerTest {

    @Mock private NotificacionService notificacionService;

    @InjectMocks private NotificacionController controller;

    @Test
    void getMyNotificationsShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> response = controller.getMyNotifications(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void markAsReadShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<Void> response = controller.markAsRead(1L, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
