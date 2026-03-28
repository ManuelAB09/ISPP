package es.us.meerkat.backend.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import es.us.meerkat.backend.dto.AnuncioListResponse;
import es.us.meerkat.backend.dto.AnuncioResponse;
import es.us.meerkat.backend.dto.CreateAnuncioRequest;
import es.us.meerkat.backend.dto.UpdateAnuncioRequest;
import es.us.meerkat.backend.entity.Anuncio;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.service.communities.AnuncioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controlador REST para la gestión de anuncios en comunidades. Base URL:
 * /api/v1/communities/{communityId}/announcements
 */
@RestController
@RequestMapping("/api/v1/communities/{communityId}/announcements")
@Tag(name = "Anuncios", description = "Gestión de anuncios en comunidades")
@RequiredArgsConstructor
public class AnuncioController {

    private final AnuncioService anuncioService;

    /**
     * Obtiene los anuncios de una comunidad de forma paginada.
     *
     * @param communityId ID de la comunidad
     * @param page número de página (0-indexado)
     * @param size tamaño de la página
     * @return página de anuncios
     */
    @GetMapping
    @Operation(
            summary = "Listar anuncios de la comunidad",
            description = "Devuelve los anuncios de la comunidad, ordenados por fecha descendente")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de anuncios obtenida exitosamente"),
        @ApiResponse(responseCode = "404", description = "Comunidad no encontrada")
    })
    public ResponseEntity<AnuncioListResponse> listAnuncios(
            @Parameter(description = "ID de la comunidad") @PathVariable Long communityId,
            @Parameter(description = "Número de página (0-indexado)")
                    @RequestParam(defaultValue = "0")
                    int page,
            @Parameter(description = "Tamaño de la página") @RequestParam(defaultValue = "20")
                    int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Anuncio> anuncios = anuncioService.getAnunciosByCommunity(communityId, pageable);
        AnuncioListResponse response =
                new AnuncioListResponse(
                        anuncios.getContent().stream().map(anuncioService::toResponse).toList(),
                        (int) anuncios.getTotalElements(),
                        page,
                        size);
        return ResponseEntity.ok(response);
    }

    /**
     * Crea un nuevo anuncio en la comunidad.
     *
     * @param communityId ID de la comunidad
     * @param request datos del anuncio
     * @param usuario usuario autenticado
     * @return el anuncio creado
     */
    @PostMapping
    @Operation(
            summary = "Crear anuncio",
            description =
                    "Crea un nuevo anuncio en la comunidad. Solo administradores pueden crear"
                            + " anuncios.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Anuncio creado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
        @ApiResponse(
                responseCode = "403",
                description = "Solo administradores pueden crear anuncios"),
        @ApiResponse(responseCode = "404", description = "Comunidad no encontrada")
    })
    public ResponseEntity<AnuncioResponse> createAnuncio(
            @Parameter(description = "ID de la comunidad") @PathVariable Long communityId,
            @Valid @RequestBody CreateAnuncioRequest request,
            @AuthenticationPrincipal Usuario usuario) {
        Anuncio anuncio = anuncioService.createAnuncio(usuario.getId(), communityId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(anuncioService.toResponse(anuncio));
    }

    /**
     * Obtiene un anuncio específico.
     *
     * @param communityId ID de la comunidad
     * @param anuncioId ID del anuncio
     * @return el anuncio
     */
    @GetMapping("/{anuncioId}")
    @Operation(
            summary = "Obtener anuncio",
            description = "Devuelve los detalles de un anuncio específico")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Anuncio encontrado"),
        @ApiResponse(responseCode = "404", description = "Anuncio o comunidad no encontrada")
    })
    public ResponseEntity<AnuncioResponse> getAnuncio(
            @Parameter(description = "ID de la comunidad") @PathVariable Long communityId,
            @Parameter(description = "ID del anuncio") @PathVariable Long anuncioId) {
        Anuncio anuncio = anuncioService.getAnuncioById(anuncioId);
        return ResponseEntity.ok(anuncioService.toResponse(anuncio));
    }

    /**
     * Actualiza un anuncio existente.
     *
     * @param communityId ID de la comunidad
     * @param anuncioId ID del anuncio
     * @param request datos a actualizar
     * @param usuario usuario autenticado
     * @return el anuncio actualizado
     */
    @PutMapping("/{anuncioId}")
    @Operation(
            summary = "Actualizar anuncio",
            description =
                    "Actualiza un anuncio existente. Solo el creador o un administrador puede"
                            + " actualizar.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Anuncio actualizado exitosamente"),
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
        @ApiResponse(responseCode = "403", description = "Sin permisos para actualizar"),
        @ApiResponse(responseCode = "404", description = "Anuncio no encontrado")
    })
    public ResponseEntity<AnuncioResponse> updateAnuncio(
            @Parameter(description = "ID de la comunidad") @PathVariable Long communityId,
            @Parameter(description = "ID del anuncio") @PathVariable Long anuncioId,
            @Valid @RequestBody UpdateAnuncioRequest request,
            @AuthenticationPrincipal Usuario usuario) {
        Anuncio anuncio = anuncioService.updateAnuncio(usuario.getId(), anuncioId, request);
        return ResponseEntity.ok(anuncioService.toResponse(anuncio));
    }

    /**
     * Elimina un anuncio.
     *
     * @param communityId ID de la comunidad
     * @param anuncioId ID del anuncio
     * @param usuario usuario autenticado
     * @return sin contenido
     */
    @DeleteMapping("/{anuncioId}")
    @Operation(
            summary = "Eliminar anuncio",
            description = "Elimina un anuncio. Solo el creador o un administrador puede eliminar.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Anuncio eliminado exitosamente"),
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
        @ApiResponse(responseCode = "403", description = "Sin permisos para eliminar"),
        @ApiResponse(responseCode = "404", description = "Anuncio no encontrado")
    })
    public ResponseEntity<Void> deleteAnuncio(
            @Parameter(description = "ID de la comunidad") @PathVariable Long communityId,
            @Parameter(description = "ID del anuncio") @PathVariable Long anuncioId,
            @AuthenticationPrincipal Usuario usuario) {
        anuncioService.deleteAnuncio(usuario.getId(), anuncioId);
        return ResponseEntity.noContent().build();
    }
}
