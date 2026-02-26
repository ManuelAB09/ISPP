package es.us.meerkat.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
        @NotBlank(message = "El nombre es requerido")
                @Size(max = 50, message = "El nombre no puede exceder 50 caracteres")
                String nombre,
        @Size(max = 200, message = "La descripción no puede exceder 200 caracteres")
                String descripcion) {}
