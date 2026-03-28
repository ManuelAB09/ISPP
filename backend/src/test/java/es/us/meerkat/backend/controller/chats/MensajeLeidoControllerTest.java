package es.us.meerkat.backend.controller.chats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.chats.MensajeRepository;
import es.us.meerkat.backend.service.chats.MensajeLeidoService;

@ExtendWith(MockitoExtension.class)
class MensajeLeidoControllerTest {

    @Mock private MensajeLeidoService mensajeLeidoService;
    @Mock private MensajeRepository mensajeRepository;

    @InjectMocks private MensajeLeidoController controller;

    @Test
    void obtenerLeidosShouldReturnBadRequestWhenMensajeIdsIsMissing() {
        ResponseEntity<java.util.List<Long>> response = controller.obtenerLeidos(Map.of(), null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void obtenerLeidosShouldReturnOkWhenMensajeIdsIsPresent() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        when(mensajeLeidoService.obtenerIdsMensajesLeidos(1L, List.of(10L, 11L)))
                .thenReturn(List.of(10L));

        ResponseEntity<java.util.List<Long>> response =
                controller.obtenerLeidos(Map.of("mensajeIds", List.of(10L, 11L)), usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(10L);
    }
}
