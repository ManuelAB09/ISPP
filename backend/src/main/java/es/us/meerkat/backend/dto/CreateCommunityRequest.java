package es.us.meerkat.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateCommunityRequest(
        @NotBlank(message = "El nombre es requerido")
                @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
                String nombre,
        @Size(max = 1000, message = "La descripción no puede exceder 1000 caracteres")
                String descripcion,
        @NotBlank(message = "El tipo de grupo es requerido") String tipoGrupo,
        String imagenUrl,
        @Positive(message = "El máximo de miembros debe ser mayor que 0") Integer maxMiembros,
        @Positive(message = "La institución debe ser válida") Long institutionId,
        String rolInicial) {}
