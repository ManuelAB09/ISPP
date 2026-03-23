package es.us.meerkat.backend.repository;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import es.us.meerkat.backend.entity.DisponibilidadTutor;

/** Repositorio JPA para la entidad {@link DisponibilidadTutor}. */
public interface DisponibilidadTutorRepository extends JpaRepository<DisponibilidadTutor, Long> {

    /** Obtiene todas las disponibilidades activas de un tutor. */
    List<DisponibilidadTutor> findByTutorIdAndActivaTrue(Long tutorId);

    /** Obtiene disponibilidades de un tutor para un día específico. */
    @Query(
            """
            SELECT d FROM DisponibilidadTutor d
            WHERE d.tutor.id = :tutorId
            AND d.activa = true
            AND (
                (d.esRecurrente = true AND d.diaSemana = :dayOfWeek)
                OR (d.esRecurrente = false AND CAST(d.fechaPuntual AS date) = :fecha)
            )
            ORDER BY d.horaInicio ASC
            """)
    List<DisponibilidadTutor> findDisponibilidadesPara(
            @Param("tutorId") Long tutorId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("fecha") java.time.LocalDate fecha);

    /** Busca disponibilidades que contengan una hora específica. */
    @Query(
            """
            SELECT d FROM DisponibilidadTutor d
            WHERE d.tutor.id = :tutorId
            AND d.activa = true
            AND d.diaSemana = :dayOfWeek
            AND d.horaInicio <= :hora
            AND d.horaFin > :hora
            """)
    List<DisponibilidadTutor> findDisponibilidadesQueContienenHora(
            @Param("tutorId") Long tutorId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("hora") java.time.LocalTime hora);

    /** Obtiene todas las disponibilidades recurrentes de un tutor. */
    List<DisponibilidadTutor> findByTutorIdAndActivaTrueAndEsRecurrenteTrue(Long tutorId);

    /** Obtiene disponibilidades puntuales dentro de un rango de fechas. */
    @Query(
            """
            SELECT d FROM DisponibilidadTutor d
            WHERE d.tutor.id = :tutorId
            AND d.activa = true
            AND d.esRecurrente = false
            AND d.fechaPuntual BETWEEN :desde AND :hasta
            ORDER BY d.fechaPuntual ASC
            """)
    List<DisponibilidadTutor> findDisponibilidadesPuntualesMiPeriodo(
            @Param("tutorId") Long tutorId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    /** Elimina todas las disponibilidades de un tutor. */
    void deleteAllByTutorId(Long tutorId);
}
