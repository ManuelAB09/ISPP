package es.us.meerkat.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.us.meerkat.backend.dto.EnviarMensajeComunidadRequest;
import es.us.meerkat.backend.dto.MensajeComunidadResponse;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.service.MensajeComunidadService;
import lombok.RequiredArgsConstructor;

/** Controlador REST para gestionar mensajes de comunidades. */
@RestController
@RequestMapping("/api/v1/comunidades")
@RequiredArgsConstructor
public class MensajeComunidadController {

    private final MensajeComunidadService mensajeComunidadService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Envía un mensaje en el chat de una comunidad.
     *
     * @param comunidadId ID de la comunidad.
     * @param usuario usuario autenticado.
     * @param request datos del mensaje.
     * @return respuesta con el mensaje guardado.
     */
    @PostMapping("/{comunidadId}/mensajes")
    public ResponseEntity<?> enviarMensaje(
            @PathVariable final Long comunidadId,
            @AuthenticationPrincipal final Usuario usuario,
            @RequestBody final EnviarMensajeComunidadRequest request) {
        try {
            request.setComunidadId(comunidadId);
            final MensajeComunidadResponse response =
                    mensajeComunidadService.enviarMensaje(usuario.getId(), request);
            messagingTemplate.convertAndSend("/topic/community." + comunidadId, response);
            return ResponseEntity.ok(response);
        } catch (final Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error al enviar el mensaje: " + e.getMessage());
        }
    }

    /**
     * Obtiene el historial de mensajes de una comunidad.
     *
     * @param comunidadId ID de la comunidad.
     * @return lista de mensajes de la comunidad.
     */
    @GetMapping("/{comunidadId}/mensajes")
    public ResponseEntity<?> obtenerHistorial(@PathVariable final Long comunidadId) {
        try {
            final List<MensajeComunidadResponse> mensajes =
                    mensajeComunidadService.obtenerHistorial(comunidadId);
            return ResponseEntity.ok(mensajes);
        } catch (final Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al obtener el historial: " + e.getMessage());
        }
    }

    /**
     * Edita un mensaje de comunidad.
     *
     * @param comunidadId ID de la comunidad.
     * @param mensajeId ID del mensaje a editar.
     * @param usuario usuario autenticado.
     * @param request con el nuevo contenido.
     * @return respuesta con el mensaje actualizado.
     */
    @PutMapping("/{comunidadId}/mensajes/{mensajeId}")
    public ResponseEntity<?> editarMensaje(
            @PathVariable final Long comunidadId,
            @PathVariable final Long mensajeId,
            @AuthenticationPrincipal final Usuario usuario,
            @RequestBody final EnviarMensajeComunidadRequest request) {
        try {
            final MensajeComunidadResponse response =
                    mensajeComunidadService.editarMensaje(
                            usuario.getId(), mensajeId, request.getContenido());
            return ResponseEntity.ok(response);
        } catch (final Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Error al editar el mensaje: " + e.getMessage());
        }
    }

    /**
     * Elimina un mensaje de comunidad.
     *
     * @param comunidadId ID de la comunidad.
     * @param mensajeId ID del mensaje a eliminar.
     * @param usuario usuario autenticado.
     * @return respuesta de éxito o error.
     */
    @DeleteMapping("/{comunidadId}/mensajes/{mensajeId}")
    public ResponseEntity<?> eliminarMensaje(
            @PathVariable final Long comunidadId,
            @PathVariable final Long mensajeId,
            @AuthenticationPrincipal final Usuario usuario) {
        try {
            mensajeComunidadService.eliminarMensaje(usuario.getId(), mensajeId);
            return ResponseEntity.ok("Mensaje eliminado exitosamente");
        } catch (final Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Error al eliminar el mensaje: " + e.getMessage());
        }
    }
}
