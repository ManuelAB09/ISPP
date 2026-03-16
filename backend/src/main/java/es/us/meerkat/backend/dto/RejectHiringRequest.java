package es.us.meerkat.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request DTO para rechazar una solicitud de contratación. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud para rechazar una contratación")
public class RejectHiringRequest {

    @NotBlank(message = "El motivo del rechazo es obligatorio")
    @Schema(description = "Motivo del rechazo", example = "No dispongo de tiempo en este momento")
    private String motivo;
}
