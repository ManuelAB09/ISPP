package es.us.meerkat.backend.dto;

import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(
    @Size(max = 50, message = "El nombre no puede exceder 50 caracteres")
    String nombre,

    @Size(max = 200, message = "La descripción no puede exceder 200 caracteres")
    String descripcion
) {}
