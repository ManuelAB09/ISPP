package es.us.meerkat.backend.entity.google;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import es.us.meerkat.backend.entity.communities.Comunidad;
import es.us.meerkat.backend.entity.tutors.EntregaTarea;
import es.us.meerkat.backend.entity.users.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad que representa una tarea sincronizada desde/hacia Google Classroom.
 *
 * <p>Permite crear y gestionar tareas que se sincronizan con Google Classroom.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "tarea_classroom")
public class TareaClassroom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Comunidad/curso al que pertenece la tarea. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comunidad_id", nullable = false)
    private Comunidad comunidad;

    /** Título de la tarea. */
    @Column(nullable = false)
    private String titulo;

    /** Descripción detallada de la tarea. */
    @Column(columnDefinition = "TEXT")
    private String descripcion;

    /** Fecha de entrega. */
    @Column(nullable = false)
    private LocalDateTime fechaVencimiento;

    /** Puntuación máxima posible. */
    @lombok.Builder.Default private Integer puntosMaximos = 100;

    /** ¿Está publicada en Classroom? */
    @Builder.Default
    @Column(nullable = false)
    private Boolean publicada = false;

    /** ID de la tarea en Google Classroom. */
    private String classroomCourseWorkId;

    /** Miembro que creó la tarea. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creada_por_id", nullable = false)
    private Usuario creadaPor;

    /** Entregas/calificaciones de la tarea. */
    @OneToMany(mappedBy = "tarea", cascade = CascadeType.ALL)
    @Builder.Default
    private List<EntregaTarea> entregas = new ArrayList<>();

    /** Estado (ABIERTA, CERRADA, CANCELADA). */
    @Column(nullable = false)
    @lombok.Builder.Default
    private String estado = "ABIERTA";

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
