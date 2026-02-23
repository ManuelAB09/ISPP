package es.us.meerkat.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * DTO que representa el perfil público de un usuario.
 *
 * Solo expone los campos que el usuario ha decidido hacer públicos.
 * No incluye email, contraseña ni datos sensibles.
 */
@Data
@Builder
public class UsuarioPerfilResponse {

    /** Identificador único del usuario. */
    private Long id;

    /** Nombre del usuario. */
    private String nombre;

    /** URL de la foto de perfil. */
    private String foto;

    /** Biografía del usuario. */
    private String bio;

    /** Lista de intereses del usuario. */
    private List<String> intereses;

    /** Indica si el usuario tiene rol de tutor. */
    private Boolean esTutor;
}
