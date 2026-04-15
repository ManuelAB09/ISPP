package es.us.meerkat.backend.dto.users;

import lombok.Data;

/**
 * DTO para la solicitud de cambio de contraseña.
 *
 * <p>Requiere la contraseña actual para verificar identidad, y la nueva contraseña con su
 * confirmación.
 */
@Data
public class CambiarPasswordRequest {

    /** Contraseña actual del usuario, requerida para confirmar identidad. */
    private String passwordActual;

    /** Nueva contraseña deseada. Debe tener al menos 8 caracteres. */
    private String passwordNueva;

    /** Confirmación de la nueva contraseña. Debe coincidir con passwordNueva. */
    private String passwordConfirmacion;
}
