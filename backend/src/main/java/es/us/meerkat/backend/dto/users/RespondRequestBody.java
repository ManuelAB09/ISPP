package es.us.meerkat.backend.dto.users;

import jakarta.validation.constraints.NotNull;

public record RespondRequestBody(
        @NotNull(message = "La respuesta es requerida") Boolean aceptado) {}
