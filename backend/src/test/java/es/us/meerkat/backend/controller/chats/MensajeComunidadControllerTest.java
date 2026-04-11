package es.us.meerkat.backend.controller.chats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
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
import org.springframework.mock.web.MockMultipartFile;

import es.us.meerkat.backend.dto.chats.EnviarMensajeComunidadRequest;
import es.us.meerkat.backend.dto.chats.MensajeComunidadResponse;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.communities.MiembroComunidadRepository;
import es.us.meerkat.backend.repository.users.UsuarioRepository;
import es.us.meerkat.backend.service.chats.ChatFileStorageService;
import es.us.meerkat.backend.service.chats.MensajeComunidadService;
import es.us.meerkat.backend.service.communities.AuthorizationService;

@ExtendWith(MockitoExtension.class)
class MensajeComunidadControllerTest {

    @Mock private MensajeComunidadService mensajeComunidadService;
    @Mock private ChatFileStorageService chatFileStorageService;
    @Mock private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;
    @Mock private MiembroComunidadRepository miembroComunidadRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AuthorizationService authorizationService;
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

    @Test
    void enviarMensajeShouldReturnOkWhenSuccessful() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        EnviarMensajeComunidadRequest req = new EnviarMensajeComunidadRequest();
        req.setContenido("Hola comunidad");
        MensajeComunidadResponse resp =
                MensajeComunidadResponse.builder().contenido("Hola comunidad").id(1L).build();
        when(mensajeComunidadService.enviarMensaje(anyLong(), any())).thenReturn(resp);

        ResponseEntity<?> r = controller.enviarMensaje(1L, usuario, req);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).isEqualTo(resp);
    }

    @Test
    void enviarMensajeShouldReturnBadRequestOnError() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        EnviarMensajeComunidadRequest req = new EnviarMensajeComunidadRequest();
        req.setContenido("Hola comunidad");
        when(mensajeComunidadService.enviarMensaje(anyLong(), any()))
                .thenThrow(new RuntimeException("Error al enviar"));

        ResponseEntity<?> r = controller.enviarMensaje(1L, usuario, req);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void obtenerHistorialShouldReturnOkWithMessages() {
        MensajeComunidadResponse msg = MensajeComunidadResponse.builder().contenido("Hola").build();
        when(mensajeComunidadService.obtenerHistorial(1L)).thenReturn(List.of(msg));

        ResponseEntity<?> r = controller.obtenerHistorial(1L);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List<?>) r.getBody()).hasSize(1);
    }

    @Test
    void obtenerHistorialShouldReturnOkWithEmptyList() {
        when(mensajeComunidadService.obtenerHistorial(1L)).thenReturn(List.of());

        ResponseEntity<?> r = controller.obtenerHistorial(1L);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List<?>) r.getBody()).isEmpty();
    }

    @Test
    void obtenerHistorialShouldReturnServerErrorOnException() {
        when(mensajeComunidadService.obtenerHistorial(1L))
                .thenThrow(new RuntimeException("Database error"));

        ResponseEntity<?> r = controller.obtenerHistorial(1L);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void editarMensajeShouldReturnOkOnSuccess() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        EnviarMensajeComunidadRequest req = new EnviarMensajeComunidadRequest();
        req.setContenido("Editado");
        MensajeComunidadResponse resp =
                MensajeComunidadResponse.builder().contenido("Editado").build();
        when(mensajeComunidadService.editarMensaje(1L, 1L, "Editado")).thenReturn(resp);

        ResponseEntity<?> r = controller.editarMensaje(1L, 1L, usuario, req);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).isEqualTo(resp);
    }

    @Test
    void editarMensajeShouldReturnForbiddenOnError() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        EnviarMensajeComunidadRequest req = new EnviarMensajeComunidadRequest();
        req.setContenido("Editado");
        when(mensajeComunidadService.editarMensaje(1L, 1L, "Editado"))
                .thenThrow(new RuntimeException("No autorizado"));

        ResponseEntity<?> r = controller.editarMensaje(1L, 1L, usuario, req);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void eliminarMensajeShouldReturnOkOnSuccess() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);

        ResponseEntity<?> r = controller.eliminarMensaje(1L, 1L, usuario);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void eliminarMensajeShouldReturnForbiddenOnError() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        doThrow(new RuntimeException("No autorizado"))
                .when(mensajeComunidadService)
                .eliminarMensaje(1L, 1L);

        ResponseEntity<?> r = controller.eliminarMensaje(1L, 1L, usuario);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void enviarArchivoShouldReturnUnauthorizedWhenUserIsNull() {
        MockMultipartFile file =
                new MockMultipartFile("file", "test.txt", "text/plain", "contenido".getBytes());

        ResponseEntity<?> r = controller.enviarArchivo(1L, null, file, "mensaje");

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void enviarArchivoShouldReturnBadRequestWhenFileIsInvalid() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        MockMultipartFile file =
                new MockMultipartFile("file", "test.txt", "text/plain", "contenido".getBytes());
        doThrow(new IllegalArgumentException("Archivo inválido"))
                .when(chatFileStorageService)
                .validateAndExtract(any());

        ResponseEntity<?> r = controller.enviarArchivo(1L, usuario, file, "mensaje");

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void marcarComunidadComoLeidaShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> r = controller.marcarComunidadComoLeida(1L, null);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void marcarComunidadComoLeidaShouldReturnForbiddenWhenNotMember() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        when(authorizationService.isMemberOf(1L, 1L)).thenReturn(false);

        ResponseEntity<?> r = controller.marcarComunidadComoLeida(1L, usuario);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void marcarComunidadComoLeidaShouldReturnOkWhenMember() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        when(authorizationService.isMemberOf(1L, 1L)).thenReturn(true);

        ResponseEntity<?> r = controller.marcarComunidadComoLeida(1L, usuario);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void marcarComunidadComoLeidaShouldReturnServerErrorOnException() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        when(authorizationService.isMemberOf(1L, 1L)).thenReturn(true);
        doThrow(new RuntimeException("Database error"))
                .when(mensajeComunidadService)
                .marcarComunidadComoLeida(1L, 1L);

        ResponseEntity<?> r = controller.marcarComunidadComoLeida(1L, usuario);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @SuppressWarnings("unchecked")
    @Test
    void obtenerNoLeidosPorComunidadShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<Map<Long, Integer>> r =
                (ResponseEntity<Map<Long, Integer>>)
                        (ResponseEntity<?>) controller.obtenerNoLeidosPorComunidad(null);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @SuppressWarnings("unchecked")
    @Test
    void obtenerNoLeidosPorComunidadShouldReturnOkWithMap() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        Map<Long, Integer> map = Map.of(1L, 5, 2L, 3);
        when(mensajeComunidadService.obtenerNoLeidosPorComunidad(1L)).thenReturn(map);
        when(authorizationService.isMemberOf(1L, 1L)).thenReturn(true);
        when(authorizationService.isMemberOf(1L, 2L)).thenReturn(true);

        ResponseEntity<Map<Long, Integer>> r =
                (ResponseEntity<Map<Long, Integer>>)
                        (ResponseEntity<?>) controller.obtenerNoLeidosPorComunidad(usuario);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).isNotEmpty();
    }

    @Test
    void enviarMensajeShouldHandleMultipleMessagesInSequence() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        EnviarMensajeComunidadRequest req1 = new EnviarMensajeComunidadRequest();
        req1.setContenido("Mensaje 1");
        EnviarMensajeComunidadRequest req2 = new EnviarMensajeComunidadRequest();
        req2.setContenido("Mensaje 2");
        MensajeComunidadResponse resp1 =
                MensajeComunidadResponse.builder().contenido("Mensaje 1").id(1L).build();
        MensajeComunidadResponse resp2 =
                MensajeComunidadResponse.builder().contenido("Mensaje 2").id(2L).build();
        when(mensajeComunidadService.enviarMensaje(anyLong(), any()))
                .thenReturn(resp1)
                .thenReturn(resp2);

        ResponseEntity<?> r1 = controller.enviarMensaje(1L, usuario, req1);
        ResponseEntity<?> r2 = controller.enviarMensaje(1L, usuario, req2);

        assertThat(r1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r2.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
