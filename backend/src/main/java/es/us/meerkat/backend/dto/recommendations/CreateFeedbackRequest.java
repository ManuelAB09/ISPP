package es.us.meerkat.backend.dto.recommendations;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateFeedbackRequest(
        @NotNull Long alumnoId,
        @NotBlank @Size(max = 2000) String contenido,
        Integer calificacion) {}
