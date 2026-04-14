package es.us.meerkat.backend.dto.subscriptions;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO para contratar un plan corporativo. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos para contratar un plan corporativo")
public class CorporatePlanRequest {

    @Schema(description = "Tipo de plan corporativo", example = "REDUCIDO_PUBLICA")
    @NotBlank(message = "El tipo de plan es requerido")
    private String tipoPlan;

    @Schema(description = "Número de usuarios a incluir", example = "100")
    @NotNull(message = "El número de usuarios es requerido")
    private Integer numUsuarios;

    @Schema(description = "Duración del plan en meses", example = "12")
    @NotNull(message = "La duración es requerida")
    private Integer duracionMeses;

    @Schema(description = "Aceptar términos de servicio")
    @NotNull(message = "Debe aceptar los términos")
    private Boolean aceptarTerminos;

    @Schema(description = "Documentación de elegibilidad (para planes reducidos)")
    private String documentacionEligibilidad;

    @Schema(description = "Periodo de facturación", example = "mensual")
    private String periodo; // "mensual" o "anual"
}
