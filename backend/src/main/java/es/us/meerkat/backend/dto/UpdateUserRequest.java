package es.us.meerkat.backend.dto;

import java.util.List;

import jakarta.validation.constraints.Size;
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
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String nombre;

    /** Nueva URL de foto de perfil. */
    @Size(max = 2048, message = "La URL de foto no puede exceder 2048 caracteres")
    private String foto;

    /** Nuevo color de fondo para la foto de perfil. */
    private String fotoBackgroundColor;

    /** Nueva biografía. */
    @Size(max = 500, message = "La bio no puede exceder 500 caracteres")
    private String bio;

    /** Universidad del usuario. */
    @Size(max = 100, message = "La universidad no puede exceder 100 caracteres")
    private String universidad;

    /** Grado del usuario. */
    @Size(max = 100, message = "El grado no puede exceder 100 caracteres")
    private String grado;

    /** Nivel de estudios del usuario. */
    @Size(max = 100, message = "El nivel de estudios no puede exceder 100 caracteres")
    private String nivelEstudios;

    /** Base formativa del usuario. */
    @Size(max = 100, message = "La base formativa no puede exceder 100 caracteres")
    private String baseFormativa;

    /** Ubicación del usuario. */
    @Size(max = 100, message = "La ubicación no puede exceder 100 caracteres")
    private String ubicacion;

    /** Estado de autenticación de dos factores del usuario. */
    private Boolean autenticacionDosFactores;

    /** Visibilidad del perfil en listados públicos. */
    private Boolean visibleEnListados;

    /** Preferencia de notificaciones por email. */
    private Boolean notificacionesEmail;

    /** Preferencia de notificaciones push. */
    private Boolean notificacionesPush;

    /** Nueva lista de intereses. */
    private Boolean esTutor;

    @Size(max = 10, message = "No se permiten más de 10 intereses")
    private List<@Size(max = 50, message = "Cada interés no puede exceder 50 caracteres") String>
            intereses;
}
