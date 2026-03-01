package es.us.meerkat.backend.dto;

import java.util.List;

import lombok.Data;

/**
 * DTO para actualizar el perfil del usuario autenticado.
 *
 * <p>Corresponde al schema UpdateUserRequest del OpenAPI. Todos los campos son opcionales: solo se
 * actualizan los que no sean nulos.
 */
@Data
public class UpdateUserRequest {

    /** Nuevo nombre del usuario. */
    private String nombre;

    /** Nueva URL de foto de perfil. */
    private String foto;

    /** Nueva biografía. */
    private String bio;

    /** Universidad del usuario. */
    private String universidad;

    /** Grado del usuario. */
    private String grado;

    /** Ubicación del usuario. */
    private String ubicacion;

    /** Nueva lista de intereses. */
    private List<String> intereses;
}
