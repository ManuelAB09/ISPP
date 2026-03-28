package es.us.meerkat.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import es.us.meerkat.backend.controller.zoom.ZoomController;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.service.communities.AuthorizationService;
import es.us.meerkat.backend.service.zoom.ZoomIntegrationService;

@ExtendWith(MockitoExtension.class)
class ZoomControllerTest {

    @Mock private ZoomIntegrationService zoomIntegrationService;
    @Mock private AuthorizationService authorizationService;

    @InjectMocks private ZoomController controller;

    private Usuario buildUsuario(Long id) {
        Usuario u = new Usuario();
        u.setId(id);
        return u;
    }

    @Test
    void getMyActiveCallsShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> response = controller.getMyActiveCalls(null);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getMyActiveCallsShouldReturnOk() {
        Usuario usuario = buildUsuario(1L);
        when(zoomIntegrationService.getActiveCallsForUser(1L)).thenReturn(java.util.List.of());

        ResponseEntity<?> response = controller.getMyActiveCalls(usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void zoomWebhookShouldReturnOkOnSuccess() {
        Map<String, Object> payload = Map.of("event", "meeting.started");
        when(zoomIntegrationService.processWebhook(payload, "sig", "ts"))
                .thenReturn(Map.of("status", "ok"));

        ResponseEntity<Map<String, Object>> response = controller.zoomWebhook(payload, "sig", "ts");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "ok");
    }

    @Test
    void zoomWebhookShouldReturnUnauthorizedOnError() {
        Map<String, Object> payload = Map.of("event", "meeting.started");
        when(zoomIntegrationService.processWebhook(payload, "badsig", "ts"))
                .thenThrow(new RuntimeException("Invalid signature"));

        ResponseEntity<Map<String, Object>> response =
                controller.zoomWebhook(payload, "badsig", "ts");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
