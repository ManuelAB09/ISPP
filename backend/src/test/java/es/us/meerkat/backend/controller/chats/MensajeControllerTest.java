package es.us.meerkat.backend.controller.chats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mock.web.MockMultipartFile;

import es.us.meerkat.backend.dto.chats.EnviarMensajeRequest;
import es.us.meerkat.backend.dto.chats.MensajeResponse;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.users.UsuarioRepository;
import es.us.meerkat.backend.service.chats.ChatFileStorageService;
import es.us.meerkat.backend.service.chats.MensajeService;

@ExtendWith(MockitoExtension.class)
class MensajeControllerTest {

    @Mock private MensajeService mensajeService;
    @Mock private ChatFileStorageService chatFileStorageService;
    @Mock private SimpMessagingTemplate broker;
    @Mock private UsuarioRepository usuarioRepository;
    @InjectMocks private MensajeController controller;

    @Test
    void enviarMensaje_ok() {
        Usuario u = new Usuario();
        u.setId(1L);
        EnviarMensajeRequest req = new EnviarMensajeRequest();
        req.setUserId(2L);
        req.setContenido("Hola");
        MensajeResponse resp = MensajeResponse.builder().contenido("Hola").build();
        when(mensajeService.enviarMensaje(1L, req)).thenReturn(resp);

        ResponseEntity<?> r = controller.enviarMensaje(u, req);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).isEqualTo(resp);
    }

    @Test
    void obtenerConversacionConUsuario_ok() {
        Usuario u = new Usuario();
        u.setId(1L);
        Long other = 2L;
        MensajeResponse m = MensajeResponse.builder().contenido("Mensaje test").build();
        when(mensajeService.obtenerConversacionConUsuario(1L, other)).thenReturn(List.of(m));

        ResponseEntity<?> r = controller.obtenerConversacionConUsuario(u, other);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((List<?>) r.getBody()).get(0)).isEqualTo(m);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void obtenerConversaciones_ok() {
        Usuario u = new Usuario();
        u.setId(1L);
        List conv = List.of(java.util.Map.of("ultimoMensaje", "Hola"));
        when(mensajeService.obtenerConversaciones(1L)).thenReturn(conv);

        ResponseEntity<?> r = controller.obtenerConversaciones(u);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).isEqualTo(conv);
    }

    @Test
    void eliminarMensaje_ok() {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setEmail("test@test.com");
        MensajeResponse deleted =
                MensajeResponse.builder().emisorId(1L).receptorId(2L).contenido("msg").build();
        when(mensajeService.obtenerMensaje(10L)).thenReturn(deleted);
        ResponseEntity<?> r = controller.eliminarMensaje(u, 10L);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void editarMensaje_ok() {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setEmail("test@test.com");
        MensajeResponse resp =
                MensajeResponse.builder().contenido("Editado").emisorId(1L).receptorId(2L).build();
        when(mensajeService.editarMensaje(1L, 5L, "Editado")).thenReturn(resp);

        ResponseEntity<?> r =
                controller.editarMensaje(
                        u,
                        5L,
                        new EnviarMensajeRequest() {
                            {
                                setContenido("Editado");
                            }
                        });

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).isEqualTo(resp);
    }

    @Test
    void enviarArchivo_ok() {
        Usuario u = new Usuario();
        u.setId(1L);
        MockMultipartFile file =
                new MockMultipartFile("file", "test.txt", "text/plain", "contenido".getBytes());
        var vf =
                new ChatFileStorageService.ValidatedChatFile(
                        "contenido".getBytes(), "test.txt", "text/plain", 8L);
        when(chatFileStorageService.validateAndExtract(file)).thenReturn(vf);
        MensajeResponse resp = MensajeResponse.builder().archivoNombre("test.txt").build();
        when(mensajeService.enviarArchivo(
                        1L,
                        2L,
                        null,
                        null,
                        vf.originalName(),
                        vf.mimeType(),
                        vf.sizeBytes(),
                        vf.content()))
                .thenReturn(resp);

        ResponseEntity<?> r = controller.enviarArchivo(u, file, null, 2L, null);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).isEqualTo(resp);
    }

    @Test
    void descargarArchivo_ok() {
        Usuario u = new Usuario();
        u.setId(1L);
        var archivo = new MensajeService.MensajeArchivo("data".getBytes(), "doc.txt", "text/plain");
        when(mensajeService.obtenerArchivo(1L, 3L)).thenReturn(archivo);

        ResponseEntity<?> r = controller.descargarArchivo(u, 3L);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getHeaders().getFirst("Content-Disposition")).contains("doc.txt");
    }

    @Test
    void enviarMensajeShouldReturnUnauthorizedWhenUserIsNull() {
        EnviarMensajeRequest req = new EnviarMensajeRequest();
        req.setContenido("Hola");

        ResponseEntity<?> r = controller.enviarMensaje(null, req);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void enviarMensajeShouldReturnBadRequestWhenServiceThrowsIllegalArgument() {
        Usuario u = new Usuario();
        u.setId(1L);
        EnviarMensajeRequest req = new EnviarMensajeRequest();
        req.setContenido("Hola");
        when(mensajeService.enviarMensaje(1L, req))
                .thenThrow(new IllegalArgumentException("Usuario destino no existe"));

        ResponseEntity<?> r = controller.enviarMensaje(u, req);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getBody()).asString().contains("Usuario destino no existe");
    }

    @Test
    void enviarMensajeShouldReturnServerErrorWhenServiceFails() {
        Usuario u = new Usuario();
        u.setId(1L);
        EnviarMensajeRequest req = new EnviarMensajeRequest();
        req.setContenido("Hola");
        when(mensajeService.enviarMensaje(1L, req))
                .thenThrow(new RuntimeException("Database error"));

        ResponseEntity<?> r = controller.enviarMensaje(u, req);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void obtenerConversacionConUsuarioShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> r = controller.obtenerConversacionConUsuario(null, 2L);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void obtenerConversacionConUsuarioShouldReturnBadRequestOnError() {
        Usuario u = new Usuario();
        u.setId(1L);
        when(mensajeService.obtenerConversacionConUsuario(1L, 2L))
                .thenThrow(new IllegalArgumentException("Usuario no válido"));

        ResponseEntity<?> r = controller.obtenerConversacionConUsuario(u, 2L);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void obtenerConversacionConUsuarioShouldReturnEmptyList() {
        Usuario u = new Usuario();
        u.setId(1L);
        when(mensajeService.obtenerConversacionConUsuario(1L, 2L)).thenReturn(List.of());

        ResponseEntity<?> r = controller.obtenerConversacionConUsuario(u, 2L);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List<?>) r.getBody()).isEmpty();
    }

    @Test
    void obtenerConversacionesShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> r = controller.obtenerConversaciones(null);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void obtenerConversacionesShouldReturnEmptyWhenNoConversations() {
        Usuario u = new Usuario();
        u.setId(1L);
        when(mensajeService.obtenerConversaciones(1L)).thenReturn(List.of());

        ResponseEntity<?> r = controller.obtenerConversaciones(u);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List<?>) r.getBody()).isEmpty();
    }

    @Test
    void obtenerConversacionesShouldHandleServiceException() {
        Usuario u = new Usuario();
        u.setId(1L);
        when(mensajeService.obtenerConversaciones(1L))
                .thenThrow(new RuntimeException("Database error"));

        ResponseEntity<?> r = controller.obtenerConversaciones(u);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void eliminarMensajeShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> r = controller.eliminarMensaje(null, 10L);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void eliminarMensajeShouldReturnBadRequestWhenServiceThrows() {
        Usuario u = new Usuario();
        u.setId(1L);
        when(mensajeService.obtenerMensaje(10L))
                .thenThrow(new IllegalArgumentException("Mensaje no encontrado"));

        ResponseEntity<?> r = controller.eliminarMensaje(u, 10L);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void editarMensajeShouldReturnUnauthorizedWhenUserIsNull() {
        EnviarMensajeRequest req = new EnviarMensajeRequest();
        req.setContenido("Editado");

        ResponseEntity<?> r = controller.editarMensaje(null, 5L, req);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void editarMensajeShouldReturnBadRequestOnError() {
        Usuario u = new Usuario();
        u.setId(1L);
        EnviarMensajeRequest req = new EnviarMensajeRequest();
        req.setContenido("Editado");
        when(mensajeService.editarMensaje(1L, 5L, "Editado"))
                .thenThrow(new IllegalArgumentException("Mensaje no encontrado"));

        ResponseEntity<?> r = controller.editarMensaje(u, 5L, req);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void enviarArchivoShouldReturnUnauthorizedWhenUserIsNull() {
        MockMultipartFile file =
                new MockMultipartFile("file", "test.txt", "text/plain", "contenido".getBytes());

        ResponseEntity<?> r = controller.enviarArchivo(null, file, null, 2L, null);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void enviarArchivoShouldReturnBadRequestWhenFileIsInvalid() {
        Usuario u = new Usuario();
        u.setId(1L);
        MockMultipartFile file =
                new MockMultipartFile("file", "test.txt", "text/plain", "contenido".getBytes());
        doThrow(new IllegalArgumentException("Archivo inválido"))
                .when(chatFileStorageService)
                .validateAndExtract(any());

        ResponseEntity<?> r = controller.enviarArchivo(u, file, null, 2L, null);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void descargarArchivoShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> r = controller.descargarArchivo(null, 3L);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void descargarArchivoShouldReturnBadRequestWhenFileNotFound() {
        Usuario u = new Usuario();
        u.setId(1L);
        when(mensajeService.obtenerArchivo(1L, 3L))
                .thenThrow(new IllegalArgumentException("Archivo no encontrado"));

        ResponseEntity<?> r = controller.descargarArchivo(u, 3L);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void marcarConversacionComoLeidaShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> r = controller.marcarConversacionComoLeida(null, 2L);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void marcarConversacionComoLeidaShouldReturnOkOnSuccess() {
        Usuario u = new Usuario();
        u.setId(1L);

        ResponseEntity<?> r = controller.marcarConversacionComoLeida(u, 2L);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void marcarConversacionComoLeidaShouldReturnServerErrorOnException() {
        Usuario u = new Usuario();
        u.setId(1L);
        doThrow(new RuntimeException("Database error"))
                .when(mensajeService)
                .marcarConversacionComoLeida(1L, 2L);

        ResponseEntity<?> r = controller.marcarConversacionComoLeida(u, 2L);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void obtenerConversacionShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> r = controller.obtenerConversacion(null, 2L);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
