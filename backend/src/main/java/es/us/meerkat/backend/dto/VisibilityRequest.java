package es.us.meerkat.backend.dto;

import lombok.Data;

/**
 * DTO para actualizar la visibilidad del perfil en listados.
 *
 * Corresponde al schema VisibilityRequest del OpenAPI.
 */
@Data
public class VisibilityRequest {

    /**
     * Indica si el perfil debe aparecer en listados
     * públicos y resultados de búsqueda.
     */
    private Boolean visibleEnListados;
}
