package es.us.meerkat.backend.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import es.us.meerkat.backend.dto.RecomendacionListResponse;
import es.us.meerkat.backend.entity.RecomendacionComunidad;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.service.RecomendacionComunidadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Controlador REST para la gestión de recomendaciones de comunidades. Base URL:
 * /api/v1/recommendations
 */
@RestController
@RequestMapping("/api/v1/recommendations")
@Tag(name = "Recomendaciones", description = "Recomendaciones personalizadas de comunidades")
@RequiredArgsConstructor
public class RecomendacionController {

    private final RecomendacionComunidadService recomendacionService;

    /**
     * Obtiene las recomendaciones de comunidades para el usuario autenticado.
     *
     * @param page número de página
     * @param size tamaño de la página
     * @param usuario usuario autenticado
     * @return página de recomendaciones
     */
    @GetMapping
    @Operation(
            summary = "Obtener recomendaciones personalizadas",
            description =
                    "Devuelve comunidades recomendadas basadas en intereses, perfil y actividad del"
                            + " usuario.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de recomendaciones obtenida"),
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado")
    })
    public ResponseEntity<RecomendacionListResponse> getRecomendaciones(
            @Parameter(description = "Número de página") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "20")
                    int size,
            @AuthenticationPrincipal Usuario usuario) {
        Pageable pageable = PageRequest.of(page, size);
        Page<RecomendacionComunidad> recomendaciones =
                recomendacionService.getRecomendacionesUsuario(usuario.getId(), pageable);
        RecomendacionListResponse response =
                new RecomendacionListResponse(
                        recomendaciones.getContent().stream()
                                .map(recomendacionService::toResponse)
                                .toList(),
                        (int) recomendaciones.getTotalElements(),
                        page,
                        size);
        return ResponseEntity.ok(response);
    }

    /**
     * Obtiene solo las recomendaciones no vistas del usuario.
     *
     * @param page número de página
     * @param size tamaño de la página
     * @param usuario usuario autenticado
     * @return página de recomendaciones no vistas
     */
    @GetMapping("/unseen")
    @Operation(
            summary = "Obtener recomendaciones no vistas",
            description = "Devuelve solo las recomendaciones que el usuario aún no ha visto.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de recomendaciones no vistas"),
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado")
    })
    public ResponseEntity<RecomendacionListResponse> getRecomendacionesNoVistas(
            @Parameter(description = "Número de página") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "20")
                    int size,
            @AuthenticationPrincipal Usuario usuario) {
        Pageable pageable = PageRequest.of(page, size);
        Page<RecomendacionComunidad> recomendaciones =
                recomendacionService.getRecomendacionesNoVistas(usuario.getId(), pageable);
        RecomendacionListResponse response =
                new RecomendacionListResponse(
                        recomendaciones.getContent().stream()
                                .map(recomendacionService::toResponse)
                                .toList(),
                        (int) recomendaciones.getTotalElements(),
                        page,
                        size);
        return ResponseEntity.ok(response);
    }

    /**
     * Marca una recomendación como vista.
     *
     * @param recomendacionId ID de la recomendación
     * @param usuario usuario autenticado
     * @return mensaje de confirmación
     */
    @PostMapping("/{recomendacionId}/marcar-vista")
    @Operation(
            summary = "Marcar recomendación como vista",
            description = "Marca una recomendación como vista para ofrecer nuevas sugerencias.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Recomendación marcada como vista"),
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
        @ApiResponse(responseCode = "404", description = "Recomendación no encontrada")
    })
    public ResponseEntity<es.us.meerkat.backend.dto.MessageResponse> marcarComoVista(
            @Parameter(description = "ID de la recomendación") @PathVariable Long recomendacionId,
            @AuthenticationPrincipal Usuario usuario) {
        recomendacionService.marcarComoVista(recomendacionId);
        return ResponseEntity.ok(
                es.us.meerkat.backend.dto.MessageResponse.builder()
                        .message("Recomendación marcada como vista.")
                        .build());
    }

    /**
     * Descarta una recomendación.
     *
     * @param recomendacionId ID de la recomendación
     * @param usuario usuario autenticado
     * @return sin contenido
     */
    @DeleteMapping("/{recomendacionId}")
    @Operation(
            summary = "Descartar recomendación",
            description = "Elimina una recomendación que no es de interés para el usuario.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Recomendación descartada"),
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
        @ApiResponse(responseCode = "404", description = "Recomendación no encontrada")
    })
    public ResponseEntity<Void> descartarRecomendacion(
            @Parameter(description = "ID de la recomendación") @PathVariable Long recomendacionId,
            @AuthenticationPrincipal Usuario usuario) {
        recomendacionService.eliminarRecomendacion(recomendacionId);
        return ResponseEntity.noContent().build();
    }
}
