package es.us.meerkat.backend.repository.tutors;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import es.us.meerkat.backend.entity.tutors.EntregaTarea;

/** Repositorio JPA para la entidad {@link EntregaTarea}. */
public interface EntregaTareaRepository extends JpaRepository<EntregaTarea, Long> {

    /** Obtiene todas las entregas de una tarea. */
    List<EntregaTarea> findByTareaIdOrderByCreatedAtDesc(Long tareaId);

    /** Obtiene la entrega de un alumno en una tarea específica. */
    Optional<EntregaTarea> findByTareaIdAndAlumnoId(Long tareaId, Long alumnoId);

    /** Obtiene entregas pendientes de calificación de una tarea. */
    @Query(
            """
            SELECT e FROM EntregaTarea e
            WHERE e.tarea.id = :tareaId
            AND e.estado IN ('ENTREGADA', 'TARDÍA')
            ORDER BY e.createdAt ASC
            """)
    List<EntregaTarea> findEntregasPendientesDeCalicar(@Param("tareaId") Long tareaId);

    /** Cuenta entregas en una tarea. */
    long countByTareaId(Long tareaId);

    /** Obtiene entregas tardías. */
    List<EntregaTarea> findByTareaIdAndEstado(Long tareaId, String estado);

    /** Obtiene entregas de un alumno. */
    @Query(
            """
            SELECT e FROM EntregaTarea e
            WHERE e.tarea.comunidad.id = :comunidadId
            AND e.alumno.id = :alumnoId
            ORDER BY e.createdAt DESC
            """)
    List<EntregaTarea> findEntregasAlumnoEnComunidad(
            @Param("comunidadId") Long comunidadId, @Param("alumnoId") Long alumnoId);
}
