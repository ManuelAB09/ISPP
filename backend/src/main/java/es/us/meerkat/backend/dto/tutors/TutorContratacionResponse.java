package es.us.meerkat.backend.dto.tutors;

import java.math.BigDecimal;
import java.time.LocalDate;

import es.us.meerkat.backend.dto.communities.CommunityDetailResponse;
import es.us.meerkat.backend.dto.subscriptions.TransactionResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/** DTO para respuesta de información de contratación de tutor. */
@Data
@Builder
@Schema(description = "Información de una contratación de tutor")
public class TutorContratacionResponse {

    @Schema(description = "ID de la contratación", example = "1")
    private Long id;

    @Schema(description = "Información del tutor contratado")
    private TutorResponse tutor;

    @Schema(description = "Información de la comunidad")
    private CommunityDetailResponse comunidad;

    @Schema(description = "Modalidad de contratación", example = "mensual")
    private String modalidad;

    @Schema(description = "Duración de la contratación", example = "3 meses")
    private String duracion;

    @Schema(description = "Tarifa acordada", example = "300.00")
    private BigDecimal tarifaAcordada;

    @Schema(description = "Estado de la contratación")
    private String estado;

    @Schema(description = "Fecha de inicio")
    private LocalDate fechaInicio;

    @Schema(description = "Fecha de fin")
    private LocalDate fechaFin;

    @Schema(description = "Detalles de la transacción de pago")
    private TransactionResponse transaccion;
}
