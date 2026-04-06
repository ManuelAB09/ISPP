package es.us.meerkat.backend.controller.zoom;

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
    void createOrGetMeetingShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> response = controller.createOrGetMeeting(1L, null, null);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getActiveMeetingShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> response = controller.getActiveMeeting(1L, null);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void joinMeetingShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> response = controller.joinMeeting(1L, null);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void listParticipantsShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> response = controller.listParticipants(1L, null);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void listMeetingsShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> response = controller.listMeetings(1L, null);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void listRecordingsShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> response = controller.listRecordings(1L, null);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void endMeetingShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> response = controller.endMeeting(1L, null);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
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
