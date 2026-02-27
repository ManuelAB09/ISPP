package es.us.meerkat.backend.dto;

import jakarta.validation.constraints.Size;

public record UpdateCommunityRequest(
        @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
                String nombre,
        @Size(max = 1000, message = "La descripción no puede exceder 1000 caracteres")
                String descripcion,
        String imagenUrl) {}
