package es.us.meerkat.backend.dto;

import lombok.Data;

/**
 * DTO para actualizar la configuración de privacidad del usuario.
 *
 * <p>Permite al usuario controlar si su perfil aparece en listados públicos y resultados de
 * búsqueda de la plataforma.
 */
@Data
public class PrivacidadRequest {

    /** Indica si el perfil debe ser visible en listados públicos y resultados de búsqueda. */
    private Boolean visibleEnListados;
}
