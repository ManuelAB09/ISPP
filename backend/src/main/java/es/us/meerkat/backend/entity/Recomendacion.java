package es.us.meerkat.backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad que representa una recomendación personalizada generada por el sistema de IA.
 *
 * <p>Almacena las recomendaciones sugeridas a cada usuario basadas en su actividad y preferencias.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "recomendacion",
        indexes = {
            @Index(name = "idx_recomendacion_usuario", columnList = "usuario_id"),
            @Index(name = "idx_recomendacion_tipo", columnList = "tipo"),
            @Index(name = "idx_recomendacion_vista", columnList = "vista")
        })
public class Recomendacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Usuario para el que se genera la recomendación. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /** Tipo de recomendación. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoRecomendacion tipo;

    /** ID del objeto recomendado (tutorId, comunidadId, etc). */
    @Column(nullable = false)
    private Long idObjetoRecomendado;

    /** Título o nombre de lo recomendado. */
    @Column(nullable = false)
    private String titulo;

    /** Descripción breve. */
    @Column(columnDefinition = "TEXT")
    private String descripcion;

    /** URL de imagen/thumbnail. */
    @Column(columnDefinition = "TEXT")
    private String imagenUrl;

    /** Puntuación de relevancia (0-100). Usada para ordenar recomendaciones. */
    @Column(nullable = false)
    private Double puntuacionRelevancia;

    /** Razón de la recomendación (para mostrar al usuario). */
    @Column(columnDefinition = "TEXT")
    private String razonRecomendacion;

    /** ¿Ha sido vista/mostrada al usuario? */
    @Builder.Default
    @Column(nullable = false)
    private Boolean vista = false;

    /** Fecha en que fue mostrada al usuario. */
    private LocalDateTime fechaVista;

    /** ¿El usuario indicó que fue útil? null = no interactuado, true = útil, false = no útil. */
    private Boolean esFavorable;

    /** Fecha de creación de la recomendación. */
    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Fecha de última actualización. */
    private LocalDateTime updatedAt;

    /** Fecha de expiración de la recomendación. */
    private LocalDateTime fechaExpiracion;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /** Verifica si la recomendación ha expirado. */
    public boolean estaExpirada() {
        if (fechaExpiracion == null) {
            return false; // Sin expiración
        }
        return LocalDateTime.now().isAfter(fechaExpiracion);
    }
}
