package es.us.meerkat.backend.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para representar un evento en el mapa.
 *
 * <p>Contiene la información básica y georeferenciación para mostrar un evento en el mapa
 * interactivo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MapEventResponse {

    /** Identificador del evento. */
    private Long id;

    /** Título del evento. */
    private String titulo;

    /** Fecha y hora del evento. */
    private LocalDateTime fechaHora;

    /** Latitud de la ubicación. */
    private Double latitud;

    /** Longitud de la ubicación. */
    private Double longitud;

    /** Nombre de la comunidad a la que pertenece. */
    private String comunidadNombre;
}
