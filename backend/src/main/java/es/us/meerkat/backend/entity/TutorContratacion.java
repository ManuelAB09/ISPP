package es.us.meerkat.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad que representa una contratación de tutor por parte de una comunidad.
 *
 * <p>Registra la relación entre una comunidad y un tutor contratado, incluyendo detalles de la
 * contratación como duración, tarifa y estado del pago.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "tutor_contratacion")
public class TutorContratacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Tutor contratado. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id", nullable = false)
    private Tutor tutor;

    /** Comunidad que contrata al tutor. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comunidad_id", nullable = false)
    private Comunidad comunidad;

    /** Modalidad de contratación (horaria, mensual, etc). */
    @Column(nullable = false)
    private String modalidad;

    /** Duración de la contratación (en semanas, meses, etc). */
    @Column(nullable = false)
    private String duracion;

    /** Tarifa acordada. */
    @Column(nullable = false)
    private BigDecimal tarifaAcordada;

    /** Estado de la contratación. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoContratacion estado = EstadoContratacion.ACTIVA;

    /** Transacción de pago asociada. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaccion_id")
    private TransaccionPago transaccion;

    /** Fecha de inicio de la contratación. */
    @Column(nullable = false)
    private LocalDate fechaInicio;

    /** Fecha de fin de la contratación. */
    @Column(nullable = false)
    private LocalDate fechaFin;

    /** Fecha de creación del registro. */
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Fecha de actualización. */
    private LocalDateTime updatedAt;

    /** Motivo de cancelación si aplica. */
    @Column(length = 500)
    private String motivoCancelacion;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
