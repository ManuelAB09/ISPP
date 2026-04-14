package es.us.meerkat.backend.dto.maps;

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

    private String direccion;

    /** Latitud geográfica. */
    private Double latitud;

    /** Longitud geográfica. */
    private Double longitud;

    /** Tipo de lugar (e.g., "cafe", "parque", "museo"). */
    private String tipo;

    /** Coste asociado al lugar (e.g., "gratis", "de pago", "desconocido"). */
    private String coste;
}
