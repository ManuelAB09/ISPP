package es.us.meerkat.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Data;

/**
 * DTO con los datos completos del usuario autenticado.
 *
 * <p>Corresponde al schema UserDetailResponse del OpenAPI. Se usa en respuestas de /users/me y
 * dentro de AuthResponse.
 */
@Data
@Builder
public class UserDetailResponse {

    /** Identificador único del usuario. */
    private Long id;

    /** Email del usuario. */
    private String email;

    /** Nombre completo del usuario. */
    private String nombre;

    /** URL de la foto de perfil. */
    private String foto;

    /** Color de fondo de la foto de perfil. */
    private String fotoBackgroundColor;

    /** Breve biografía del usuario. */
    private String bio;

    /** Universidad del usuario. */
    private String universidad;

    /** Grado del usuario. */
    private String grado;

    /** Nivel de estudios del usuario. */
    private String nivelEstudios;

    /** Base formativa del usuario. */
    private String baseFormativa;

    /** Ubicación del usuario. */
    private UbicacionResponse ubicacion;

    /** Lista de intereses del usuario. */
    private List<String> intereses;

    /** Indica si el perfil es visible en listados públicos y resultados de búsqueda. */
    private Boolean visibleEnListados;

    /** Indica si el usuario tiene perfil de tutor. */
    private Boolean esTutor;

    /** Indica si el usuario tiene activada la autenticación de dos factores. */
    private Boolean autenticacionDosFactores;

    /** Indica si el usuario recibe notificaciones por email. */
    private Boolean notificacionesEmail;

    /** Indica si el usuario recibe notificaciones push. */
    private Boolean notificacionesPush;

    /** Fecha de creación de la cuenta. */
    private LocalDateTime createdAt;
}
