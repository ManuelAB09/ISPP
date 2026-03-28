package es.us.meerkat.backend.repository.notifications;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import es.us.meerkat.backend.entity.AlertaEvento;
import es.us.meerkat.backend.entity.TipoAlerta;

/**
 * Repositorio JPA para la entidad {@link AlertaEvento}.
 *
 * <p>Gestiona la persistencia de alertas automáticas de eventos próximos.
 */
public interface AlertaEventoRepository extends JpaRepository<AlertaEvento, Long> {

    /**
     * Obtiene todas las alertas no leídas de un usuario, ordenadas por fecha de creación.
     *
     * @param usuarioId Identificador del usuario.
     * @return Lista de alertas no leídas.
     */
    @Query(
            "SELECT a FROM AlertaEvento a WHERE a.usuario.id = :usuarioId AND a.leida = false "
                    + "ORDER BY a.createdAt DESC")
    List<AlertaEvento> findUnreadByUsuarioId(@Param("usuarioId") Long usuarioId);

    /**
     * Obtiene todas las alertas de un usuario (leídas y no leídas).
     *
     * @param usuarioId Identificador del usuario.
     * @return Lista de todas las alertas del usuario.
     */
    @Query(
            "SELECT a FROM AlertaEvento a WHERE a.usuario.id = :usuarioId "
                    + "ORDER BY a.createdAt DESC")
    List<AlertaEvento> findAllByUsuarioId(@Param("usuarioId") Long usuarioId);

    /**
     * Verifica si ya existe una alerta de un tipo específico para un evento y usuario. Evita
     * duplicados cuando el scheduler se ejecuta repetidamente.
     *
     * @param eventoId Identificador del evento.
     * @param usuarioId Identificador del usuario.
     * @param tipo Tipo de alerta.
     * @return Optional con la alerta si existe.
     */
    Optional<AlertaEvento> findByEventoIdAndUsuarioIdAndTipo(
            Long eventoId, Long usuarioId, TipoAlerta tipo);

    /**
     * Cuenta las alertas no leídas de un usuario.
     *
     * @param usuarioId Identificador del usuario.
     * @return Número de alertas no leídas.
     */
    @Query(
            "SELECT COUNT(a) FROM AlertaEvento a WHERE a.usuario.id = :usuarioId "
                    + "AND a.leida = false")
    Long countUnreadByUsuarioId(@Param("usuarioId") Long usuarioId);

    /**
     * Marca todas las alertas no leídas de un usuario como leídas.
     *
     * @param usuarioId Identificador del usuario.
     * @param leidaAt Fecha y hora en que se marcaron como leídas.
     */
    @Modifying
    @Query(
            "UPDATE AlertaEvento a SET a.leida = true, a.leidaAt = :leidaAt "
                    + "WHERE a.usuario.id = :usuarioId AND a.leida = false")
    void markAllAsReadByUsuarioId(
            @Param("usuarioId") Long usuarioId, @Param("leidaAt") LocalDateTime leidaAt);

    /**
     * Obtiene alertas de eventos que comenzarán en el rango de tiempo especificado y cuya alerta
     * del tipo dado aún no ha sido generada para ningún usuario. Usado internamente por el
     * scheduler.
     *
     * @param desde Inicio del rango de tiempo.
     * @param hasta Fin del rango de tiempo.
     * @param tipo Tipo de alerta a buscar.
     * @return Lista de IDs de eventos que ya tienen alerta generada en ese rango.
     */
    @Query(
            "SELECT DISTINCT a.evento.id FROM AlertaEvento a "
                    + "WHERE a.tipo = :tipo AND a.evento.fechaHora BETWEEN :desde AND :hasta")
    List<Long> findEventoIdsWithAlertaInRange(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("tipo") TipoAlerta tipo);

    /**
     * Elimina alertas antiguas ya leídas con más de 7 días de antigüedad. Usado por el scheduler de
     * limpieza.
     *
     * @param fechaLimite Fecha límite, alertas anteriores a esta se eliminan.
     */
    @Modifying
    @Query("DELETE FROM AlertaEvento a WHERE a.leida = true AND a.leidaAt < :fechaLimite")
    void deleteOldReadAlertas(@Param("fechaLimite") LocalDateTime fechaLimite);

    /** Elimina todas las alertas de un evento. */
    @Modifying
    @Query("DELETE FROM AlertaEvento a WHERE a.evento.id = :eventoId")
    void deleteByEventoId(@Param("eventoId") Long eventoId);
}
