package es.us.meerkat.backend.dto;

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

    /** Latitud geográfica. */
    private Double latitud;

    /** Longitud geográfica. */
    private Double longitud;
}
