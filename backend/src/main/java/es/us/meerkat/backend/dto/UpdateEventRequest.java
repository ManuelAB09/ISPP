package es.us.meerkat.backend.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * DTO para la actualización de un evento existente.
 *
 * <p>Contiene los campos opcionales que se pueden actualizar de un evento.
 */
@Data
public class UpdateEventRequest {

    /** Título del evento (3-200 caracteres). */
    private String titulo;

    /** Descripción detallada del evento (máximo 2000 caracteres). */
    private String descripcion;

    /** Fecha y hora de inicio del evento. */
    private LocalDateTime fechaHora;

    /** Fecha y hora de fin del evento. */
    private LocalDateTime fechaFin;

    /** Ubicación física del evento (máximo 500 caracteres). */
    private String ubicacion;

    /** Latitud de la ubicación. */
    private Double latitud;

    /** Longitud de la ubicación. */
    private Double longitud;

    /** Aforo máximo del evento (1-500). */
    private Integer aforo;

    /** Qué llevar o qué preparar para el evento (máximo 1000 caracteres). */
    private String queLlevar;

    /** Si el evento es visible en el mapa. */
    private Boolean visibleEnMapa;

    /** Si el evento tiene modalidad virtual. */
    private Boolean esVirtual;

    /** Enlace para participación virtual (si aplica). */
    private String enlaceVirtual;
}
