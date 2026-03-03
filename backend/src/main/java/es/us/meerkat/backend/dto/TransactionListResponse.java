package es.us.meerkat.backend.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/** DTO para listar transacciones con paginación. */
@Data
@Builder
@Schema(description = "Lista paginada de transacciones")
public class TransactionListResponse {

    @Schema(description = "Lista de transacciones")
    private List<TransactionResponse> content;

    @Schema(description = "Información de paginación")
    private PageInfo page;
}
