package es.us.meerkat.backend.repository.tutors;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import es.us.meerkat.backend.entity.tutors.EstadoSolicitudContratacion;
import es.us.meerkat.backend.entity.tutors.SolicitudContratacionDirecta;

@Repository
public interface SolicitudContratacionDirectaRepository
        extends JpaRepository<SolicitudContratacionDirecta, Long> {

    /** Solicitudes recibidas por el tutor (por tutorId) filtradas por estado. */
    List<SolicitudContratacionDirecta> findByTutorIdAndEstadoOrderByCreatedAtDesc(
            Long tutorId, EstadoSolicitudContratacion estado);

    /** Todas las solicitudes recibidas por el tutor. */
    List<SolicitudContratacionDirecta> findByTutorIdOrderByCreatedAtDesc(Long tutorId);

    /** Solicitudes creadas por un alumno. */
    List<SolicitudContratacionDirecta> findByAlumnoIdOrderByCreatedAtDesc(Long alumnoId);

    /** Solicitudes pendientes de un tutor. */
    @Query(
            "SELECT s FROM SolicitudContratacionDirecta s WHERE s.tutor.id = :tutorId "
                    + "AND s.estado = 'PENDIENTE' ORDER BY s.createdAt DESC")
    List<SolicitudContratacionDirecta> findPendientesByTutorId(@Param("tutorId") Long tutorId);

    /** Solicitudes por alumno y tutor. */
    List<SolicitudContratacionDirecta> findByAlumnoIdAndTutorIdOrderByCreatedAtDesc(
            Long alumnoId, Long tutorId);

    /**
     * Busca reservas que solapen con el rango horario dado para un tutor y día. Solo considera
     * estados activos (ACEPTADA, PAGADA).
     */
    @Query(
            "SELECT s FROM SolicitudContratacionDirecta s WHERE s.tutor.id = :tutorId AND s.dia ="
                    + " :dia AND s.estado IN"
                    + " ('ACEPTADA', 'PAGADA') AND"
                    + " s.horaInicio < :horaFin AND s.horaFin > :horaInicio")
    List<SolicitudContratacionDirecta> findConflictingBookings(
            @Param("tutorId") Long tutorId,
            @Param("dia") LocalDate dia,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin);

    /** Busca reservas que solapen EXCLUYENDO una solicitud específica (para reprogramar). */
    @Query(
            "SELECT s FROM SolicitudContratacionDirecta s WHERE s.tutor.id = :tutorId AND s.dia ="
                    + " :dia AND s.id <> :excludeId AND s.estado IN"
                    + " ('ACEPTADA', 'PAGADA') AND"
                    + " s.horaInicio < :horaFin AND s.horaFin > :horaInicio")
    List<SolicitudContratacionDirecta> findConflictingBookingsExcluding(
            @Param("tutorId") Long tutorId,
            @Param("dia") LocalDate dia,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin,
            @Param("excludeId") Long excludeId);

    /**
     * Busca reservas que solapen con el rango horario dado para un tutor y día (cualquier estado
     * excepto CANCELADA y RECHAZADA). Se usa para bloquear franjas al reservar desde el perfil.
     */
    @Query(
            "SELECT s FROM SolicitudContratacionDirecta s WHERE s.tutor.id = :tutorId AND s.dia ="
                    + " :dia AND s.estado NOT IN"
                    + " ('CANCELADA_ALUMNO', 'CANCELADA_TUTOR', 'RECHAZADA') AND"
                    + " s.horaInicio < :horaFin AND s.horaFin > :horaInicio")
    List<SolicitudContratacionDirecta> findConflictingBookingsAnyState(
            @Param("tutorId") Long tutorId,
            @Param("dia") LocalDate dia,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin);

    /** Reservas confirmadas (PAGADA) cuya clase es mañana para enviar recordatorio. */
    @Query(
            "SELECT s FROM SolicitudContratacionDirecta s WHERE s.dia = :manana AND s.estado ="
                    + " 'PAGADA'")
    List<SolicitudContratacionDirecta> findBookingsForDate(@Param("manana") LocalDate manana);

    /**
     * Solicitudes ACEPTADAS (pendientes de pago) cuya fecha de clase ya pasó. Se usan para expirar
     * automáticamente las que no fueron pagadas a tiempo.
     */
    @Query(
            "SELECT s FROM SolicitudContratacionDirecta s WHERE s.dia <= :fecha"
                    + " AND s.estado = 'ACEPTADA'")
    List<SolicitudContratacionDirecta> findExpiredAcceptedBookings(@Param("fecha") LocalDate fecha);

    /**
     * Solicitudes PENDIENTES cuya fecha de clase ya pasó. Se usan para expirar automáticamente las
     * que no recibieron respuesta del tutor.
     */
    @Query(
            "SELECT s FROM SolicitudContratacionDirecta s WHERE s.dia <= :fecha"
                    + " AND s.estado = 'PENDIENTE'")
    List<SolicitudContratacionDirecta> findExpiredPendingBookings(@Param("fecha") LocalDate fecha);

    /**
     * Obtiene contrataciones activas (ACEPTADA, PAGADA, REPROGRAMACION_PENDIENTE) de un tutor en un
     * día concreto, para mostrar horarios ocupados.
     */
    @Query(
            "SELECT s FROM SolicitudContratacionDirecta s WHERE s.tutor.id = :tutorId AND s.dia ="
                    + " :dia AND s.estado IN"
                    + " ('ACEPTADA', 'PAGADA', 'REPROGRAMACION_PENDIENTE')"
                    + " ORDER BY s.horaInicio ASC")
    List<SolicitudContratacionDirecta> findActiveBookingsByTutorAndDate(
            @Param("tutorId") Long tutorId, @Param("dia") LocalDate dia);
}
