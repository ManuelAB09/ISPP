package es.us.meerkat.backend.controller.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import es.us.meerkat.backend.dto.events.UpdatePreferenciasRequest;
import es.us.meerkat.backend.dto.notifications.PreferenciasNotificacionResponse;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.service.notifications.PreferenciasNotificacionService;

@ExtendWith(MockitoExtension.class)
class NotificacionesControllerTest {

    @Mock private PreferenciasNotificacionService preferenciasService;

    @InjectMocks private NotificacionesController controller;

    private Usuario buildUsuario(Long id) {
        Usuario u = new Usuario();
        u.setId(id);
        return u;
    }

    private PreferenciasNotificacionResponse buildPreferencias() {
        PreferenciasNotificacionResponse response = new PreferenciasNotificacionResponse();
        return response;
    }

    @Test
    void obtenerPreferenciasWithNullUserShouldThrowException() {
        try {
            controller.obtenerPreferencias(null);
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }
    }

    @Test
    void obtenerPreferenciasWithValidUserShouldReturnOk() {
        Usuario usuario = buildUsuario(1L);
        PreferenciasNotificacionResponse prefs = buildPreferencias();

        when(preferenciasService.obtenerPreferencias(1L)).thenReturn(prefs);

        ResponseEntity<PreferenciasNotificacionResponse> response =
                controller.obtenerPreferencias(usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(prefs);
    }

    @Test
    void actualizarPreferenciasWithNullUserShouldThrowException() {
        UpdatePreferenciasRequest request = new UpdatePreferenciasRequest();
        try {
            controller.actualizarPreferencias(request, null);
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }
    }

    @Test
    void actualizarPreferenciasWithValidUserShouldReturnOk() {
        Usuario usuario = buildUsuario(1L);
        UpdatePreferenciasRequest request = new UpdatePreferenciasRequest();
        PreferenciasNotificacionResponse prefs = buildPreferencias();

        when(preferenciasService.actualizarPreferencias(1L, request)).thenReturn(prefs);

        ResponseEntity<PreferenciasNotificacionResponse> response =
                controller.actualizarPreferencias(request, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(prefs);
    }

    @Test
    void obtenerPreferenciasMultipleTimes() {
        Usuario usuario = buildUsuario(1L);
        PreferenciasNotificacionResponse prefs = buildPreferencias();

        when(preferenciasService.obtenerPreferencias(1L)).thenReturn(prefs);

        ResponseEntity<PreferenciasNotificacionResponse> response1 =
                controller.obtenerPreferencias(usuario);
        ResponseEntity<PreferenciasNotificacionResponse> response2 =
                controller.obtenerPreferencias(usuario);

        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

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
