package es.us.meerkat.backend.dto;

import org.springframework.data.domain.Page;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response DTO para lista paginada de solicitudes de contratación. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Lista paginada de solicitudes de contratación")
public class HiringRequestListResponse {

    @Schema(description = "Contenido de la página")
    private java.util.List<HiringRequestResponse> content;

    @Schema(description = "Información de paginación")
    private PageInfo page;

    public HiringRequestListResponse(Page<HiringRequestResponse> page) {
        this.content = page.getContent();
        this.page =
                PageInfo.builder()
                        .number(page.getNumber())
                        .size(page.getSize())
                        .totalElements(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .first(page.isFirst())
                        .last(page.isLast())
                        .build();
    }
}
