package es.us.meerkat.backend.dto;

import lombok.Data;

/**
 * DTO para el cambio de contraseña del usuario autenticado.
 *
 * Corresponde al schema ChangePasswordRequest del OpenAPI.
 */
@Data
public class ChangePasswordRequest {

    /** Contraseña actual, requerida para confirmar identidad. */
    private String currentPassword;

    /**
     * Nueva contraseña deseada.
     * Debe tener al menos 8 caracteres.
     */
    private String newPassword;
}
