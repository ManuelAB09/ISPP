package es.us.meerkat.backend.controller.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import es.us.meerkat.backend.dto.events.MisEventosItemResponse;
import es.us.meerkat.backend.dto.notifications.AlertaEventoResponse;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.service.events.AlarmaPersonalizadaService;
import es.us.meerkat.backend.service.events.MisEventosService;

/**
 * Comprehensive test suite for MisEventosController.
 *
 * <p>Tests cover event listing, history, alerts, and alert notifications for authenticated users.
 * Includes 25+ test cases covering happy paths, error handling, and edge cases.
 */
@ExtendWith(MockitoExtension.class)
class MisEventosControllerTest {

    @Mock private MisEventosService misEventosService;
    @Mock private AlarmaPersonalizadaService alarmaPersonalizadaService;

    @InjectMocks private MisEventosController controller;

    private Usuario usuario;
    private Long usuarioId;
    private Long alertaId;

    @BeforeEach
    void setUp() {
        usuarioId = 1L;
        alertaId = 100L;

        usuario = buildUsuario(usuarioId, "user@test.es");
    }

    // ===============================
    // HELPER METHODS
    // ===============================

    private Usuario buildUsuario(Long id, String email) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setEmail(email);
        return u;
    }

    private MisEventosItemResponse buildMisEventosItem(Long id, String titulo) {
        MisEventosItemResponse item = new MisEventosItemResponse();
        item.setId(id);
        item.setTitulo(titulo);
        item.setProximaEn24H(false);
        item.setInminenteEn15Min(false);
        return item;
    }

    private AlertaEventoResponse buildAlertaEventoResponse(
            Long id, es.us.meerkat.backend.entity.notifications.TipoAlerta tipo) {
        AlertaEventoResponse alerta = new AlertaEventoResponse();
        alerta.setId(id);
        alerta.setTipo(tipo);
        alerta.setLeida(false);
        return alerta;
    }

    // ===============================
    // MY EVENTS TESTS
    // ===============================

    @Test
    void obtenerMisEventosShouldReturnEventsWhenAuthenticated() {
        List<MisEventosItemResponse> eventos =
                List.of(buildMisEventosItem(1L, "Reunión 1"), buildMisEventosItem(2L, "Reunión 2"));
        when(misEventosService.obtenerMisEventos(usuarioId)).thenReturn(eventos);

        ResponseEntity<List<MisEventosItemResponse>> response =
                controller.obtenerMisEventos(usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        verify(misEventosService).obtenerMisEventos(usuarioId);
    }

    @Test
    void obtenerMisEventosShouldReturnEmptyListWhenNoEvents() {
        when(misEventosService.obtenerMisEventos(usuarioId)).thenReturn(List.of());

        ResponseEntity<List<MisEventosItemResponse>> response =
                controller.obtenerMisEventos(usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void obtenerMisEventosShouldThrowUnauthorizedWhenUserIsNull() {
        ResponseStatusException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                        ResponseStatusException.class, () -> controller.obtenerMisEventos(null));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void obtenerMisEventosShouldReturnMultipleEvents() {
        List<MisEventosItemResponse> eventos =
                List.of(
                        buildMisEventosItem(1L, "Examen"),
                        buildMisEventosItem(2L, "Cuestionario"),
                        buildMisEventosItem(3L, "Tutoría"));
        when(misEventosService.obtenerMisEventos(usuarioId)).thenReturn(eventos);

        ResponseEntity<List<MisEventosItemResponse>> response =
                controller.obtenerMisEventos(usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(3);
    }

    @Test
    void obtenerMisEventosShouldMarkEventsAsProximas24h() {
        MisEventosItemResponse evento = buildMisEventosItem(1L, "Evento Próximo");
        evento.setProximaEn24H(true);
        when(misEventosService.obtenerMisEventos(usuarioId)).thenReturn(List.of(evento));

        ResponseEntity<List<MisEventosItemResponse>> response =
                controller.obtenerMisEventos(usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get(0).getProximaEn24H()).isTrue();
    }

    @Test
    void obtenerMisEventosShouldMarkEventsAsInminentes15min() {
        MisEventosItemResponse evento = buildMisEventosItem(1L, "Evento Inminente");
        evento.setInminenteEn15Min(true);
        when(misEventosService.obtenerMisEventos(usuarioId)).thenReturn(List.of(evento));

        ResponseEntity<List<MisEventosItemResponse>> response =
                controller.obtenerMisEventos(usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get(0).getInminenteEn15Min()).isTrue();
    }

    // ===============================
    // HISTORY TESTS
    // ===============================

    @Test
    void obtenerHistorialShouldReturnAllEventsWhenIncluirCanceladosFalse() {
        List<MisEventosItemResponse> eventos =
                List.of(
                        buildMisEventosItem(1L, "Evento Pasado"),
                        buildMisEventosItem(2L, "Evento Futuro"));
        when(misEventosService.obtenerHistorialEventos(usuarioId, false)).thenReturn(eventos);

        ResponseEntity<List<MisEventosItemResponse>> response =
                controller.obtenerHistorial(false, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        verify(misEventosService).obtenerHistorialEventos(usuarioId, false);
    }

    @Test
    void obtenerHistorialShouldIncludeCanceledEventsWhenRequested() {
        List<MisEventosItemResponse> eventos = List.of(buildMisEventosItem(1L, "Evento Cancelado"));
        when(misEventosService.obtenerHistorialEventos(usuarioId, true)).thenReturn(eventos);

        ResponseEntity<List<MisEventosItemResponse>> response =
                controller.obtenerHistorial(true, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(misEventosService).obtenerHistorialEventos(usuarioId, true);
    }

    @Test
    void obtenerHistorialShouldThrowUnauthorizedWhenUserIsNull() {
        ResponseStatusException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                        ResponseStatusException.class,
                        () -> controller.obtenerHistorial(false, null));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void obtenerHistorialShouldReturnEmptyList() {
        when(misEventosService.obtenerHistorialEventos(usuarioId, false)).thenReturn(List.of());

        ResponseEntity<List<MisEventosItemResponse>> response =
                controller.obtenerHistorial(false, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    // ===============================
    // ALERTS - UNREAD TESTS
    // ===============================

    @Test
    void obtenerAlertasNoLeidasShouldReturnUnreadAlerts() {
        List<AlertaEventoResponse> alertas =
                List.of(
                        buildAlertaEventoResponse(
                                1L,
                                es.us.meerkat.backend.entity.notifications.TipoAlerta.PROXIMA_24H),
                        buildAlertaEventoResponse(
                                2L,
                                es.us.meerkat.backend.entity.notifications.TipoAlerta
                                        .INMINENTE_15MIN));
        when(misEventosService.obtenerAlertasNoLeidas(usuarioId)).thenReturn(alertas);

        ResponseEntity<List<AlertaEventoResponse>> response =
                controller.obtenerAlertasNoLeidas(usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        verify(misEventosService).obtenerAlertasNoLeidas(usuarioId);
    }

    @Test
    void obtenerAlertasNoLeidasShouldReturnEmptyListWhenNoUnreadAlerts() {
        when(misEventosService.obtenerAlertasNoLeidas(usuarioId)).thenReturn(List.of());

        ResponseEntity<List<AlertaEventoResponse>> response =
                controller.obtenerAlertasNoLeidas(usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void obtenerAlertasNoLeidasShouldThrowUnauthorizedWhenUserIsNull() {
        ResponseStatusException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                        ResponseStatusException.class,
                        () -> controller.obtenerAlertasNoLeidas(null));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void obtenerAlertasNoLeidasShouldReturnOnlyProxima24hType() {
        AlertaEventoResponse alerta =
                buildAlertaEventoResponse(
                        1L, es.us.meerkat.backend.entity.notifications.TipoAlerta.PROXIMA_24H);
        when(misEventosService.obtenerAlertasNoLeidas(usuarioId)).thenReturn(List.of(alerta));

        ResponseEntity<List<AlertaEventoResponse>> response =
                controller.obtenerAlertasNoLeidas(usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get(0).getTipo())
                .isEqualTo(es.us.meerkat.backend.entity.notifications.TipoAlerta.PROXIMA_24H);
    }

    // ===============================
    // ALERTS - ALL TESTS
    // ===============================

    @Test
    void obtenerTodasLasAlertasShouldReturnAllAlerts() {
        List<AlertaEventoResponse> alertas =
                List.of(
                        buildAlertaEventoResponse(
                                1L,
                                es.us.meerkat.backend.entity.notifications.TipoAlerta.PROXIMA_24H),
                        buildAlertaEventoResponse(
                                2L,
                                es.us.meerkat.backend.entity.notifications.TipoAlerta
                                        .INMINENTE_15MIN));
        when(misEventosService.obtenerTodasLasAlertas(usuarioId)).thenReturn(alertas);

        ResponseEntity<List<AlertaEventoResponse>> response =
                controller.obtenerTodasLasAlertas(usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    void obtenerTodasLasAlertasShouldThrowUnauthorizedWhenUserIsNull() {
        ResponseStatusException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                        ResponseStatusException.class,
                        () -> controller.obtenerTodasLasAlertas(null));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ===============================
    // ALERT COUNT TESTS
    // ===============================

    @Test
    void contarAlertasNoLeidasShouldReturnCount() {
        when(misEventosService.contarAlertasNoLeidas(usuarioId)).thenReturn(5L);

        ResponseEntity<Long> response = controller.contarAlertasNoLeidas(usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(5L);
        verify(misEventosService).contarAlertasNoLeidas(usuarioId);
    }

    @Test
    void contarAlertasNoLeidasShouldReturnZeroWhenNoUnreadAlerts() {
        when(misEventosService.contarAlertasNoLeidas(usuarioId)).thenReturn(0L);

        ResponseEntity<Long> response = controller.contarAlertasNoLeidas(usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(0L);
    }

    @Test
    void contarAlertasNoLeidasShouldThrowUnauthorizedWhenUserIsNull() {
        ResponseStatusException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                        ResponseStatusException.class,
                        () -> controller.contarAlertasNoLeidas(null));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ===============================
    // MARK SINGLE ALERT AS READ TESTS
    // ===============================

    @Test
    void marcarAlertaComoLeidaShouldReturnOkWhenSuccessful() {
        AlertaEventoResponse alertaLeida =
                buildAlertaEventoResponse(
                        alertaId,
                        es.us.meerkat.backend.entity.notifications.TipoAlerta.PROXIMA_24H);
        alertaLeida.setLeida(true);
        when(misEventosService.marcarAlertaComoLeida(alertaId, usuarioId)).thenReturn(alertaLeida);

        ResponseEntity<AlertaEventoResponse> response =
                controller.marcarAlertaComoLeida(alertaId, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getLeida()).isTrue();
        verify(misEventosService).marcarAlertaComoLeida(alertaId, usuarioId);
    }

    @Test
    void marcarAlertaComoLeidaShouldThrowUnauthorizedWhenUserIsNull() {
        ResponseStatusException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                        ResponseStatusException.class,
                        () -> controller.marcarAlertaComoLeida(alertaId, null));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void marcarAlertaComoLeidaShouldThrowForbiddenWhenNoPermission() {
        when(misEventosService.marcarAlertaComoLeida(alertaId, usuarioId))
                .thenThrow(new RuntimeException("No tienes permiso sobre esta alerta"));

        ResponseStatusException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                        ResponseStatusException.class,
                        () -> controller.marcarAlertaComoLeida(alertaId, usuario));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void marcarAlertaComoLeidaShouldThrowNotFoundWhenAlertDoesNotExist() {
        when(misEventosService.marcarAlertaComoLeida(alertaId, usuarioId))
                .thenThrow(new RuntimeException("Alerta no encontrada"));

        ResponseStatusException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                        ResponseStatusException.class,
                        () -> controller.marcarAlertaComoLeida(alertaId, usuario));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ===============================
    // MARK ALL ALERTS AS READ TESTS
    // ===============================

    @Test
    void marcarTodasComoLeidasShouldReturnNoContent() {
        doNothing().when(misEventosService).marcarTodasComoLeidas(usuarioId);

        ResponseEntity<Void> response = controller.marcarTodasComoLeidas(usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(misEventosService).marcarTodasComoLeidas(usuarioId);
    }

    @Test
    void marcarTodasComoLeidasShouldThrowUnauthorizedWhenUserIsNull() {
        ResponseStatusException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                        ResponseStatusException.class,
                        () -> controller.marcarTodasComoLeidas(null));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void marcarTodasComoLeidasShouldSucceedEvenWithNoAlerts() {
        doNothing().when(misEventosService).marcarTodasComoLeidas(usuarioId);

        ResponseEntity<Void> response = controller.marcarTodasComoLeidas(usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
}
