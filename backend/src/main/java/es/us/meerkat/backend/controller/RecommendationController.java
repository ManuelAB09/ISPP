package es.us.meerkat.backend.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import es.us.meerkat.backend.dto.FeedbackRecomendacionRequest;
import es.us.meerkat.backend.dto.RecomendacionResponse;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.security.AuthUtils;
import es.us.meerkat.backend.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Controlador para recomendaciones personalizadas basadas en IA. */
@RestController
@RequestMapping("/api/v1/me/recomendaciones")
@Tag(name = "Recomendaciones", description = "Recomendaciones personalizadas basadas en IA")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recomendacionService;
    private final AuthUtils authUtils;

    @GetMapping("/profesores")
    @Operation(
            summary = "Recomendaciones de profesores",
            description = "Obtiene sugerencias de profesores según tus intereses")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de profesores recomendados"),
        @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public ResponseEntity<Page<RecomendacionResponse>> getRecomendacionesProfesores(
            @Parameter(description = "Número de página") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "10")
                    int size,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<RecomendacionResponse> recomendaciones =
                recomendacionService.getRecomendacionesProfesores(usuario.getId(), pageable);
        return ResponseEntity.ok(recomendaciones);
    }

    @GetMapping("/contenido")
    @Operation(
            summary = "Recomendaciones de contenido",
            description = "Obtiene sugerencias de materiales educativos")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de contenido recomendado"),
        @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public ResponseEntity<Page<RecomendacionResponse>> getRecomendacionesContenido(
            @Parameter(description = "Número de página") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "10")
                    int size,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<RecomendacionResponse> recomendaciones =
                recomendacionService.getRecomendacionesContenido(usuario.getId(), pageable);
        return ResponseEntity.ok(recomendaciones);
    }

    @GetMapping("/cuestionarios")
    @Operation(
            summary = "Recomendaciones de cuestionarios",
            description = "Obtiene sugerencias de quizzes y ejercicios prácticos")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de cuestionarios recomendados"),
        @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public ResponseEntity<Page<RecomendacionResponse>> getRecomendacionesCuestionarios(
            @Parameter(description = "Número de página") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "10")
                    int size,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<RecomendacionResponse> recomendaciones =
                recomendacionService.getRecomendacionesCuestionarios(usuario.getId(), pageable);
        return ResponseEntity.ok(recomendaciones);
    }

    @GetMapping("/comunidades")
    @Operation(
            summary = "Recomendaciones de comunidades",
            description = "Obtiene sugerencias de comunidades relevantes")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de comunidades recomendadas"),
        @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public ResponseEntity<Page<RecomendacionResponse>> getRecomendacionesComunidades(
            @Parameter(description = "Número de página") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "10")
                    int size,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<RecomendacionResponse> recomendaciones =
                recomendacionService.getRecomendacionesComunidades(usuario.getId(), pageable);
        return ResponseEntity.ok(recomendaciones);
    }

    @GetMapping
    @Operation(
            summary = "Todas mis recomendaciones",
            description = "Obtiene todas las recomendaciones activas")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de recomendaciones"),
        @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public ResponseEntity<Page<RecomendacionResponse>> getRecomendaciones(
            @Parameter(description = "Número de página") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "20")
                    int size,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<RecomendacionResponse> recomendaciones =
                recomendacionService.getRecomendacionesActivas(usuario.getId(), pageable);
        return ResponseEntity.ok(recomendaciones);
    }

    @PostMapping("/{recomendacionId}/feedback")
    @Operation(
            summary = "Dar feedback sobre una recomendación",
            description = "Indica si una recomendación fue útil para mejorar el algoritmo")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Feedback registrado"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "No tienes permisos"),
        @ApiResponse(responseCode = "404", description = "Recomendación no encontrada")
    })
    public ResponseEntity<Void> darFeedback(
            @Parameter(description = "ID de la recomendación") @PathVariable Long recomendacionId,
            @Valid @RequestBody FeedbackRecomendacionRequest request,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        recomendacionService.darFeedbackRecomendacion(recomendacionId, request, usuario.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/generar")
    @Operation(
            summary = "Generar nuevas recomendaciones",
            description =
                    "Fuerza la generación de nuevas recomendaciones basadas en actividades"
                            + " recientes")
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Generación iniciada"),
        @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public ResponseEntity<Void> generarNuevasRecomendaciones(
            @AuthenticationPrincipal Usuario usuario) {
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        recomendacionService.generarRecomendacionesUsuario(usuario.getId());
        return ResponseEntity.accepted().build();
    }
}
