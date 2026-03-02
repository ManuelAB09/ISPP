package es.us.meerkat.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO para solicitar suscripción a un plan premium. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos para suscribirse a un plan premium")
public class SubscribeRequest {

    @Schema(description = "ID del plan a contratar", example = "PREMIUM")
    @NotNull(message = "El plan es requerido")
    private String planId;

    @Schema(description = "Acepta los términos de servicio")
    @NotNull(message = "Debe aceptar los términos de servicio")
    private Boolean aceptarTerminos;
}
