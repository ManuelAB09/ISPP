package es.us.meerkat.backend.dto;

import lombok.Data;

/**
 * DTO para la solicitud de inicio de sesión.
 *
 * Recibe las credenciales del usuario (email y contraseña).
 */
@Data
public class LoginRequest {

    /** Email del usuario registrado. */
    private String email;

    /** Contraseña del usuario. */
    private String password;
}
