package es.us.meerkat.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request DTO para dar feedback sobre una recomendación. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Feedback sobre una recomendación")
public class FeedbackRecomendacionRequest {

    @NotNull(message = "¿Le fue útil?")
    @Schema(description = "¿Le fue útil la recomendación?", example = "true")
    private Boolean esUtil;

    @Schema(description = "Comentario opcional", example = "Me gustaría, pero el horario no me va")
    private String comentario;

    @Min(value = 1, message = "Mínimo 1")
    @Max(value = 5, message = "Máximo 5")
    @Schema(description = "Nivel de satisfacción (1-5)", example = "4")
    private Integer satisfaccion;
}
