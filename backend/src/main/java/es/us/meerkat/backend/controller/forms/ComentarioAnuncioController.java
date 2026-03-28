package es.us.meerkat.backend.controller.forms;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import es.us.meerkat.backend.dto.ComentarioAnuncioResponse;
import es.us.meerkat.backend.dto.CreateComentarioAnuncioRequest;
import es.us.meerkat.backend.entity.Usuario;
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
}
