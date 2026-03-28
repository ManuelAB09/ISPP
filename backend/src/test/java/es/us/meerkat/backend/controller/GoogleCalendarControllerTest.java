package es.us.meerkat.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import es.us.meerkat.backend.service.google.GoogleCalendarService;

@ExtendWith(MockitoExtension.class)
class GoogleCalendarControllerTest {

    @Mock private GoogleCalendarService googleCalendarService;

    @InjectMocks private GoogleCalendarController controller;

    @Test
    void oauthCallbackShouldRedirectWithDeniedErrorWhenGoogleReturnsError() {
        ResponseEntity<Void> response = controller.oauthCallback(null, null, "access_denied");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation())
                .hasToString("http://localhost:3000/settings/calendar?error=acceso_denegado");
    }

    @Test
    void disconnectShouldThrowUnauthorizedWhenUserIsNull() {
        ResponseStatusException ex =
                assertThrows(ResponseStatusException.class, () -> controller.disconnect(null));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
