package es.us.meerkat.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

/** DTO para contratar un plan corporativo. */
@Data
@Builder
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
}
