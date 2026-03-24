package es.us.meerkat.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.us.meerkat.backend.entity.Notificacion;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.service.NotificacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Controlador REST para obtener las notificaciones del usuario autenticado. Base URL:
 * /api/v1/notifications
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notificaciones", description = "Notificaciones push y de anuncios")
public class NotificacionController {
    @Operation(
            summary = "Marcar notificación como leída",
            description = "Marca una notificación como leída por su ID.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Notificación marcada como leída"),
        @ApiResponse(responseCode = "404", description = "Notificación no encontrada")
    })
    @org.springframework.web.bind.annotation.PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {
        if (usuario == null) {
            return ResponseEntity.status(401).build();
        }
        boolean marcada = notificacionService.marcarComoLeida(id, usuario.getId());
        if (!marcada) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().build();
    }

    private final NotificacionService notificacionService;

    @GetMapping
    @Operation(
            summary = "Obtener notificaciones del usuario",
            description =
                    "Devuelve todas las notificaciones del usuario, ordenadas por fecha"
                            + " descendente.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Notificaciones obtenidas correctamente"),
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado")
    })
    public ResponseEntity<List<Notificacion>> getMyNotifications(
            @AuthenticationPrincipal Usuario usuario) {
        if (usuario == null) {
            return ResponseEntity.status(401).build();
        }
        List<Notificacion> notificaciones = notificacionService.obtenerNotificaciones(usuario);
        return ResponseEntity.ok(notificaciones);
    }
}
