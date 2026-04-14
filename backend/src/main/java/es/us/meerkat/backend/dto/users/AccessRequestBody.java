package es.us.meerkat.backend.dto.users;

import jakarta.validation.constraints.Size;

public record AccessRequestBody(
        @Size(max = 500, message = "El mensaje no puede exceder 500 caracteres") String mensaje,
        String rolDeseado) {}
