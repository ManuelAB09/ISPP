package es.us.meerkat.backend.controller.google;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import es.us.meerkat.backend.dto.google.GoogleCalendarStatusResponse;
import es.us.meerkat.backend.dto.google.UpdateCalendarPreferenciasRequest;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.service.google.GoogleCalendarService;

@ExtendWith(MockitoExtension.class)
class GoogleCalendarControllerTest {

    @Mock private GoogleCalendarService googleCalendarService;

    @InjectMocks private GoogleCalendarController controller;

    private Usuario usuario;
    private GoogleCalendarStatusResponse statusResponse;

    @BeforeEach
    void setUp() throws Exception {
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("user@test.es");

        statusResponse = new GoogleCalendarStatusResponse();
        statusResponse.setConectado(true);
    }

    // ============ GET AUTH URL TESTS ============
    @Test
    void getAuthUrlShouldReturnUrlWhenAuthenticated() throws Exception {
        String expectedUrl =
                "https://accounts.google.com/oauth/authorize?client_id=123&redirect_uri=...";
        when(googleCalendarService.generarUrlAutorizacion(1L)).thenReturn(expectedUrl);

        ResponseEntity<?> response = controller.getAuthUrl(usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedUrl);
    }

    @Test
    void getAuthUrlShouldReturnUnauthorizedWhenUserIsNull() {
        try {
            controller.getAuthUrl(null);
        } catch (ResponseStatusException e) {
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Test
    void getAuthUrlShouldReturnInternalServerErrorWhenServiceFails() throws Exception {
        when(googleCalendarService.generarUrlAutorizacion(1L))
                .thenThrow(new RuntimeException("Service error"));

        try {
            controller.getAuthUrl(usuario);
        } catch (ResponseStatusException e) {
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ============ OAUTH CALLBACK TESTS ============
    @Test
    void oauthCallbackShouldRedirectWithSuccessWhenCodeProvided() throws Exception {
        doNothing().when(googleCalendarService).procesarCallback("auth_code_123", 1L);

        ResponseEntity<Void> response = controller.oauthCallback("auth_code_123", "1", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation())
                .hasToString("http://localhost:3000/settings/calendar?connected=true");
    }

    @Test
    void oauthCallbackShouldRedirectWithDeniedErrorWhenGoogleReturnsError() {
        ResponseEntity<Void> response = controller.oauthCallback(null, null, "access_denied");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation())
                .hasToString("http://localhost:3000/settings/calendar?error=acceso_denegado");
    }

    @Test
    void oauthCallbackShouldRedirectWithDeniedErrorWhenCodeIsNull() {
        ResponseEntity<Void> response = controller.oauthCallback(null, "1", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation())
                .hasToString("http://localhost:3000/settings/calendar?error=acceso_denegado");
    }

    @Test
    void oauthCallbackShouldRedirectWithErrorWhenProcessingFails() throws Exception {
        doThrow(new RuntimeException("Token exchange failed"))
                .when(googleCalendarService)
                .procesarCallback("auth_code_123", 1L);

        ResponseEntity<Void> response = controller.oauthCallback("auth_code_123", "1", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation())
                .hasToString("http://localhost:3000/settings/calendar?error=error_interno");
    }

    @Test
    void oauthCallbackShouldHandleInvalidUserIdInState() {
        ResponseEntity<Void> response =
                controller.oauthCallback("auth_code_123", "invalid_id", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation())
                .hasToString("http://localhost:3000/settings/calendar?error=error_interno");
    }

    // ============ GET STATUS TESTS ============
    @Test
    void getStatusShouldReturnConnectedStatus() {
        when(googleCalendarService.obtenerEstado(1L)).thenReturn(statusResponse);

        ResponseEntity<GoogleCalendarStatusResponse> response = controller.getStatus(usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getConectado()).isTrue();
    }

    @Test
    void getStatusShouldReturnUnauthorizedWhenUserIsNull() {
        try {
            controller.getStatus(null);
        } catch (ResponseStatusException e) {
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Test
    void getStatusShouldReturnDisconnectedStatus() throws Exception {
        statusResponse.setConectado(false);
        when(googleCalendarService.obtenerEstado(1L)).thenReturn(statusResponse);

        ResponseEntity<GoogleCalendarStatusResponse> response = controller.getStatus(usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getConectado()).isFalse();
    }

    // ============ UPDATE PREFERENCES TESTS ============
    @Test
    void updatePreferencesShouldReturnUpdatedStatusOnSuccess() {
        UpdateCalendarPreferenciasRequest request = new UpdateCalendarPreferenciasRequest();
        request.setSincronizacionActiva(true);

        when(googleCalendarService.actualizarPreferencias(1L, request)).thenReturn(statusResponse);

        ResponseEntity<GoogleCalendarStatusResponse> response =
                controller.updatePreferences(request, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        verify(googleCalendarService).actualizarPreferencias(1L, request);
    }

    @Test
    void updatePreferencesShouldReturnUnauthorizedWhenUserIsNull() {
        UpdateCalendarPreferenciasRequest request = new UpdateCalendarPreferenciasRequest();
        assertThrows(
                ResponseStatusException.class, () -> controller.updatePreferences(request, null));
    }

    @Test
    void updatePreferencesShouldReturnBadRequestWhenNotConnected() {
        UpdateCalendarPreferenciasRequest request = new UpdateCalendarPreferenciasRequest();
        when(googleCalendarService.actualizarPreferencias(1L, request))
                .thenThrow(new RuntimeException("Google Calendar no conectado"));

        assertThrows(
                ResponseStatusException.class,
                () -> controller.updatePreferences(request, usuario));
    }

    // ============ DISCONNECT TESTS ============
    @Test
    void disconnectShouldReturnNoContentOnSuccess() {
        ResponseEntity<Void> response = controller.disconnect(usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(googleCalendarService).desconectar(1L);
    }

    @Test
    void disconnectShouldThrowUnauthorizedWhenUserIsNull() {
        ResponseStatusException ex =
                assertThrows(ResponseStatusException.class, () -> controller.disconnect(null));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void disconnectShouldHandleServiceError() {
        doThrow(new RuntimeException("Disconnection failed"))
                .when(googleCalendarService)
                .desconectar(1L);

        // The service exception should be allowed to propagate
        assertThrows(RuntimeException.class, () -> controller.disconnect(usuario));
    }

    // ============ ADDITIONAL TESTS ============
    @Test
    void getAuthUrlShouldReturnValidUrlFormat() throws Exception {
        String expectedUrl =
                "https://accounts.google.com/oauth/authorize?client_id=123&redirect_uri=http://localhost:3000";
        when(googleCalendarService.generarUrlAutorizacion(1L)).thenReturn(expectedUrl);

        ResponseEntity<?> response = controller.getAuthUrl(usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).asString().contains("accounts.google.com");
    }

    @Test
    void oauthCallbackShouldHandleNullStateParameter() {
        ResponseEntity<Void> response = controller.oauthCallback("auth_code_123", null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation()).isNotNull();
    }

    @Test
    void oauthCallbackShouldHandleInvalidStateFormat() {
        ResponseEntity<Void> response =
                controller.oauthCallback("auth_code_123", "malformed", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
    }

    @Test
    void getStatusShouldReturnStatusWithPreferences() {
        statusResponse.setConectado(true);
        when(googleCalendarService.obtenerEstado(1L)).thenReturn(statusResponse);

        ResponseEntity<GoogleCalendarStatusResponse> response = controller.getStatus(usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void updatePreferencesShouldActivateSynchronization() {
        UpdateCalendarPreferenciasRequest request = new UpdateCalendarPreferenciasRequest();
        request.setSincronizacionActiva(true);

        statusResponse.setConectado(true);
        when(googleCalendarService.actualizarPreferencias(1L, request)).thenReturn(statusResponse);

        ResponseEntity<GoogleCalendarStatusResponse> response =
                controller.updatePreferences(request, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(googleCalendarService).actualizarPreferencias(1L, request);
    }

    @Test
    void updatePreferencesShouldDeactivateSynchronization() {
        UpdateCalendarPreferenciasRequest request = new UpdateCalendarPreferenciasRequest();
        request.setSincronizacionActiva(false);

        statusResponse.setConectado(true);
        when(googleCalendarService.actualizarPreferencias(1L, request)).thenReturn(statusResponse);

        ResponseEntity<GoogleCalendarStatusResponse> response =
                controller.updatePreferences(request, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void disconnectShouldCallServiceWithCorrectUserId() {
        ResponseEntity<Void> response = controller.disconnect(usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(googleCalendarService).desconectar(1L);
    }

    @Test
    void getAuthUrlShouldGenerateDifferentUrlsForDifferentUsers() throws Exception {
        Usuario otherUser = new Usuario();
        otherUser.setId(2L);

        String url1 = "https://accounts.google.com/oauth/authorize?client_id=123&state=user1";
        String url2 = "https://accounts.google.com/oauth/authorize?client_id=123&state=user2";

        when(googleCalendarService.generarUrlAutorizacion(1L)).thenReturn(url1);
        when(googleCalendarService.generarUrlAutorizacion(2L)).thenReturn(url2);

        ResponseEntity<?> response1 = controller.getAuthUrl(usuario);
        ResponseEntity<?> response2 = controller.getAuthUrl(otherUser);

        assertThat(response1.getBody()).isNotEqualTo(response2.getBody());
    }

    @Test
    void oauthCallbackShouldProcessValidCode() throws Exception {
        doNothing().when(googleCalendarService).procesarCallback("valid_code", 1L);

        ResponseEntity<Void> response = controller.oauthCallback("valid_code", "1", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        verify(googleCalendarService).procesarCallback("valid_code", 1L);
    }
}
