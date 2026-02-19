package es.us.meerkatters.controller;

import java.time.LocalDateTime;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador de ejemplo para verificar que Swagger funciona.
 * Puedes eliminarlo cuando implementes los controladores reales.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Health", description = "Endpoints de estado del servidor")
public class HealthController {

    @GetMapping("/health")
    @Operation(
        summary = "Verificar estado del servidor",
        description = "Devuelve el estado actual del servidor y timestamp"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Servidor funcionando correctamente",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = HealthResponse.class)
        )
    )
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(new HealthResponse(
            "UP",
            LocalDateTime.now().toString(),
            "MeerKatters API v1.0.0"
        ));
    }

    // DTO interno para la respuesta
    public record HealthResponse(
        @Schema(description = "Estado del servidor", example = "UP")
        String status,
        
        @Schema(description = "Timestamp actual", example = "2026-02-19T18:30:00")
        String timestamp,
        
        @Schema(description = "Versión de la API", example = "MeerKatters API v1.0.0")
        String version
    ) {}
}
