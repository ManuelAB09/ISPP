package es.us.meerkat.backend.entity.recommendations;

import java.time.LocalDateTime;

import es.us.meerkat.backend.entity.users.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad que registra el feedback del usuario sobre una recomendación.
 *
 * <p>Permite mejorar el algoritmo de recomendación basándose en si el usuario encontró útil o no la
 * sugerencia.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "feedback_recomendacion",
        indexes = {
            @Index(name = "idx_feedback_recomendacion", columnList = "recomendacion_id"),
            @Index(name = "idx_feedback_usuario", columnList = "usuario_id")
        })
public class FeedbackRecomendacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** La recomendación sobre la que se da feedback. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recomendacion_id", nullable = false)
    private Recomendacion recomendacion;

    /** Usuario que da el feedback. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /** ¿Le fue útil la recomendación? true = útil, false = no útil. */
    @Column(nullable = false)
    private Boolean esUtil;

    /** Comentario opcional del usuario. */
    @Column(columnDefinition = "TEXT")
    private String comentario;

    /** Rating de satisfacción (1-5) si aplica. */
    private Integer satisfaccion;

    /** Fecha de creación del feedback. */
    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        // Auto
    }
}
