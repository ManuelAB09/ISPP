package es.us.meerkat.backend.repository.google;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import es.us.meerkat.backend.entity.GoogleCalendarEvento;

/** Repositorio JPA para {@link GoogleCalendarEvento}. */
public interface GoogleCalendarEventoRepository extends JpaRepository<GoogleCalendarEvento, Long> {

    /**
     * Busca el mapeo de un evento concreto para un usuario concreto. Usado para actualizar o
     * eliminar el evento en Google Calendar.
     */
    Optional<GoogleCalendarEvento> findByEventoIdAndUsuarioId(Long eventoId, Long usuarioId);

    /**
     * Obtiene todos los mapeos de un evento (todos los usuarios que lo tienen en su calendario).
     * Usado cuando el evento se cancela o modifica para actualizar todos los calendarios.
     */
    List<GoogleCalendarEvento> findByEventoId(Long eventoId);

    /** Elimina todos los mapeos de un usuario (cuando desconecta Google Calendar). */
    @Modifying
    @Query("DELETE FROM GoogleCalendarEvento g WHERE g.usuario.id = :usuarioId")
    void deleteByUsuarioId(@Param("usuarioId") Long usuarioId);

    /** Elimina todos los mapeos de un evento. */
    @Modifying
    @Query("DELETE FROM GoogleCalendarEvento g WHERE g.evento.id = :eventoId")
    void deleteByEventoId(@Param("eventoId") Long eventoId);
}
