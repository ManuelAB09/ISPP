package es.us.meerkat.backend.dto;

import es.us.meerkat.backend.entity.RolComunidad;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** DTO para crear invitaciones a miembros en una comunidad. */
public record CreateInvitacionRequest(
        @NotBlank(message = "El email es requerido") @Email(message = "El email debe ser válido")
                String email,
        @NotNull(message = "El rol es requerido") RolComunidad rol) {}
