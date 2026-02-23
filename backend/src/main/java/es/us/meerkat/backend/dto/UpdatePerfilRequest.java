package es.us.meerkat.backend.dto;

import lombok.Data;

import java.util.List;

/**
 * DTO para la edición del perfil de un usuario.
 *
 * Solo se actualizan los campos que el usuario desea modificar.
 * Los campos nulos se ignoran en el servicio.
 */
@Data
public class UpdatePerfilRequest {

    /** Nuevo nombre del usuario. */
    private String nombre;

    /** Nueva URL de la foto de perfil. */
    private String foto;

    /** Nueva biografía del usuario. */
    private String bio;

    /** Nueva lista de intereses del usuario. */
    private List<String> intereses;
}
