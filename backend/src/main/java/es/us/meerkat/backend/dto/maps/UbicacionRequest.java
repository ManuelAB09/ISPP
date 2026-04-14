package es.us.meerkat.backend.dto.maps;

import lombok.Data;

/**
 * DTO para la creación o edición de una ubicación.
 *
 * <p>Contiene la información básica necesaria para registrar un punto geográfico asociado a un
 * evento.
 */
@Data
public class UbicacionRequest {

    /** Nombre del lugar (ej. Biblioteca Central). */
    private String nombre;

    private String direccion;

    /** Latitud geográfica. */
    private Double latitud;

    /** Longitud geográfica. */
    private Double longitud;

    private String tipo;

    /** Coste asociado al lugar (e.g., "gratis", "de pago", "desconocido"). */
    private String coste;
}
