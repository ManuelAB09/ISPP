package es.us.meerkat.backend.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import es.us.meerkat.backend.dto.EnviarMensajeRequest;
import es.us.meerkat.backend.dto.MensajeResponse;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.UsuarioRepository;
import es.us.meerkat.backend.service.ChatFileStorageService;
import es.us.meerkat.backend.service.MensajeService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/mensajes")
@RequiredArgsConstructor
public class MensajeController {

    private final MensajeService mensajeService;
    private final ChatFileStorageService chatFileStorageService;
    private final SimpMessagingTemplate broker;
    private final UsuarioRepository usuarioRepository;

    @PostMapping
    public ResponseEntity<?> enviarMensaje(
            @AuthenticationPrincipal Usuario usuario, @RequestBody EnviarMensajeRequest request) {
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
        }

        try {
            MensajeResponse response = mensajeService.enviarMensaje(usuario.getId(), request);
            broadcastDm(usuario, response);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al enviar el mensaje: " + e.getMessage());
        }
    }

    @GetMapping("/tutor/{tutorId}")
    public ResponseEntity<?> obtenerConversacion(
            @AuthenticationPrincipal Usuario usuario, @PathVariable Long tutorId) {
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
        }

        try {
            List<MensajeResponse> conversacion =
                    mensajeService.obtenerConversacion(usuario.getId(), tutorId);
            return ResponseEntity.ok(conversacion);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al obtener la conversación: " + e.getMessage());
        }
    }

    @GetMapping("/usuario/{userId}")
    public ResponseEntity<?> obtenerConversacionConUsuario(
            @AuthenticationPrincipal Usuario usuario, @PathVariable Long userId) {
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
        }

        try {
            List<MensajeResponse> conversacion =
                    mensajeService.obtenerConversacionConUsuario(usuario.getId(), userId);
            return ResponseEntity.ok(conversacion);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al obtener la conversación: " + e.getMessage());
        }
    }

    @GetMapping("/conversaciones")
    public ResponseEntity<?> obtenerConversaciones(@AuthenticationPrincipal Usuario usuario) {
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
        }

        try {
            var conversaciones = mensajeService.obtenerConversaciones(usuario.getId());
            return ResponseEntity.ok(conversaciones);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al obtener conversaciones: " + e.getMessage());
        }
    }

    @DeleteMapping("/{mensajeId}")
    public ResponseEntity<?> eliminarMensaje(
            @AuthenticationPrincipal Usuario usuario, @PathVariable Long mensajeId) {
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
        }

        try {
            MensajeResponse deleted = mensajeService.obtenerMensaje(mensajeId);
            Long otherUserId =
                    deleted.getEmisorId().equals(usuario.getId())
                            ? deleted.getReceptorId()
                            : deleted.getEmisorId();
            mensajeService.eliminarMensaje(usuario.getId(), mensajeId);
            // Notify both parties
            broker.convertAndSendToUser(usuario.getEmail(), "/queue/dm_delete_success", mensajeId);
            usuarioRepository
                    .findById(otherUserId)
                    .ifPresent(
                            other ->
                                    broker.convertAndSendToUser(
                                            other.getEmail(),
                                            "/queue/dm_delete_success",
                                            mensajeId));
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al eliminar el mensaje: " + e.getMessage());
        }
    }

    @PutMapping("/{mensajeId}")
    public ResponseEntity<?> editarMensaje(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable Long mensajeId,
            @RequestBody EnviarMensajeRequest request) {
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
        }

        try {
            MensajeResponse response =
                    mensajeService.editarMensaje(
                            usuario.getId(), mensajeId, request.getContenido());
            broadcastDmUpdate(usuario, response);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al editar el mensaje: " + e.getMessage());
        }
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> enviarArchivo(
            @AuthenticationPrincipal Usuario usuario,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long tutorId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String contenido) {
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
        }

        try {
            ChatFileStorageService.ValidatedChatFile validatedFile =
                    chatFileStorageService.validateAndExtract(file);

            MensajeResponse response =
                    mensajeService.enviarArchivo(
                            usuario.getId(),
                            userId,
                            tutorId,
                            contenido,
                            validatedFile.originalName(),
                            validatedFile.mimeType(),
                            validatedFile.sizeBytes(),
                            validatedFile.content());

            broadcastDm(usuario, response);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al subir archivo de chat: " + e.getMessage());
        }
    }

    @GetMapping("/{mensajeId}/archivo")
    public ResponseEntity<?> descargarArchivo(
            @AuthenticationPrincipal Usuario usuario, @PathVariable Long mensajeId) {
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
        }

        try {
            MensajeService.MensajeArchivo archivo =
                    mensajeService.obtenerArchivo(usuario.getId(), mensajeId);

            String mimeType =
                    archivo.mimeType() == null || archivo.mimeType().isBlank()
                            ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                            : archivo.mimeType();
            String nombre =
                    archivo.nombre() == null || archivo.nombre().isBlank()
                            ? "adjunto"
                            : archivo.nombre();

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(mimeType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + nombre + "\"")
                    .body(archivo.data());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al descargar archivo: " + e.getMessage());
        }
    }

    /** Broadcasts a new/file message to both sender and receiver via WebSocket. */
    private void broadcastDm(Usuario sender, MensajeResponse response) {
        broker.convertAndSendToUser(sender.getEmail(), "/queue/dm", response);
        Long receiverId = response.getReceptorId();
        if (receiverId != null && !receiverId.equals(sender.getId())) {
            usuarioRepository
                    .findById(receiverId)
                    .ifPresent(
                            receiver ->
                                broker.convertAndSendToUser(
                                        receiver.getEmail(), "/queue/dm", response));
        }
    }

    /** Broadcasts a message update to both parties via WebSocket. */
    private void broadcastDmUpdate(Usuario sender, MensajeResponse response) {
        broker.convertAndSendToUser(sender.getEmail(), "/queue/dm_update_success", response);
        Long otherUserId =
                response.getEmisorId().equals(sender.getId())
                        ? response.getReceptorId()
                        : response.getEmisorId();
        if (otherUserId != null) {
            usuarioRepository
                    .findById(otherUserId)
                    .ifPresent(
                            other ->
                                    broker.convertAndSendToUser(
                                            other.getEmail(),
                                            "/queue/dm_update_success",
                                            response));
        }
    }
}
