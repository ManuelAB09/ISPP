package es.us.meerkat.backend.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    /** Indica si el cuestionario está publicado (`true`) o en borrador (`false`). */
    @Column(nullable = false)
    @Builder.Default
    private Boolean publicado = false;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /** Preguntas creadas manualmente por usuarios (test, verdadero/falso, respuesta corta) */
    @OneToMany(
            mappedBy = "cuestionario",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER)
    @Builder.Default
    private List<Pregunta> preguntas = new ArrayList<>();

    /** Comunidades asociadas a este cuestionario. Puede pertenecer a varias comunidades. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "cuestionario_comunidades",
            joinColumns = @JoinColumn(name = "cuestionario_id"),
            inverseJoinColumns = @JoinColumn(name = "comunidad_id"))
    @Builder.Default
    private Set<Comunidad> comunidades = new HashSet<>();

    /** Alumnos asignados o invitados a este cuestionario. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "cuestionario_alumnos",
            joinColumns = @JoinColumn(name = "cuestionario_id"),
            inverseJoinColumns = @JoinColumn(name = "usuario_id"))
    @Builder.Default
    private Set<Usuario> alumnos = new HashSet<>();

    /** Usuario que creo el cuestionario. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creador_id")
    private Usuario creador;
}
