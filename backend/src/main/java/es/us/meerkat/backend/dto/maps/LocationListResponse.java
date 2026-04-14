package es.us.meerkat.backend.dto.maps;

import java.util.List;

import es.us.meerkat.backend.dto.users.PageInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la respuesta paginada de ubicaciones.
 *
 * <p>Contiene una lista de ubicaciones recomendadas y la información de paginación.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationListResponse {

    /** Lista de ubicaciones. */
    private List<LocationResponse> content;

    /** Información de paginación. */
    private PageInfo page;
}
