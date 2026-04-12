package es.us.meerkat.backend.controller.chats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.chats.MensajeComunidadRepository;
import es.us.meerkat.backend.service.chats.MensajeComunidadLeidoService;

@ExtendWith(MockitoExtension.class)
class MensajeComunidadLeidoControllerTest {

    @Mock private MensajeComunidadLeidoService mensajeComunidadLeidoService;
    @Mock private MensajeComunidadRepository mensajeComunidadRepository;

    @InjectMocks private MensajeComunidadLeidoController controller;

    @Test
    void marcarComoLeidoShouldReturnNotFoundWhenMessageDoesNotExist() {
        when(mensajeComunidadRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.marcarComoLeido(1L, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void obtenerLeidosShouldReturnBadRequestWhenMensajeIdsIsMissing() {
        ResponseEntity<java.util.List<Long>> response = controller.obtenerLeidos(Map.of(), null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void obtenerLeidosShouldReturnOkWhenMensajeIdsIsPresent() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        when(mensajeComunidadLeidoService.obtenerIdsMensajesLeidos(1L, List.of(10L, 11L)))
                .thenReturn(List.of(10L));

        ResponseEntity<java.util.List<Long>> response =
                controller.obtenerLeidos(Map.of("mensajeIds", List.of(10L, 11L)), usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(10L);
    }

    @Test
    void obtenerLeidosShouldReturnEmptyListWhenNoMessagesRead() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        when(mensajeComunidadLeidoService.obtenerIdsMensajesLeidos(1L, List.of(10L, 11L)))
                .thenReturn(List.of());

        ResponseEntity<java.util.List<Long>> response =
                controller.obtenerLeidos(Map.of("mensajeIds", List.of(10L, 11L)), usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void obtenerLeidosShouldReturnAllWhenAllMessagesRead() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        java.util.List<Long> ids = List.of(10L, 11L, 12L);
        when(mensajeComunidadLeidoService.obtenerIdsMensajesLeidos(1L, ids)).thenReturn(ids);

        ResponseEntity<java.util.List<Long>> response =
                controller.obtenerLeidos(Map.of("mensajeIds", ids), usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactlyElementsOf(ids);
    }

    @Test
    void obtenerLeidosShouldHandleEmptyMessageIdsList() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        when(mensajeComunidadLeidoService.obtenerIdsMensajesLeidos(1L, List.of()))
                .thenReturn(List.of());

        ResponseEntity<java.util.List<Long>> response =
                controller.obtenerLeidos(Map.of("mensajeIds", List.of()), usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }
}
