package es.us.meerkat.backend.dto.users;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Data;

/**
 * Respuesta al activar 2FA con TOTP.
 *
 * <p>Incluye los códigos de respaldo en claro solo en el momento de activación.
 */
@Data
@Builder
public class TotpEnableResponse {

    /** Mensaje descriptivo del resultado. */
    private String message;

    /** Códigos de respaldo en claro (mostrar y guardar una sola vez). */
    private List<String> backupCodes;

    /** Marca de tiempo de la respuesta. */
    @Builder.Default private LocalDateTime timestamp = LocalDateTime.now();
}
