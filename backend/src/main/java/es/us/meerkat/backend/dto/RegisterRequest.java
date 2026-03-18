package es.us.meerkat.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO para la solicitud de registro de un nuevo usuario.
 *
 * <p>Corresponde al schema RegisterRequest del OpenAPI.
 */
@Data
public class RegisterRequest {

    /** Email del nuevo usuario. Debe ser único y válido. */
    @NotBlank(message = "El email no puede estar vacío")
    @Email(message = "El formato del email no es válido")
    private String email;

    /** Contraseña del nuevo usuario. Se almacenará cifrada. Entre 8 y 128 caracteres. */
    @NotBlank(message = "La contraseña no puede estar vacía")
    @Size(min = 8, max = 128, message = "La contraseña debe tener entre 8 y 128 caracteres")
    private String password;

    /** Nombre completo del nuevo usuario. */
    @NotBlank(message = "El nombre no puede estar vacío")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String nombre;

    private Boolean esTutor;
}
