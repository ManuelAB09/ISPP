package es.us.meerkat.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import es.us.meerkat.backend.controller.chats.MensajeComunidadController;
import es.us.meerkat.backend.dto.chats.EnviarMensajeComunidadRequest;
import es.us.meerkat.backend.dto.chats.MensajeComunidadResponse;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.communities.MiembroComunidadRepository;
import es.us.meerkat.backend.repository.users.UsuarioRepository;
import es.us.meerkat.backend.service.chats.ChatFileStorageService;
import es.us.meerkat.backend.service.chats.MensajeComunidadService;

@ExtendWith(MockitoExtension.class)
class MensajeComunidadControllerTest {

    @Mock private MensajeComunidadService mensajeComunidadService;
    @Mock private ChatFileStorageService chatFileStorageService;
    @Mock private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;
    @Mock private MiembroComunidadRepository miembroComunidadRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @InjectMocks private MensajeComunidadController controller;

    @Test
    void enviarMensaje_ok() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        EnviarMensajeComunidadRequest req = new EnviarMensajeComunidadRequest();
        req.setContenido("Hola");
        MensajeComunidadResponse resp =
                MensajeComunidadResponse.builder().contenido("Hola").build();
        when(mensajeComunidadService.enviarMensaje(1L, req)).thenReturn(resp);

        ResponseEntity<?> r = controller.enviarMensaje(1L, usuario, req);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).isEqualTo(resp);
    }

    @Test
    void obtenerHistorial_ok() {
        when(mensajeComunidadService.obtenerHistorial(1L))
                .thenReturn(List.of(MensajeComunidadResponse.builder().build()));
        ResponseEntity<?> r = controller.obtenerHistorial(1L);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void enviarArchivo_ok() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        MockMultipartFile file =
                new MockMultipartFile("file", "test.txt", "text/plain", "contenido".getBytes());
        var vf =
                new ChatFileStorageService.ValidatedChatFile(
                        "contenido".getBytes(), "test.txt", "text/plain", 9L);
        when(chatFileStorageService.validateAndExtract(any())).thenReturn(vf);
        when(mensajeComunidadService.enviarArchivo(
                        anyLong(), anyLong(), any(), any(), any(), anyLong(), any()))
                .thenReturn(MensajeComunidadResponse.builder().build());

        ResponseEntity<?> r = controller.enviarArchivo(1L, usuario, file, "mensaje");
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
