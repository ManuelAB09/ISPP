package es.us.meerkat.backend.entity.google;

import java.time.LocalDateTime;

import es.us.meerkat.backend.entity.users.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad que representa la sincronización de calificaciones entre la plataforma y Google
 * Classroom.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "calificacion_classroom")
public class CalificacionClassroom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** La tarea a la que corresponde la calificación. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tarea_id", nullable = false)
    private TareaClassroom tarea;

    /** El alumno calificado. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alumno_id", nullable = false)
    private Usuario alumno;

    /** Calificación en la plataforma. */
    @Column(nullable = false)
    private Integer calificacionPlataforma;

    /** Calificación en Classroom (puede estar sincronizada). */
    private Integer calificacionClassroom;

    /** ¿Un valor es más reciente? PLATAFORMA, CLASSROOM. */
    private String fuenteMasReciente;

    /** Comentario del profesor. */
    @Column(columnDefinition = "TEXT")
    private String comentario;

    /** ¿Está sincronizada bidireccionalmentemente? */
    @Builder.Default
    @Column(nullable = false)
    private Boolean sincronizada = false;

    /** Fecha de última sincronización. */
    private LocalDateTime fechaUltimaSincronizacion;

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
