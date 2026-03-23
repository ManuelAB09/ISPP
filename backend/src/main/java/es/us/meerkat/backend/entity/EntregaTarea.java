package es.us.meerkat.backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Entidad que representa la entrega de una tarea por parte de un alumno. */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "entrega_tarea")
public class EntregaTarea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Tarea a la que corresponde. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tarea_id", nullable = false)
    private TareaClassroom tarea;

    /** Alumno que entrega. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alumno_id", nullable = false)
    private Usuario alumno;

    /** URL del archivo entregado (Drive, etc). */
    @Column(columnDefinition = "TEXT")
    private String urlArchivo;

    /** Contenido si es texto directo. */
    @Column(columnDefinition = "TEXT")
    private String contenido;

    /** Estado (ABIERTA, ENTREGADA, TARDÍA, CALIFICADA). */
    @Column(nullable = false)
    private String estado = "ABIERTA";

    /** Calificación dada por el profesor. */
    private Integer calificacion;

    /** Comentario de retroalimentación. */
    @Column(columnDefinition = "TEXT")
    private String comentarioRetroalimentacion;

    /** ID de la entrega en Google Classroom. */
    private String classroomSubmissionId;

    /** Fecha de creación/entrega. */
    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Fecha de calificación. */
    private LocalDateTime fechaCalificacion;

    /** ¿Está sincronizada con Classroom? */
    @Builder.Default
    @Column(nullable = false)
    private Boolean sincronizadoClassroom = false;

    @PreUpdate
    protected void onUpdate() {
        // Auto
    }
}
