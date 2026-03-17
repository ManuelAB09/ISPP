package es.us.meerkat.backend.entity;

import java.time.LocalDate;

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
    @Builder.Default
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
    @Builder.Default
    @Column(nullable = false)
    private Boolean activa = true;

    /** Indica si la renovación automática está habilitada. */
    @Builder.Default
    @Column(nullable = false)
    private Boolean autoRenovar = true;

    /** Periodo de la suscripción (MENSUAL o ANUAL). */
    @Column(length = 10)
    private String periodo;

    /** Suscribe al usuario al plan. */
    public static Suscripcion suscribir(String periodo) {
        int meses = "anual".equalsIgnoreCase(periodo) ? 12 : 1;
        return Suscripcion.builder()
                .activa(true)
                .autoRenovar(true)
                .plan(TipoPlan.PREMIUM)
                .periodo(periodo != null ? periodo.toUpperCase() : "MENSUAL")
                .fechaInicio(LocalDate.now())
                .fechaFin(LocalDate.now().plusMonths(meses))
                .build();
    }

    /** Suscribe al usuario al plan (periodo mensual por defecto). */
    public static Suscripcion suscribir() {
        return suscribir("MENSUAL");
    }

    /** Cancela la suscripción del usuario. */
    public void cancelar() {
        this.activa = false;
        this.autoRenovar = false;
        this.plan = TipoPlan.FREE;
        // NO PUEDE SER NULL (cambiado)
        this.fechaInicio = LocalDate.now();
        this.fechaFin = LocalDate.now();
    }

    /** Renueva la suscripción extendiendo las fechas de vigencia. */
    /** Renueva la suscripción extendiendo las fechas de vigencia. */
    public void renovar() {
        this.plan = TipoPlan.PREMIUM;
        this.activa = true;
        this.autoRenovar = true;

        int meses = "ANUAL".equalsIgnoreCase(this.periodo) ? 12 : 1;

        if (this.estaActiva()) {
            this.fechaInicio = this.fechaFin;
            this.fechaFin = this.fechaFin.plusMonths(meses);
        } else {
            this.fechaInicio = LocalDate.now();
            this.fechaFin = LocalDate.now().plusMonths(meses);
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
                .periodo(this.periodo)
                .fechaInicio(this.fechaInicio)
                .fechaFin(this.fechaFin)
                .activa(this.activa)
                .autoRenovar(this.autoRenovar)
                .enPeriodoGracia(enPeriodoGracia)
                .build();
    }
}
