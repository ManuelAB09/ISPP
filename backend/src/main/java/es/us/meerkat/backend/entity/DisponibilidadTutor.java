package es.us.meerkat.backend.entity;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad que representa una franja horaria de disponibilidad de un tutor.
 *
 * <p>Permite a los tutores definir sus horarios disponibles para reservas de clases. Puede ser
 * recurrente (ej: "Todos los lunes de 10:00 a 14:00") o puntual.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "disponibilidad_tutor",
        indexes = {@Index(name = "idx_tutor_disponibilidad", columnList = "tutor_id")})
public class DisponibilidadTutor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Tutor propietario de esta disponibilidad. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id", nullable = false)
    private Tutor tutor;

    /** Día de la semana si es recurrente (LUNES, MARTES, etc) o null si es puntual. */
    @Enumerated(EnumType.STRING)
    private DayOfWeek diaSemana;

    /** Hora de inicio (HH:mm). */
    @Column(nullable = false)
    private LocalTime horaInicio;

    /** Hora de fin (HH:mm). */
    @Column(nullable = false)
    private LocalTime horaFin;

    /** Es una disponibilidad recurrente (igual franja cada semana) o puntual. */
    @Column(nullable = false)
    private Boolean esRecurrente;

    /** Si no es recurrente, la fecha específica de disponibilidad. */
    private LocalDateTime fechaPuntual;

    /** Indica si esta franja está activa. */
    @Builder.Default
    @Column(nullable = false)
    private Boolean activa = true;

    /** Modalidad de clase (PRESENCIAL, VIRTUAL, HIBRIDA). */
    @Column(nullable = false)
    @lombok.Builder.Default
    private String modalidad = "VIRTUAL";

    /** Ubicación si es presencial. */
    private String ubicacionPresencial;

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

    /**
     * Verifica si dos disponibilidades se solapan.
     *
     * @param otra Otra franja de disponibilidad
     * @return true si se solapan
     */
    public boolean solapaCon(DisponibilidadTutor otra) {
        // Si no son el mismo día no solapan
        if (!this.diaSemana.equals(otra.diaSemana)) {
            return false;
        }
        // Solapan si una empieza antes de que termine la otra
        return this.horaInicio.isBefore(otra.horaFin) && this.horaFin.isAfter(otra.horaInicio);
    }
}
