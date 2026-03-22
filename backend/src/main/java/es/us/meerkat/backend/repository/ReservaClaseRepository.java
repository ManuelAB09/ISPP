package es.us.meerkat.backend.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import es.us.meerkat.backend.entity.EstadoReserva;
import es.us.meerkat.backend.entity.ReservaClase;

/** Repositorio JPA para la entidad {@link ReservaClase}. */
public interface ReservaClaseRepository extends JpaRepository<ReservaClase, Long> {

    /** Obtiene todas las reservas de un alumno. */
    Page<ReservaClase> findByAlumnoId(Long alumnoId, Pageable pageable);

    /** Obtiene todas las reservas de un tutor. */
    Page<ReservaClase> findByTutorIdAndEstado(
            Long tutorId, EstadoReserva estado, Pageable pageable);

    /** Obtiene todas las reservas de un tutor sin filtrar por estado. */
    Page<ReservaClase> findByTutorId(Long tutorId, Pageable pageable);

    /** Busca reservas solapadas en un horario. */
    @Query(
            value =
                    """
                    SELECT r.* FROM reserva_clase r
                    WHERE r.tutor_id = :tutorId
                    AND r.estado NOT IN ('CANCELADA_ALUMNO', 'CANCELADA_TUTOR', 'NO_ASISTIDA')
                    AND r.fecha_hora <= :fechaHoraFin
                    AND r.fecha_hora + (r.duracion_minutos * INTERVAL '1 minute') >= :fechaHora
                    """,
            nativeQuery = true)
    List<ReservaClase> findReservasConflictivas(
            @Param("tutorId") Long tutorId,
            @Param("fechaHora") LocalDateTime fechaHora,
            @Param("fechaHoraFin") LocalDateTime fechaHoraFin);

    /** Obtiene reservas próximas (no completadas, no canceladas). */
    @Query(
            """
            SELECT r FROM ReservaClase r
            WHERE r.tutor.id = :tutorId
            AND r.estado IN (es.us.meerkat.backend.entity.EstadoReserva.PENDIENTE,
                             es.us.meerkat.backend.entity.EstadoReserva.CONFIRMADA)
            AND r.fechaHora BETWEEN :desde AND :hasta
            ORDER BY r.fechaHora ASC
            """)
    List<ReservaClase> findReservasProximas(
            @Param("tutorId") Long tutorId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    /** Obtiene reservas completadas de un alumno con un tutor (para reseñas). */
    @Query(
            """
            SELECT r FROM ReservaClase r
            WHERE r.alumno.id = :alumnoId
            AND r.tutor.id = :tutorId
            AND r.estado = es.us.meerkat.backend.entity.EstadoReserva.COMPLETADA
            """)
    List<ReservaClase> findReservasCompletadasAlumnoTutor(
            @Param("alumnoId") Long alumnoId, @Param("tutorId") Long tutorId);

    /** Cuenta cuántas reservas confirmadas tiene un tutor en un período. */
    @Query(
            """
            SELECT COUNT(r) FROM ReservaClase r
            WHERE r.tutor.id = :tutorId
            AND r.estado = es.us.meerkat.backend.entity.EstadoReserva.CONFIRMADA
            AND r.fechaHora BETWEEN :desde AND :hasta
            """)
    long countReservasConfirmadas(
            @Param("tutorId") Long tutorId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    /** Obtiene reservas de un alumno con estado específico. */
    Page<ReservaClase> findByAlumnoIdAndEstado(
            Long alumnoId, EstadoReserva estado, Pageable pageable);
}
