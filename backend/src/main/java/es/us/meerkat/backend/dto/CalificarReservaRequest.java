package es.us.meerkat.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request DTO para calificar y comentar una reserva completada. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud para calificar una reserva completa")
public class CalificarReservaRequest {

    @NotNull(message = "Calificación requerida")
    @Min(value = 1, message = "Calificación mínima es 1")
    @Max(value = 5, message = "Calificación máxima es 5")
    @Schema(description = "Calificación de 1 a 5", example = "5")
    private Integer calificacion;

    @Schema(
            description = "Comentario optativo (máx 500 caracteres)",
            example = "Excelente clase, muy clara la explicación")
    private String comentario;

    @NotNull(message = "Se requiere aceptar términos")
    @Schema(description = "Aceptación de términos", example = "true")
    private Boolean aceptarTerminos;
}
