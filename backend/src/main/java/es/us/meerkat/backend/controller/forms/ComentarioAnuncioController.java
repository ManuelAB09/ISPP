package es.us.meerkat.backend.controller.forms;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import es.us.meerkat.backend.dto.communities.ComentarioAnuncioResponse;
import es.us.meerkat.backend.dto.communities.CreateComentarioAnuncioRequest;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.service.forms.ComentarioAnuncioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/announcements/{anuncioId}/comments")
@Tag(name = "Comentarios de Anuncio", description = "Gestión de comentarios en anuncios")
@RequiredArgsConstructor
public class ComentarioAnuncioController {
    private final ComentarioAnuncioService comentarioService;

    @GetMapping
    @Operation(summary = "Listar comentarios de un anuncio")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Lista de comentarios obtenida exitosamente"),
        @ApiResponse(responseCode = "404", description = "Anuncio no encontrado")
    })
    public ResponseEntity<List<ComentarioAnuncioResponse>> listarComentarios(
            @Parameter(description = "ID del anuncio") @PathVariable Long anuncioId) {
        return ResponseEntity.ok(comentarioService.listarComentarios(anuncioId));
    }

    @PostMapping
    @Operation(
            summary = "Crear comentario en anuncio",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Comentario creado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
        @ApiResponse(responseCode = "404", description = "Anuncio no encontrado")
    })
    public ResponseEntity<ComentarioAnuncioResponse> crearComentario(
            @Parameter(description = "ID del anuncio") @PathVariable Long anuncioId,
            @Valid @RequestBody CreateComentarioAnuncioRequest request,
            @AuthenticationPrincipal Usuario usuario) {
        ComentarioAnuncioResponse resp =
                comentarioService.crearComentario(anuncioId, usuario.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @DeleteMapping("/{comentarioId}")
    @Operation(
            summary = "Eliminar comentario",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Comentario eliminado"),
        @ApiResponse(responseCode = "403", description = "Sin permisos"),
        @ApiResponse(responseCode = "404", description = "Comentario no encontrado")
    })
    public ResponseEntity<Void> eliminarComentario(
            @PathVariable Long anuncioId,
            @PathVariable Long comentarioId,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            comentarioService.eliminarComentario(comentarioId, usuario.getId());
            return ResponseEntity.noContent().build();
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
