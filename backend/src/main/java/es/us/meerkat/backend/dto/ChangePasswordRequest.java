package es.us.meerkat.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO para el cambio de contraseña del usuario autenticado.
 *
 * <p>Corresponde al schema ChangePasswordRequest del OpenAPI.
 */
@Data
public class ChangePasswordRequest {

    /** Contraseña actual, requerida para confirmar identidad. */
    @NotBlank(message = "La contraseña actual no puede estar vacía")
    private String currentPassword;

    /** Nueva contraseña deseada. Debe tener al menos 8 caracteres. */
    @NotBlank(message = "La nueva contraseña no puede estar vacía")
    @Size(min = 8, message = "La nueva contraseña debe tener al menos 8 caracteres")
    private String newPassword;
}
