package es.us.meerkat.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO con información de paginación.
 *
 * <p>Contiene metadatos sobre la página actual de resultados.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageInfo {

    /** Número de página actual (basado en 0). */
    private Integer number;

    /** Tamaño de la página (elementos por página). */
    private Integer size;

    /** Número total de elementos. */
    private Long totalElements;

    /** Número total de páginas. */
    private Integer totalPages;

    /** Si es la primera página. */
    private Boolean first;

    /** Si es la última página. */
    private Boolean last;
}
