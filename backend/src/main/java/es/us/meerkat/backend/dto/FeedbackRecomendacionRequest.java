package es.us.meerkat.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request para registrar el feedback de un usuario sobre una recomendación. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackRecomendacionRequest {

    /** ¿Le fue útil la recomendación? true = útil, false = no útil */
    @NotNull(message = "Debes indicar si la recomendación fue útil o no")
    private Boolean esUtil;

    /** Comentario opcional del usuario */
    private String comentario;

    /** Valoración de 1 a 5 (opcional) */
    @Min(value = 1, message = "La satisfacción mínima es 1")
    @Max(value = 5, message = "La satisfacción máxima es 5")
    private Integer satisfaccion;
}
