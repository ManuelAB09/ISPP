package es.us.meerkat.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad que representa una reserva de clase o servicio directo con un tutor.
 *
 * <p>Registra toda la información de la reserva: quién (alumno/tutor), cuándo, dónde, estado del
 * pago, cancelaciones, etc.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "reserva_clase",
        indexes = {
            @Index(name = "idx_reserva_alumno", columnList = "alumno_id"),
            @Index(name = "idx_reserva_tutor", columnList = "tutor_id"),
            @Index(name = "idx_reserva_estado", columnList = "estado"),
            @Index(name = "idx_reserva_fecha", columnList = "fechaHora")
        })
public class ReservaClase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Alumno que hace la reserva. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alumno_id", nullable = false)
    private Usuario alumno;

    /** Tutor con el que se reserva. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id", nullable = false)
    private Tutor tutor;

    /** Fecha y hora de la clase reservada. */
    @Column(nullable = false)
    private LocalDateTime fechaHora;

    /** Duración de la clase en minutos. */
    @Column(nullable = false)
    private Integer duracionMinutos = 60;

    /** Modalidad de la clase (PRESENCIAL, VIRTUAL, HIBRIDA). */
    @Column(nullable = false)
    private String modalidad;

    /** Ubicación si es presencial o hibrida. */
    private String ubicacion;

    /** URL de la sala virtual si es virtual o hibrida. */
    private String enlaceVirtual;

    /** Materia o tema de la clase. */
    @Column(length = 500)
    private String tema;

    /** Descripción adicional de la clase por parte del alumno. */
    @Column(columnDefinition = "TEXT")
    private String descripcion;

    /** Tarifa acordada para esta reserva. */
    @Column(nullable = false)
    private BigDecimal tarifa;

    /** Moneda de la tarifa (EUR, USD, etc). */
    @Column(nullable = false)
    private String moneda = "EUR";

    /** Estado actual de la reserva. */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoReserva estado = EstadoReserva.PENDIENTE;

    /** Transacción de pago asociada. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaccion_id")
    private TransaccionPago transaccion;

    /** Calificación que el alumno da después de la clase (1-5). */
    private Integer calificacion;

    /** Comentario del alumno después de la clase. */
    @Column(columnDefinition = "TEXT")
    private String comentarioAlumno;

    /** Calificación que el tutor da al alumno (1-5). */
    private Integer calificacionTutor;

    /** Comentario del tutor. */
    @Column(columnDefinition = "TEXT")
    private String comentarioTutor;

    /** Por qué fue cancelada (si aplica). */
    @Column(columnDefinition = "TEXT")
    private String motivoCancelacion;

    /** Quién canceló (ALUMNO o TUTOR). */
    private String quienCancelo;

    /** Grabación de la clase si es virtual. */
    @Column(columnDefinition = "TEXT")
    private String urlGrabacion;

    /** Notas de la tutela (para el tutor). */
    @Column(columnDefinition = "TEXT")
    private String notasTutela;

    /** Fecha de creación de la reserva. */
    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Fecha de última actualización. */
    private LocalDateTime updatedAt;

    /** Fecha de confirmación por el tutor. */
    private LocalDateTime fechaConfirmacion;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Verifica si la reserva ya pasó.
     *
     * @return true si la fecha/hora de la reserva es anterior a ahora
     */
    public boolean yaEnPasado() {
        return fechaHora.isBefore(LocalDateTime.now());
    }

    /**
     * Verifica si es posible cancelar esta reserva (generalmente, debe ser con anticipación).
     *
     * @param minutosAnticipaion minutos antes de la clase para permitir cancelación
     * @return true si se puede cancelar
     */
    public boolean puedeSerCancelada(Integer minutosAnticipaion) {
        if (yaEnPasado()) {
            return false;
        }
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime limiteParaCancelar = fechaHora.minusMinutes(minutosAnticipaion);
        return ahora.isBefore(limiteParaCancelar);
    }
}
