package es.us.meerkat.backend.dto;

import lombok.Builder;
import lombok.Data;

/**
 * DTO de respuesta tras autenticación exitosa (registro o inicio de sesión).
 *
 * Devuelve un token JWT junto con los datos básicos del usuario autenticado.
 */
@Data
@Builder
public class AuthResponse {

    /** Token JWT para autenticar futuras peticiones. */
    private String token;

    /** Identificador único del usuario autenticado. */
    private Long id;

    /** Email del usuario autenticado. */
    private String email;

    /** Nombre del usuario autenticado. */
    private String nombre;

    /** Indica si el usuario tiene rol de tutor. */
    private Boolean esTutor;
}
