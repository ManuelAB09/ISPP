package es.us.meerkat.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Representa la asociación entre una comunidad y una clase de Google Classroom. */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComunidadClassroom {

    /** Identificador único */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Comunidad asociada */
    @OneToOne
    @JoinColumn(name = "comunidad_id", nullable = false, unique = true)
    private Comunidad comunidad;

    /** ID del curso en Google Classroom */
    @Column(nullable = false)
    private String classroomCourseId;

    /** Nombre del curso (para mostrar sin llamar a la API siempre) */
    @Column(nullable = false)
    private String classroomCourseName;

    /** Indica si la vinculación está activa */
    @Builder.Default
    @Column(nullable = false)
    private Boolean activa = true;
}
