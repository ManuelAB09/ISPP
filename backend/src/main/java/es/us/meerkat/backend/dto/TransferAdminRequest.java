package es.us.meerkat.backend.dto;

import jakarta.validation.constraints.NotNull;

public record TransferAdminRequest(
        @NotNull(message = "El ID del nuevo admin es requerido") Long nuevoAdminId,
        String nuevoRolOrigen) {}
