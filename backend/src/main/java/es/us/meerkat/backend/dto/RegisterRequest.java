package es.us.meerkat.backend.dto;

import lombok.Data;

/**
 * DTO para la solicitud de registro de un nuevo usuario.
 *
 * <p>Corresponde al schema RegisterRequest del OpenAPI.
 */
@Data
public class RegisterRequest {

    /** Email del nuevo usuario. Debe ser único y válido. */
    private String email;

    /** Contraseña del nuevo usuario. Se almacenará cifrada. Mínimo 8 caracteres. */
    private String password;

    /** Nombre completo del nuevo usuario. */
    private String nombre;

    private Boolean esTutor;
}
