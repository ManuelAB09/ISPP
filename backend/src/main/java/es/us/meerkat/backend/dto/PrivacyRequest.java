package es.us.meerkat.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record PrivacyRequest(
    @NotBlank(message = "El tipo de grupo es requerido")
    String tipoGrupo
) {}
