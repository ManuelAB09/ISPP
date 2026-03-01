package es.us.meerkat.backend.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

/**
 * DTO con los datos públicos del perfil de un usuario.
 *
 * <p>Corresponde al schema UserPublicResponse del OpenAPI. No expone email ni datos sensibles.
 */
@Data
@Builder
public class UserPublicResponse {

    /** Identificador del usuario. */
    private Long id;

    /** Nombre del usuario. */
    private String nombre;

    /** URL de la foto de perfil. */
    private String foto;

    /** Biografía del usuario. */
    private String bio;

    /** Universidad del usuario. */
    private String universidad;

    /** Grado del usuario. */
    private String grado;

    /** Ubicación del usuario. */
    private String ubicacion;

    /** Lista de intereses del usuario. */
    private List<String> intereses;

    /** Indica si el usuario tiene perfil de tutor. */
    private Boolean esTutor;
}
