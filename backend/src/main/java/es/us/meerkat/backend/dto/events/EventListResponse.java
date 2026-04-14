package es.us.meerkat.backend.dto.events;

import java.util.List;

import es.us.meerkat.backend.dto.users.PageInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la respuesta paginada de eventos resumidos.
 *
 * <p>Contiene una lista de eventos en resumen y la información de paginación.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventListResponse {

    /** Lista de eventos en resumen. */
    private List<EventSummaryResponse> content;

    /** Información de paginación. */
    private PageInfo page;
}
