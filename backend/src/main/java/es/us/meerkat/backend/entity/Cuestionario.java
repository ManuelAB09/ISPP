package es.us.meerkat.backend.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.*;

/**
 * Cuestionario de práctica de la plataforma. Contiene preguntas sobre una materia con nivel de
 * dificultad definido.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "cuestionario",
        indexes = {
            @Index(name = "idx_cuestionario_materia", columnList = "materia"),
            @Index(name = "idx_cuestionario_dificultad", columnList = "dificultad"),
            @Index(name = "idx_cuestionario_activo", columnList = "activo")
        })
public class Cuestionario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Título del cuestionario. */
    @Column(nullable = false)
    private String titulo;

    /** Descripción del cuestionario. */
    @Column(columnDefinition = "TEXT")
    private String descripcion;

    /** URL de imagen/thumbnail. */
    @Column(columnDefinition = "TEXT")
    private String imagenUrl;

    /** Materia principal del cuestionario. */
    @Column(nullable = false)
    private String materia;

    /**
     * Etiquetas adicionales para búsqueda y recomendación. Ej: ["derivadas", "cálculo",
     * "universidad"]
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "cuestionario_tags",
            joinColumns = @JoinColumn(name = "cuestionario_id"))
    @Column(name = "tag")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    /** Nivel de dificultad. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NivelDificultad dificultad;

    /** Nivel educativo al que va dirigido. */
    private String nivelEducativo;

    /** Número de preguntas del cuestionario. */
    @Builder.Default private Integer numPreguntas = 0;

    /** Tiempo estimado en minutos. */
    private Integer tiempoEstimadoMinutos;

    /** Número de veces que ha sido intentado. */
    @Builder.Default private Long intentos = 0L;

    /** Indica si está activo y disponible. */
    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
