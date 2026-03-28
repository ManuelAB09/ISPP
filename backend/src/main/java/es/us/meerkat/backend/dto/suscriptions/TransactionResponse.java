package es.us.meerkat.backend.dto.suscriptions;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import es.us.meerkat.backend.dto.tutors.TutorResponse;
import es.us.meerkat.backend.dto.users.UserPublicResponse;
import es.us.meerkat.backend.entity.EstadoTransaccion;
import es.us.meerkat.backend.entity.TipoTransaccion;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/** DTO para respuesta de información de transacción de pago. */
@Data
@Builder
@Schema(description = "Información de una transacción de pago")
public class TransactionResponse {

    @Schema(description = "ID de la transacción", example = "1")
    private Long id;

    @Schema(description = "Tipo de transacción")
    private TipoTransaccion tipo;

    @Schema(description = "Monto de la transacción", example = "29.99")
    private BigDecimal monto;

    @Schema(description = "Moneda utilizada", example = "EUR")
    private String moneda;

    @Schema(description = "Comisión aplicada", example = "3.00")
    private BigDecimal comision;

    @Schema(description = "Monto neto después de comisión", example = "26.99")
    private BigDecimal montoNeto;

    @Schema(description = "Estado de la transacción")
    private EstadoTransaccion estado;

    @Schema(description = "Descripción de la transacción")
    private String descripcion;

    @Schema(description = "ID de Stripe (si aplica)")
    private String stripePaymentId;

    @Schema(description = "Fecha y hora de creación")
    private LocalDateTime iniciadoAt;

    @Schema(description = "Fecha y hora de completación")
    private LocalDateTime completadoAt;

    @Schema(description = "Usuario que realizó el pago")
    private UserPublicResponse usuario;

    @Schema(description = "Tutor involucrado (si aplica)")
    private TutorResponse tutor;
}
