package es.us.meerkat.backend.dto;

import java.time.LocalDateTime;

import es.us.meerkat.backend.entity.FactorRecomendacion;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO de respuesta para recomendaciones de comunidades. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Recomendación personalizada del sistema")
public class RecomendacionResponse {

    @Schema(description = "ID de la recomendación", example = "123")
    private Long id;

    @Schema(
            description =
                    "Tipo de recomendación (PROFESOR, CONTENIDO, CUESTIONARIO, COMUNIDAD, EVENTO)")
    private String tipo;

    @Schema(description = "ID del objeto recomendado", example = "456")
    private Long communityId;

    @Schema(description = "Título o nombre de lo recomendado", example = "Profesor Juan García")
    private String nombre;

    @Schema(description = "Descripción breve")
    private String descripcion;

    @Schema(description = "URL de imagen/thumbnail")
    private String imagenUrl;

    @Schema(description = "Factor de recomendación")
    private FactorRecomendacion factor;

    @Schema(description = "Puntuación de relevancia (0-100)", example = "85.5")
    private Double relevancia;

    @Schema(
            description = "Razón de la recomendación para mostrar al usuario",
            example = "Coincide con tu interés en Matemáticas")
    private String motivo;

    @Schema(description = "¿Ha sido visto?")
    private Boolean vista;

    @Schema(
            description =
                    "¿El usuario indicó si fue útil? null=sin feedback, true=útil, false=no útil")
    private Boolean esFavorable;

    @Schema(description = "Fecha de creación")
    private LocalDateTime createdAt;
}
