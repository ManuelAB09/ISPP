package es.us.meerkat.backend.dto.events;

import lombok.Data;

/**
 * DTO para la cancelación de un evento.
 *
 * <p>Contiene el motivo de la cancelación del evento.
 */
@Data
public class CancelEventRequest {

    /** Motivo de la cancelación (máximo 500 caracteres). */
    private String motivo;
}
