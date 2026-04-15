package es.us.meerkat.backend.entity.tutors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import es.us.meerkat.backend.entity.users.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad que representa una solicitud de contratación directa de un tutor por parte de un alumno.
 * El alumno elige día y rango horario, y el tutor decide si acepta o rechaza.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "solicitud_contratacion_directa")
public class SolicitudContratacionDirecta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Alumno que solicita la contratación. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alumno_id", nullable = false)
    private Usuario alumno;

    /** Tutor al que se solicita. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id", nullable = false)
    private Tutor tutor;

    /** Día de la clase solicitada. */
    @Column(nullable = false)
    private LocalDate dia;

    /** Día original de la clase (no cambia al reprogramar). */
    private LocalDate diaOriginal;

    /** Hora de inicio. */
    @Column(nullable = false)
    private LocalTime horaInicio;

    /** Hora de fin. */
    @Column(nullable = false)
    private LocalTime horaFin;

    /** Tarifa por hora del tutor en el momento de la solicitud. */
    @Column(nullable = false)
    private BigDecimal tarifaHora;

    /** Importe total calculado (tarifaHora * horas). */
    @Column(nullable = false)
    private BigDecimal importeTotal;

    /** Modalidad de la clase: ONLINE, PRESENCIAL o HIBRIDO. */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String modalidad = "ONLINE";

    /** Mensaje opcional del alumno al tutor. */
    @Column(length = 500)
    private String mensaje;

    /** Estado de la solicitud. */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoSolicitudContratacion estado = EstadoSolicitudContratacion.PENDIENTE;

    /** Motivo de rechazo si aplica. */
    @Column(length = 500)
    private String motivoRechazo;

    /** ID del PaymentIntent de Stripe (se guarda al confirmar pago para poder reembolsar). */
    @Column(length = 100)
    private String stripePaymentIntentId;

    /** Ubicación elegida por el alumno para clases presenciales (dirección o coordenadas). */
    @Column(length = 500)
    private String ubicacionClase;

    /** URL de Zoom para que el alumno se una a la clase (join URL). */
    @Column(length = 2048)
    private String zoomJoinUrl;

    /** URL de Zoom para que el tutor inicie la clase (start URL). */
    @Column(length = 2048)
    private String zoomStartUrl;

    /** Calificación que el alumno da después de la clase (1-5). */
    private Integer calificacion;

    /** Comentario del alumno después de la clase. */
    @Column(columnDefinition = "TEXT")
    private String comentarioAlumno;

    /** Estado anterior antes de REPROGRAMACION_PENDIENTE (para poder restaurar). */
    @Enumerated(EnumType.STRING)
    private EstadoSolicitudContratacion estadoAnterior;

    /** Día propuesto para reprogramación (pendiente de aprobación del alumno). */
    private LocalDate reprogramacionDia;

    /** Hora inicio propuesta para reprogramación. */
    private LocalTime reprogramacionHoraInicio;

    /** Hora fin propuesta para reprogramación. */
    private LocalTime reprogramacionHoraFin;

    /** Fecha de creación. */
    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Fecha de última actualización. */
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /** Verifica si la fecha/hora de la clase ya pasó. */
    public boolean yaEnPasado() {
        return LocalDateTime.of(dia, horaInicio).isBefore(LocalDateTime.now());
    }

    /** Verifica si se puede cancelar (estado adecuado + 24h de antelación). */
    public boolean puedeSerCanceladaPorAlumno() {
        if (estado != EstadoSolicitudContratacion.PENDIENTE
                && estado != EstadoSolicitudContratacion.ACEPTADA
                && estado != EstadoSolicitudContratacion.PAGADA) {
            return false;
        }
        if (yaEnPasado()) {
            return false;
        }
        LocalDateTime limite = LocalDateTime.of(dia, horaInicio).minusHours(24);
        return LocalDateTime.now().isBefore(limite);
    }
}
