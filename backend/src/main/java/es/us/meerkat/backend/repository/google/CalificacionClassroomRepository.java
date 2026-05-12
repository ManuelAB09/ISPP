package es.us.meerkat.backend.repository.google;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import es.us.meerkat.backend.entity.google.CalificacionClassroom;

/** Repositorio JPA para la entidad {@link CalificacionClassroom}. */
public interface CalificacionClassroomRepository
        extends JpaRepository<CalificacionClassroom, Long> {

    /** Obtiene la calificación de un alumno en una tarea. */
    Optional<CalificacionClassroom> findByTareaIdAndAlumnoId(Long tareaId, Long alumnoId);

    /** Obtiene todas las calificaciones de una tarea. */
    List<CalificacionClassroom> findByTareaIdOrderByAlumnoId(Long tareaId);

    /** Obtiene todas las calificaciones de un alumno en una comunidad. */
    @Query(
            """
            SELECT c FROM CalificacionClassroom c
            WHERE c.tarea.comunidad.id = :comunidadId
            AND c.alumno.id = :alumnoId
            ORDER BY c.tarea.fechaVencimiento DESC
            """)
    List<CalificacionClassroom> findCalificacionesAlumnoEnComunidad(
            @Param("comunidadId") Long comunidadId, @Param("alumnoId") Long alumnoId);

    /** Obtiene calificaciones no sincronizadas. */
    @Query(
            """
            SELECT c FROM CalificacionClassroom c
            WHERE c.sincronizada = false
            AND c.tarea.comunidad.id = :comunidadId
            ORDER BY c.updatedAt ASC
            """)
    List<CalificacionClassroom> findNoSincronizadas(@Param("comunidadId") Long comunidadId);

    /** Cuenta calificaciones en una tarea. */
    long countByTareaId(Long tareaId);

    /** Elimina todas las calificaciones de tareas de una comunidad (bulk). */
    @Modifying
    @Query("DELETE FROM CalificacionClassroom c WHERE c.tarea.comunidad.id = :comunidadId")
    void deleteByComunidadId(@Param("comunidadId") Long comunidadId);
}
