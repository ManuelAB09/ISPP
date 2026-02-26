package es.us.meerkat.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record HireTutorRequest(
    @NotNull(message = "El ID del tutor es requerido")
    Long tutorId,
    
    @Min(value = 1, message = "La duración debe ser mínimo 1 mes")
    Integer duracionMeses
) {}
