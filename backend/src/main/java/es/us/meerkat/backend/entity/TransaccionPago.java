package es.us.meerkat.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad que representa una transacción de pago dentro de la plataforma.
 *
 * <p>Contiene información sobre el tipo de transacción, monto, moneda, estado, comisiones, fechas y
 * relaciones con usuario y tutor.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransaccionPago {

    /** Identificador único de la transacción. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Tipo de transacción (ej. pago verificación, pago tutor, suscripción). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoTransaccion tipo;

    /** Monto de la transacción. */
    @Column(nullable = false)
    private BigDecimal monto;

    /** Moneda utilizada en la transacción (ej: "USD", "EUR"). */
    @Column(nullable = false)
    private String moneda;

    /** Comisión aplicada, si corresponde. */
    private BigDecimal comision;

    /** Estado actual de la transacción (pendiente, completada, fallida). */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoTransaccion estado = EstadoTransaccion.PENDIENTE;

    /** Fecha y hora en que se inició la transacción. */
    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime iniciadoAt = LocalDateTime.now();

    /** Fecha y hora en que se completó o falló la transacción. */
    private LocalDateTime completadoAt;

    /** Usuario que realiza el pago. */
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /** Tutor involucrado en la transacción, si aplica. */
    @ManyToOne
    @JoinColumn(name = "tutor_id")
    private Tutor tutor;
}
