package es.us.meerkat.backend.dto.suscriptions;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO para solicitar suscripcion a un plan premium. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos para suscribirse a un plan premium")
public class SubscribeRequest {

    @Schema(description = "ID del plan a contratar", example = "PREMIUM")
    @NotNull(message = "El plan es requerido")
    private String planId;

    @Schema(description = "Acepta los terminos de servicio")
    @NotNull(message = "Debe aceptar los terminos de servicio")
    @AssertTrue(message = "Debe aceptar los terminos de servicio")
    private Boolean aceptarTerminos;

    @Schema(description = "Periodo de la suscripcion", example = "mensual")
    @NotNull(message = "Debe seleccionar un periodo")
    @Pattern(regexp = "^(mensual|anual)$", message = "El periodo debe ser 'mensual' o 'anual'")
    private String periodo;
}
