package es.us.meerkat.backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import es.us.meerkat.backend.entity.Mensaje;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.MensajeRepository;
import es.us.meerkat.backend.service.chats.MensajeLeidoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/v1/mensajes-leidos")
public class MensajeLeidoController {
    @Autowired private MensajeLeidoService mensajeLeidoService;
    @Autowired private MensajeRepository mensajeRepository;

    @PostMapping("/marcar-leido/{mensajeId}")
    @Operation(
            summary = "Marcar mensaje como leído",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Marcado como leído correctamente"),
        @ApiResponse(responseCode = "404", description = "Mensaje no encontrado")
    })
    public ResponseEntity<?> marcarComoLeido(
            @PathVariable Long mensajeId, @AuthenticationPrincipal Usuario usuario) {
        Mensaje mensaje = mensajeRepository.findById(mensajeId).orElse(null);
        if (mensaje == null) {
            return ResponseEntity.notFound().build();
        }
        mensajeLeidoService.marcarComoLeido(mensaje, usuario);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/leidos")
    @Operation(
            summary = "Obtener ids de mensajes leídos de una lista",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<Long>> obtenerLeidos(
            @RequestBody Map<String, List<Long>> body, @AuthenticationPrincipal Usuario usuario) {
        List<Long> mensajeIds = body.get("mensajeIds");
        if (mensajeIds == null) {
            return ResponseEntity.badRequest().build();
        }
        List<Long> idsLeidos =
                mensajeLeidoService.obtenerIdsMensajesLeidos(usuario.getId(), mensajeIds);
        return ResponseEntity.ok(idsLeidos);
    }
}
