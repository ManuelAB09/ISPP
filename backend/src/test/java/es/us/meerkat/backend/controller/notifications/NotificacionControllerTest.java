package es.us.meerkat.backend.controller.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import es.us.meerkat.backend.entity.notifications.Notificacion;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.service.notifications.NotificacionService;

@ExtendWith(MockitoExtension.class)
class NotificacionControllerTest {

    @Mock private NotificacionService notificacionService;

    @InjectMocks private NotificacionController controller;

    private Usuario buildUsuario(Long id) {
        Usuario u = new Usuario();
        u.setId(id);
        return u;
    }

    private Notificacion buildNotificacion(Long id) {
        Notificacion n = new Notificacion();
        n.setId(id);
        n.setLeida(false);
        return n;
    }

    @Test
    void getMyNotificationsShouldReturnOk() {
        Usuario usuario = buildUsuario(1L);
        Notificacion notif1 = buildNotificacion(1L);
        Notificacion notif2 = buildNotificacion(2L);

        when(notificacionService.obtenerNotificaciones(usuario))
                .thenReturn(List.of(notif1, notif2));

        ResponseEntity<List<Notificacion>> response = controller.getMyNotifications(usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    void getMyNotificationsShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<List<Notificacion>> response = controller.getMyNotifications(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getMyNotificationsShouldReturnEmptyList() {
        Usuario usuario = buildUsuario(1L);

        when(notificacionService.obtenerNotificaciones(usuario)).thenReturn(List.of());

        ResponseEntity<List<Notificacion>> response = controller.getMyNotifications(usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void markAsReadShouldReturnOkWhenSuccessful() {
        Usuario usuario = buildUsuario(1L);

        when(notificacionService.marcarComoLeida(1L, 1L)).thenReturn(true);

        ResponseEntity<Void> response = controller.markAsRead(1L, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void markAsReadShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<Void> response = controller.markAsRead(1L, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void markAsReadShouldReturnNotFoundWhenNotificationDoesNotExist() {
        Usuario usuario = buildUsuario(1L);

        when(notificacionService.marcarComoLeida(999L, 1L)).thenReturn(false);

        ResponseEntity<Void> response = controller.markAsRead(999L, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
