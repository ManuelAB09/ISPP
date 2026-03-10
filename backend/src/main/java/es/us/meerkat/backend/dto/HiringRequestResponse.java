package es.us.meerkat.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/** DTO para respuesta de información de solicitud de contratación. */
@Data
@Builder
@Schema(description = "Información de una solicitud de contratación pendiente")
public class HiringRequestResponse {

    @Schema(description = "ID de la solicitud", example = "1")
    private Long id;

    @Schema(description = "Información básica de la comunidad solicitante")
    private ComunidadSimpleDto comunidad;

    @Schema(description = "Modalidad de contratación solicitada", example = "mensual")
    private String modalidad;

    @Schema(description = "Duración de la contratación solicitada", example = "3 meses")
    private String duracion;

    @Schema(description = "Tarifa acordada", example = "300.00")
    private BigDecimal tarifaAcordada;

    @Schema(description = "Estado de la solicitud")
    private String estado;

    @Schema(description = "Fecha de creación de la solicitud")
    private LocalDateTime createdAt;

    @Data
    @Builder
    @Schema(description = "Información simplificada de una comunidad")
    public static class ComunidadSimpleDto {
        @Schema(description = "ID de la comunidad", example = "1")
        private Long id;

        @Schema(description = "Nombre de la comunidad", example = "Matemáticas Avanzadas")
        private String nombre;

        @Schema(description = "Descripción de la comunidad")
        private String descripcion;

        @Schema(description = "Foto de la comunidad")
        private String foto;

        @Schema(description = "Creador de la comunidad")
        private UserSimpleResponse creador;
    }
}
