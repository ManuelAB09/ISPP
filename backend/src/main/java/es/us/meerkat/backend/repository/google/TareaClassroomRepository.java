package es.us.meerkat.backend.repository.google;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import es.us.meerkat.backend.entity.google.TareaClassroom;

/** Repositorio JPA para la entidad {@link TareaClassroom}. */
public interface TareaClassroomRepository extends JpaRepository<TareaClassroom, Long> {

    /** Obtiene todas las tareas de una comunidad. */
    List<TareaClassroom> findByComunidadIdOrderByFechaVencimientoAsc(Long comunidadId);

    /** Obtiene tareas activas (estado ABIERTA). */
    List<TareaClassroom> findByComunidadIdAndEstadoOrderByFechaVencimientoAsc(
            Long comunidadId, String estado);

    /** Obtiene tareas por rango de fecha de vencimiento. */
    @Query(
            """
            SELECT t FROM TareaClassroom t
            WHERE t.comunidad.id = :comunidadId
            AND t.fechaVencimiento BETWEEN :desde AND :hasta
            ORDER BY t.fechaVencimiento ASC
            """)
    List<TareaClassroom> findTareasPorPeriodo(
            @Param("comunidadId") Long comunidadId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    /** Obtiene una tarea por su ID en Google Classroom. */
    Optional<TareaClassroom> findByClassroomCourseWorkId(String classroomCourseWorkId);

    /** Cuenta tareas en una comunidad. */
    long countByComunidadId(Long comunidadId);

    /** Obtiene tareas próximas a vencer. */
    @Query(
            """
            SELECT t FROM TareaClassroom t
            WHERE t.comunidad.id = :comunidadId
            AND t.estado = 'ABIERTA'
            AND t.fechaVencimiento <= :fecha
            ORDER BY t.fechaVencimiento ASC
            """)
    List<TareaClassroom> findTareasProximas(
            @Param("comunidadId") Long comunidadId, @Param("fecha") LocalDateTime fecha);

    /** Elimina todas las tareas de una comunidad (bulk). */
    @Modifying
    @Query("DELETE FROM TareaClassroom t WHERE t.comunidad.id = :comunidadId")
    void deleteByComunidadId(@Param("comunidadId") Long comunidadId);
}
