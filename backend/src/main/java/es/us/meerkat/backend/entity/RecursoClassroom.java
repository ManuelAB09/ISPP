package es.us.meerkat.backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad que representa un recurso educativo (documento, presentación, enlace, etc) vinculado a
 * una comunidad/classroom.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "recurso_classroom")
public class RecursoClassroom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Comunidad donde se publica el recurso. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comunidad_id", nullable = false)
    private Comunidad comunidad;

    /** Tipo de recurso (DOCUMENTO, PRESENTACION, VIDEO, ENLACE, ARCHIVO). */
    @Column(nullable = false)
    private String tipo;

    /** Título del recurso. */
    @Column(nullable = false)
    private String titulo;

    /** Descripción. */
    @Column(columnDefinition = "TEXT")
    private String descripcion;

    /** URL del recurso. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    /** ID en Google Drive (si aplica). */
    private String googleDriveFileId;

    /** Miembro que subió el recurso. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subido_por_id", nullable = false)
    private Usuario subidoPor;

    /** ¿Está compartido en Classroom? */
    @Builder.Default
    @Column(nullable = false)
    private Boolean sincronizadoClassroom = false;

    /** ID del recurso en Classroom. */
    private String classroomResourceId;

    /** ¿Está visible para los alumnos? */
    @Builder.Default
    @Column(nullable = false)
    private Boolean visible = true;

    /** Orden de visualización. */
    private Integer orden;

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
}
