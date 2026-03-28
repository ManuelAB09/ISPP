package es.us.meerkat.backend.controller.chats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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
}
