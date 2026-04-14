package es.us.meerkat.backend.dto.tutors;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO para contratar un tutor desde una comunidad.
 *
 * <p>Incluye los detalles de la modalidad de trabajo, duración y tarifa acordada.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud de contratación de tutor")
public class HireTutorRequest {

    @NotBlank(message = "La modalidad es requerida (ej: horaria, mensual, por proyecto)")
    @Schema(
            description = "Modalidad de trabajo (ej: horaria, mensual, por proyecto)",
            example = "horaria")
    private String modalidad;

    @NotBlank(message = "La duración es requerida")
    @Schema(
            description = "Duración de la contratación (ej: 3 meses, 12 semanas, 1 año)",
            example = "3 meses")
    private String duracion;

    @NotNull(message = "La tarifa acordada es requerida")
    @DecimalMin(value = "0.01", message = "La tarifa debe ser mayor a 0")
    @Schema(description = "Tarifa acordada en euros", example = "25.50")
    private BigDecimal tarifaAcordada;

    @NotNull(message = "Debes aceptar los términos y condiciones")
    @Schema(description = "Aceptación de términos y condiciones", example = "true")
    private Boolean aceptarTerminos;
}
