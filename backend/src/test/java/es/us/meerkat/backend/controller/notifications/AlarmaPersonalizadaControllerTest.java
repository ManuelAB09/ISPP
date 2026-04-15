package es.us.meerkat.backend.controller.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import es.us.meerkat.backend.dto.notifications.AlarmaPersonalizadaResponse;
import es.us.meerkat.backend.dto.notifications.CrearAlarmaRequest;
import es.us.meerkat.backend.dto.notifications.CrearAlarmasLoteRequest;
import es.us.meerkat.backend.entity.notifications.TipoCanal;
import es.us.meerkat.backend.entity.users.Usuario;
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

        @Test
        void crearAlarmaShouldReturnCreatedWhenServiceSucceeds() {
        Usuario usuario = buildUsuario(7L);
        CrearAlarmaRequest request = new CrearAlarmaRequest();
        request.setMinutosAntes(30);
        request.setCanal(TipoCanal.AMBOS);

        AlarmaPersonalizadaResponse responseDto =
            AlarmaPersonalizadaResponse.builder().id(1L).minutosAntes(30).build();
        when(alarmaService.crearAlarma(7L, 10L, request)).thenReturn(responseDto);

        ResponseEntity<AlarmaPersonalizadaResponse> response =
            controller.crearAlarma(10L, request, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(responseDto);
        }

        @Test
        void crearAlarmaShouldMapRuntimeExceptionToBadRequest() {
        Usuario usuario = buildUsuario(7L);
        CrearAlarmaRequest request = new CrearAlarmaRequest();
        request.setMinutosAntes(15);

        when(alarmaService.crearAlarma(7L, 10L, request))
            .thenThrow(new RuntimeException("Evento ya finalizado"));

        ResponseStatusException ex =
            assertThrows(
                ResponseStatusException.class,
                () -> controller.crearAlarma(10L, request, usuario));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getReason()).contains("Evento ya finalizado");
        }

        @Test
        void crearAlarmasLoteShouldReturnCreatedWhenServiceSucceeds() {
        Usuario usuario = buildUsuario(11L);
        CrearAlarmasLoteRequest request = new CrearAlarmasLoteRequest();
        request.setMinutosAntesList(List.of(30, 60, 120));
        request.setCanal(TipoCanal.EMAIL);

        List<AlarmaPersonalizadaResponse> created =
            List.of(
                AlarmaPersonalizadaResponse.builder().id(1L).minutosAntes(30).build(),
                AlarmaPersonalizadaResponse.builder().id(2L).minutosAntes(60).build());
        when(alarmaService.crearAlarmasLote(11L, 20L, request)).thenReturn(created);

        ResponseEntity<List<AlarmaPersonalizadaResponse>> response =
            controller.crearAlarmasLote(20L, request, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).hasSize(2);
        }

        @Test
        void listarAlarmasDeEventoShouldReturnAlarmListWhenAuthenticated() {
        Usuario usuario = buildUsuario(5L);
        when(alarmaService.listarAlarmasDeEvento(5L, 1L))
            .thenReturn(List.of(AlarmaPersonalizadaResponse.builder().id(99L).build()));

        ResponseEntity<List<AlarmaPersonalizadaResponse>> response =
            controller.listarAlarmasDeEvento(1L, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        }

        @Test
        void eliminarAlarmaShouldReturnNoContentWhenServiceSucceeds() {
        Usuario usuario = buildUsuario(3L);

        ResponseEntity<Void> response = controller.eliminarAlarma(20L, 30L, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(alarmaService).eliminarAlarma(30L, 3L);
        }

        @Test
        void eliminarAlarmaShouldReturnForbiddenWhenServiceDeniesPermission() {
        Usuario usuario = buildUsuario(3L);
        doThrow(new RuntimeException("sin permiso para eliminar alarma"))
            .when(alarmaService)
            .eliminarAlarma(40L, 3L);

        ResponseStatusException ex =
            assertThrows(
                ResponseStatusException.class,
                () -> controller.eliminarAlarma(20L, 40L, usuario));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        void eliminarAlarmaShouldReturnNotFoundForGenericRuntimeException() {
        Usuario usuario = buildUsuario(3L);
        doThrow(new RuntimeException("alarma no encontrada"))
            .when(alarmaService)
            .eliminarAlarma(41L, 3L);

        ResponseStatusException ex =
            assertThrows(
                ResponseStatusException.class,
                () -> controller.eliminarAlarma(20L, 41L, usuario));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        void eliminarTodasLasAlarmasShouldReturnNoContent() {
        Usuario usuario = buildUsuario(13L);

        ResponseEntity<Void> response = controller.eliminarTodasLasAlarmas(2L, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(alarmaService).eliminarAlarmasDeEvento(13L, 2L);
        }

        private Usuario buildUsuario(Long id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        return usuario;
        }
}
