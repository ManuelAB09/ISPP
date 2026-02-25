package es.us.meerkat.backend.dto;

import lombok.Builder;
import lombok.Data;

/**
 * DTO que representa la información pública de una ubicación.
 *
 * <p>Contiene identificador y coordenadas geográficas.
 */
@Data
@Builder
public class UbicacionResponse {

    /** Identificador único de la ubicación. */
    private Long id;

    /** Nombre del lugar. */
    private String nombre;

    /** Latitud geográfica. */
    private Double latitud;

    /** Longitud geográfica. */
    private Double longitud;
}
