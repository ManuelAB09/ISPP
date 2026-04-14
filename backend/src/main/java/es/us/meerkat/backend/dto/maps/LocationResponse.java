package es.us.meerkat.backend.dto.maps;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO con los detalles de una ubicación.
 *
 * <p>Contiene información sobre una ubicación recomendada para eventos: dirección, coordenadas,
 * tipo de lugar, rating y horario.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationResponse {

    /** Identificador de la ubicación. */
    private String id;

    /** Nombre del lugar. */
    private String nombre;

    /** Dirección completa. */
    private String direccion;

    /** Tipo de lugar (biblioteca, cafetería, parque, etc.). */
    private String tipo;

    /** Latitud de la ubicación. */
    private Double latitud;

    /** Longitud de la ubicación. */
    private Double longitud;

    /** Rating o puntuación del lugar. */
    private Float rating;

    /** Horario de funcionamiento del lugar. */
    private String horario;
}
