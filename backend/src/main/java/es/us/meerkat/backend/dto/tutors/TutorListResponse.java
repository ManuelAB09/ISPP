package es.us.meerkat.backend.dto.tutors;

import java.util.List;

import es.us.meerkat.backend.dto.users.PageInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/** DTO para listar tutores con paginación. */
@Data
@Builder
@Schema(description = "Lista paginada de tutores")
public class TutorListResponse {

    @Schema(description = "Lista de tutores")
    private List<TutorProfileResponse> content;

    @Schema(description = "Información de paginación")
    private PageInfo page;
}
