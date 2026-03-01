package es.us.meerkat.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;

import es.us.meerkat.backend.dto.EnviarMensajeRequest;
import es.us.meerkat.backend.dto.MensajeResponse;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.service.MensajeService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/mensajes")
@RequiredArgsConstructor
public class MensajeController {

    private final MensajeService mensajeService;

    @PostMapping
    public ResponseEntity<?> enviarMensaje(
            @AuthenticationPrincipal Usuario usuario, @RequestBody EnviarMensajeRequest request) {
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
        }

        try {
            MensajeResponse response = mensajeService.enviarMensaje(usuario.getId(), request);
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
            List<MensajeResponse> conversacion = mensajeService.obtenerConversacion(usuario.getId(), tutorId);
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
            List<MensajeResponse> conversacion = mensajeService.obtenerConversacionConUsuario(usuario.getId(), userId);
            return ResponseEntity.ok(conversacion);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al obtener la conversación: " + e.getMessage());
        }
    }

    @DeleteMapping("/{mensajeId}")
    public ResponseEntity<?> eliminarMensaje(
            @AuthenticationPrincipal Usuario usuario, @PathVariable Long mensajeId) {
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
        }

        try {
            mensajeService.eliminarMensaje(usuario.getId(), mensajeId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al eliminar el mensaje: " + e.getMessage());
        }
    }
}
