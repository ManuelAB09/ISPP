package es.us.meerkat.backend.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

/**
 * DTO de respuesta para operaciones que devuelven un mensaje.
 *
 * <p>Corresponde al schema MessageResponse del OpenAPI.
 */
@Data
@Builder
public class MessageResponse {

    /** Mensaje descriptivo del resultado. */
    private String message;

    /** Marca de tiempo de la respuesta. */
    @Builder.Default private LocalDateTime timestamp = LocalDateTime.now();
}
