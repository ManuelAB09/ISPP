package es.us.meerkat.backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import es.us.meerkat.backend.entity.MensajeComunidad;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.MensajeComunidadRepository;
import es.us.meerkat.backend.service.MensajeComunidadLeidoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/v1/mensajes-comunidad-leidos")
public class MensajeComunidadLeidoController {
    @Autowired private MensajeComunidadLeidoService mensajeComunidadLeidoService;
    @Autowired private MensajeComunidadRepository mensajeComunidadRepository;

    @PostMapping("/marcar-leido/{mensajeId}")
    @Operation(
            summary = "Marcar mensaje de comunidad como leído",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Marcado como leído correctamente"),
        @ApiResponse(responseCode = "404", description = "Mensaje no encontrado")
    })
    public ResponseEntity<?> marcarComoLeido(
            @PathVariable Long mensajeId, @AuthenticationPrincipal Usuario usuario) {
        MensajeComunidad mensaje = mensajeComunidadRepository.findById(mensajeId).orElse(null);
        if (mensaje == null) {
            return ResponseEntity.notFound().build();
        }
        mensajeComunidadLeidoService.marcarComoLeido(mensaje, usuario);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/leidos")
    @Operation(
            summary = "Obtener ids de mensajes de comunidad leídos de una lista",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<Long>> obtenerLeidos(
            @RequestBody Map<String, List<Long>> body, @AuthenticationPrincipal Usuario usuario) {
        List<Long> mensajeIds = body.get("mensajeIds");
        if (mensajeIds == null) {
            return ResponseEntity.badRequest().build();
        }
        List<Long> idsLeidos =
                mensajeComunidadLeidoService.obtenerIdsMensajesLeidos(usuario.getId(), mensajeIds);
        return ResponseEntity.ok(idsLeidos);
    }
}
