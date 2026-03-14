package es.us.meerkat.backend.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * DTO para la creación de un nuevo evento.
 *
 * <p>Contiene la información necesaria para crear un evento de estudio.
 */
@Data
public class CreateEventRequest {

    /** Título del evento (requerido, 3-200 caracteres). */
    private String titulo;

    /** Descripción detallada del evento (máximo 2000 caracteres). */
    private String descripcion;

    /** Fecha y hora de inicio del evento (requerida). */
    private LocalDateTime fechaHora;

    /** Fecha y hora de fin del evento. */
    private LocalDateTime fechaFin;

    /** Aforo máximo del evento (requerido, 1-999). */
    private Integer aforo;

    /** Qué llevar o qué preparar para el evento (máximo 1000 caracteres). */
    private String queLlevar;

    /** Si el evento es visible en el mapa (por defecto true). */
    private Boolean visibleEnMapa;

    /** Si el evento tiene modalidad virtual (por defecto false). */
    private Boolean esVirtual;

    /** Si el evento es privado o no. */
    private Boolean privado;

    /** Enlace para participación virtual (si aplica). */
    private String enlaceVirtual;

    /** ID de la ubicación asociada (para eventos presenciales). */
    private Long ubicacionId;
}
