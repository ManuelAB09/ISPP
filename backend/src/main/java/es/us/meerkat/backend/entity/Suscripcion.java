package es.us.meerkat.backend.entity;

import java.time.LocalDate;

import es.us.meerkat.backend.ConstantUtils;
import es.us.meerkat.backend.dto.SubscriptionResponse;
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
 * Entidad que representa una suscripción a un plan de usuario.
 *
 * <p>Contiene información sobre el tipo de plan contratado, fechas de vigencia y estado de la
 * suscripción.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Suscripcion {
    /** Identificador único de la suscripción. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Usuario que posee la suscripción. */
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /** Tipo de plan contratado (FREE o PREMIUM). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoPlan plan = TipoPlan.FREE;

    /** Fecha de inicio de la suscripción. */
    @Column(nullable = false)
    private LocalDate fechaInicio;

    /** Fecha de fin de la suscripción. */
    @Column(nullable = false)
    private LocalDate fechaFin;

    /** Indica si la suscripción está activa. */
    @Column(nullable = false)
    private Boolean activa = true;

    /** Indica si la renovación automática está habilitada. */
    @Column(nullable = false)
    private Boolean autoRenovar = true;

    /** Suscribe al usuario al plan. */
    public static Suscripcion suscribir() {
        return Suscripcion.builder()
                .activa(true)
                .autoRenovar(true)
                .plan(TipoPlan.PREMIUM)
                .fechaInicio(LocalDate.now())
                .fechaFin(LocalDate.now().plusMonths(ConstantUtils.MESES_SUSCRIPCION))
                .build();
    }

    /** Cancela la suscripción del usuario. */
    public void cancelar() {
        this.activa = false;
        this.autoRenovar = false;
        this.plan = TipoPlan.FREE;
        this.fechaInicio = null;
        this.fechaFin = null;
    }

    /** Renueva la suscripción extendiendo las fechas de vigencia. */
    public void renovar() {
        if (this.estaActiva()) {
            this.fechaInicio = this.fechaFin;
            this.fechaFin = this.fechaFin.plusMonths(ConstantUtils.MESES_SUSCRIPCION);
            this.activa = true;
        } else {
            this.fechaInicio = LocalDate.now();
            this.fechaFin = LocalDate.now().plusMonths(ConstantUtils.MESES_SUSCRIPCION);
            this.activa = true;
        }
    }

    /**
     * Verifica si la suscripción está activa en la fecha actual.
     *
     * @return true si la suscripción está dentro del período válido
     */
    public Boolean estaActiva() {
        if (this.fechaInicio == null || this.fechaFin == null) {
            return false;
        }
        LocalDate hoy = LocalDate.now();
        return !hoy.isBefore(this.fechaInicio) && !hoy.isAfter(this.fechaFin);
    }

    public SubscriptionResponse toDTO() {
        boolean enPeriodoGracia =
                !this.activa && this.fechaFin != null && this.fechaFin.isAfter(LocalDate.now());
        return SubscriptionResponse.builder()
                .id(this.id)
                .plan(this.plan)
                .fechaInicio(this.fechaInicio)
                .fechaFin(this.fechaFin)
                .activa(this.activa)
                .autoRenovar(this.autoRenovar)
                .enPeriodoGracia(enPeriodoGracia)
                .build();
    }
}
