package es.us.meerkat.backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad que representa una grabación de clase vinculada a Google Classroom.
 *
 * <p>Permite almacenar referencias a grabaciones de videoclases que se sincronizan con Google
 * Classroom.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "grabacion_clase")
public class GrabacionClase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Evento o clase de la que proviene la grabación. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id", nullable = true)
    private Evento evento;

    /** Comunidad donde se realizó la clase. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comunidad_id", nullable = false)
    private Comunidad comunidad;

    /** URL de la grabación en el almacenamiento (Google Drive, etc). */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String urlGrabacion;

    /** ID del archivo en Google Drive. */
    private String googleDriveFileId;

    /** Título de la grabación. */
    @Column(nullable = false)
    private String titulo;

    /** Descripción de la grabación. */
    @Column(columnDefinition = "TEXT")
    private String descripcion;

    /** Duración en segundos. */
    private Long durationSeconds;

    /** ¿Está compartida en Classroom? */
    @Builder.Default
    @Column(nullable = false)
    private Boolean sincronizadoClassroom = false;

    /** ID del recurso en Classroom. */
    private String classroomResourceId;

    /** Estado de la grabación (DISPONIBLE, PROCESANDO, ERROR). */
    @Column(nullable = false)
    private String estado = "DISPONIBLE";

    /** Miembro que subió la grabación. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subido_por_id")
    private Usuario subidoPor;

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
