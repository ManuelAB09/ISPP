package es.us.meerkat.backend.repository.notifications;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import es.us.meerkat.backend.entity.notifications.AlarmaPersonalizada;

/** Repositorio JPA para {@link AlarmaPersonalizada}. */
public interface AlarmaPersonalizadaRepository extends JpaRepository<AlarmaPersonalizada, Long> {

    /**
     * Obtiene todas las alarmas pendientes (no disparadas) de un usuario, ordenadas por fecha de
     * disparo ascendente.
     */
    @Query(
            "SELECT a FROM AlarmaPersonalizada a "
                    + "WHERE a.usuario.id = :usuarioId AND a.disparada = false "
                    + "ORDER BY a.fechaDisparo ASC")
    List<AlarmaPersonalizada> findPendientesByUsuarioId(@Param("usuarioId") Long usuarioId);

    /** Obtiene todas las alarmas de un usuario para un evento concreto. */
    List<AlarmaPersonalizada> findByEventoIdAndUsuarioId(Long eventoId, Long usuarioId);

    /**
     * Busca alarma concreta de un usuario para un evento con unos minutos determinados. Usado para
     * evitar duplicados al crear alarmas.
     */
    Optional<AlarmaPersonalizada> findByEventoIdAndUsuarioIdAndMinutosAntes(
            Long eventoId, Long usuarioId, Integer minutosAntes);

    /**
     * Obtiene todas las alarmas pendientes cuya fechaDisparo ya ha llegado o pasado. Llamado por el
     * scheduler cada minuto para disparar alarmas.
     *
     * @param ahora Momento actual.
     */
    @Query(
            "SELECT a FROM AlarmaPersonalizada a "
                    + "WHERE a.disparada = false AND a.fechaDisparo <= :ahora "
                    + "AND a.evento.cancelado = false")
    List<AlarmaPersonalizada> findAlarmasPendientesADisparar(@Param("ahora") LocalDateTime ahora);

    /** Elimina todas las alarmas de un usuario para un evento (usado al cancelar asistencia). */
    @Modifying
    @Query(
            "DELETE FROM AlarmaPersonalizada a "
                    + "WHERE a.evento.id = :eventoId AND a.usuario.id = :usuarioId")
    void deleteByEventoIdAndUsuarioId(
            @Param("eventoId") Long eventoId, @Param("usuarioId") Long usuarioId);

    /** Elimina alarmas ya disparadas con más de 30 días para limpieza periódica. */
    @Modifying
    @Query(
            "DELETE FROM AlarmaPersonalizada a "
                    + "WHERE a.disparada = true AND a.disparadaAt < :fechaLimite")
    void deleteOldDisparadas(@Param("fechaLimite") LocalDateTime fechaLimite);

    /** Elimina todas las alarmas de un evento. */
    @Modifying
    @Query("DELETE FROM AlarmaPersonalizada a WHERE a.evento.id = :eventoId")
    void deleteByEventoId(@Param("eventoId") Long eventoId);
}
