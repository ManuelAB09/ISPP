package es.us.meerkat.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import es.us.meerkat.backend.entity.GoogleCalendarBooking;

/** Repositorio JPA para {@link GoogleCalendarBooking}. */
public interface GoogleCalendarBookingRepository
        extends JpaRepository<GoogleCalendarBooking, Long> {

    /** Busca el mapeo de una solicitud para un usuario concreto. */
    Optional<GoogleCalendarBooking> findBySolicitudIdAndUsuarioId(Long solicitudId, Long usuarioId);

    /** Obtiene todos los mapeos de una solicitud (alumno + tutor). */
    List<GoogleCalendarBooking> findBySolicitudId(Long solicitudId);

    /** Elimina todos los mapeos de un usuario (cuando desconecta Google Calendar). */
    @Modifying
    @Query("DELETE FROM GoogleCalendarBooking g WHERE g.usuario.id = :usuarioId")
    void deleteByUsuarioId(@Param("usuarioId") Long usuarioId);
}
