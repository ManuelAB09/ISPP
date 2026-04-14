package es.us.meerkat.backend.entity.google;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.*;

/**
 * Recurso educativo de la plataforma. Puede ser un vídeo, artículo, documento o cualquier material
 * educativo.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "contenido",
        indexes = {
            @Index(name = "idx_contenido_materia", columnList = "materia"),
            @Index(name = "idx_contenido_tipo", columnList = "tipo"),
            @Index(name = "idx_contenido_activo", columnList = "activo")
        })
public class Contenido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Título del contenido. */
    @Column(nullable = false)
    private String titulo;

    /** Descripción o resumen del contenido. */
    @Column(columnDefinition = "TEXT")
    private String descripcion;

    /** URL del recurso (vídeo, artículo, PDF…). */
    @Column(columnDefinition = "TEXT")
    private String url;

    /** URL de la imagen/thumbnail. */
    @Column(columnDefinition = "TEXT")
    private String imagenUrl;

    /** Tipo de contenido. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoContenido tipo;

    /** Materia principal a la que pertenece. */
    @Column(nullable = false)
    private String materia;

    /**
     * Etiquetas adicionales para mejorar la búsqueda y recomendación. Ej: ["álgebra", "ecuaciones",
     * "bachillerato"]
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "contenido_tags", joinColumns = @JoinColumn(name = "contenido_id"))
    @Column(name = "tag")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    /** Nivel educativo al que va dirigido. */
    private String nivelEducativo;

    /** Autor o fuente del contenido. */
    private String autor;

    /** Duración en minutos (para vídeos). */
    private Integer duracionMinutos;

    /** Número de visualizaciones. */
    @Builder.Default private Long visualizaciones = 0L;

    /** Indica si está activo y visible para los usuarios. */
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
