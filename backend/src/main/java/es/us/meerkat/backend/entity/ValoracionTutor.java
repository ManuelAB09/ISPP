package es.us.meerkat.backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

/**
 * Valoración de un tutor por parte de un usuario. Usada por el motor de recomendaciones para
 * calcular la valoración media y priorizar tutores bien valorados por otros usuarios.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "valoracion_tutor",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tutor_id", "usuario_id"}))
public class ValoracionTutor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Tutor valorado. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id", nullable = false)
    private Tutor tutor;

    /** Usuario que hace la valoración. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /** Puntuación de 1 a 5. */
    @Min(1)
    @Max(5)
    @Column(nullable = false)
    private Integer puntuacion;

    /** Comentario opcional. */
    @Column(columnDefinition = "TEXT")
    private String comentario;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
