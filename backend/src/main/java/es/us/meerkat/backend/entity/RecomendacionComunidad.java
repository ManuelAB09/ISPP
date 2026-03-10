package es.us.meerkat.backend.entity;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad que representa una recomendación de comunidad para un usuario.
 *
 * <p>Las recomendaciones se basan en los intereses, perfil académico, comunidades a las que ya
 * pertenece el usuario y actividad previa. Se personalizan para cada usuario e incluyen comunidades
 * tanto públicas como privadas.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "recomendaciones_comunidad",
        uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "comunidad_id"}))
public class RecomendacionComunidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Usuario al que se recomienda la comunidad. */
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /** Comunidad recomendada. */
    @ManyToOne
    @JoinColumn(name = "comunidad_id", nullable = false)
    private Comunidad comunidad;

    /** Factor por el cual se recomienda la comunidad. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FactorRecomendacion factor;

    /** Puntuación de relevancia de la recomendación (0-100). */
    @Column(nullable = false)
    @Builder.Default
    private Double relevancia = 0.0;

    /** Fecha de creación de la recomendación. */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Indica si el usuario ya vio esta recomendación. */
    @Builder.Default
    @Column(nullable = false)
    private Boolean vista = false;

    /** Fecha en que fue vista la recomendación. */
    @Column private LocalDateTime fechaVista;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.vista == null) {
            this.vista = false;
        }
        if (this.relevancia == null) {
            this.relevancia = 0.0;
        }
    }

    /** Marca la recomendación como vista. */
    public void marcarComoVista() {
        this.vista = true;
        this.fechaVista = LocalDateTime.now();
    }
}
