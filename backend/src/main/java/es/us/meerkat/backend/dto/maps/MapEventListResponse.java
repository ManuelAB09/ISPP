package es.us.meerkat.backend.dto.maps;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la respuesta de eventos en el mapa.
 *
 * <p>Contiene una lista de eventos con información georeferenciada para mostrar en el mapa
 * interactivo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MapEventListResponse {

    /** Lista de eventos georeferenciados. */
    private List<MapEventResponse> content;
}
