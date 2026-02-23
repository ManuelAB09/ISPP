package es.us.meerkat.backend.dto;

import lombok.Data;

/**
 * DTO para la solicitud de registro de un nuevo usuario.
 *
 * Recibe email, contraseña y nombre para crear la cuenta.
 */
@Data
public class RegisterRequest {

    /** Email del nuevo usuario. Debe ser único y válido. */
    private String email;

    /** Contraseña del nuevo usuario. Se almacenará cifrada. */
    private String password;

    /** Nombre completo del nuevo usuario. */
    private String nombre;
}
