package es.us.meerkat.backend.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import es.us.meerkat.backend.entity.Evento;

/**
 * Repositorio JPA para la entidad {@link Evento}.
 *
 * <p>Permite realizar operaciones CRUD sobre los eventos en la base de datos, así como consultas
 * personalizadas para filtrar por criterios específicos.
 */
public interface EventoRepository extends JpaRepository<Evento, Long> {

    /**
     * Obtiene todos los eventos no cancelados.
     *
     * @return Lista de eventos activos.
     */
    List<Evento> findByCanceladoFalse();

    /**
     * Obtiene todos los eventos visibles en el mapa (solo públicos de comunidades públicas).
     *
     * @return Lista de eventos visibles en mapa.
     */
    @Query(
            "SELECT e FROM Evento e WHERE e.visibleMapa = true AND e.cancelado = false AND"
                    + " e.privado = false AND e.fechaHora >= :ahora")
    List<Evento> findVisibleOnMap(@Param("ahora") LocalDateTime ahora);

    /**
     * Obtiene todos los eventos públicos de una comunidad.
     *
     * @param comunidadCount Identificador de la comunidad.
     * @return Lista de eventos públicos.
     */
    @Query(
            "SELECT e FROM Evento e WHERE e.privado = false AND e.cancelado = false AND e.fechaHora"
                    + " >= :ahora")
    List<Evento> findPublicEvents(@Param("ahora") LocalDateTime ahora);

    /**
     * Obtiene todos los eventos privados de una comunidad.
     *
     * @param comunidadCount Identificador de la comunidad.
     * @return Lista de eventos privados.
     */
    @Query("SELECT e FROM Evento e WHERE e.privado = true AND e.cancelado = false")
    List<Evento> findPrivateEvents();

    /**
     * Busca un evento por su ID.
     *
     * @param id Identificador del evento.
     * @return El evento si existe.
     */
    @Override
    Optional<Evento> findById(Long id);

    /**
     * Obtiene todos los eventos de una comunidad.
     *
     * @param comunidadId Identificador de la comunidad.
     * @return Lista de eventos de la comunidad.
     */
    List<Evento> findByComunidadId(Long comunidadId);

    @Modifying
    @Query("UPDATE Evento e SET e.comunidad = null WHERE e.comunidad.id = :comunidadId")
    void disassociateFromComunidad(@Param("comunidadId") Long comunidadId);

    /**
     * Obtiene los eventos no cancelados de una comunidad.
     *
     * @param comunidadId Identificador de la comunidad.
     * @return Lista de eventos activos de la comunidad.
     */
    List<Evento> findByComunidadIdAndCanceladoFalse(Long comunidadId);

    /**
     * Obtiene los eventos no cancelados y futuros de una comunidad.
     *
     * @param comunidadId Identificador de la comunidad.
     * @param ahora Momento actual para filtrar eventos futuros.
     * @return Lista de eventos activos futuros de la comunidad.
     */
    @Query(
            "SELECT e FROM Evento e WHERE e.comunidad.id = :comunidadId"
                    + " AND e.cancelado = false"
                    + " AND ("
                    + "   e.fechaHora >= :ahora"
                    + "   OR ("
                    + "     e.fechaHora < :ahora"
                    + "     AND ((e.fechaFin IS NOT NULL AND e.fechaFin >= :ahora)"
                    + "          OR (e.fechaFin IS NULL AND e.fechaHora >= :limiteMargen))"
                    + "     AND (:usuarioId IS NOT NULL"
                    + "          AND (e.creador.id = :usuarioId"
                    + "               OR EXISTS (SELECT ae FROM AsistenciaEvento ae"
                    + "                          WHERE ae.evento.id = e.id"
                    + "                          AND ae.usuario.id = :usuarioId"
                    + "                          AND ae.estado = 'CONFIRMADA')))"
                    + "   )"
                    + ")")
    List<Evento> findByComunidadIdAndCanceladoFalseAndFuture(
            @Param("comunidadId") Long comunidadId,
            @Param("ahora") LocalDateTime ahora,
            @Param("limiteMargen") LocalDateTime limiteMargen,
            @Param("usuarioId") Long usuarioId);

    // -----------------------------------------------
    // NUEVAS CONSULTAS: Mis Eventos
    // -----------------------------------------------

    /**
     * Obtiene todos los eventos activos de las comunidades a las que pertenece el usuario: eventos
     * futuros, eventos en curso (fechaFin no ha pasado) y eventos sin fecha de fin que empezaron
     * hace menos de 2 horas. Ordena por fecha ascendente.
     *
     * @param usuarioId Identificador del usuario.
     * @param ahora Momento actual.
     * @param limiteMargen Momento actual menos el margen (ahora - 2h) para eventos sin fechaFin.
     * @return Lista de eventos próximos y en curso del usuario.
     */
    @Query(
            "SELECT DISTINCT e FROM Evento e "
                    + "JOIN MiembroComunidad mc ON mc.comunidad.id = e.comunidad.id "
                    + "WHERE mc.usuario.id = :usuarioId "
                    + "AND e.cancelado = false "
                    + "AND ("
                    + "  e.fechaHora >= :ahora "
                    + "  OR ("
                    + "    e.fechaHora < :ahora "
                    + "    AND ((e.fechaFin IS NOT NULL AND e.fechaFin >= :ahora)"
                    + "         OR (e.fechaFin IS NULL AND e.fechaHora >= :limiteMargen))"
                    + "    AND (e.creador.id = :usuarioId"
                    + "         OR EXISTS (SELECT ae FROM AsistenciaEvento ae"
                    + "                    WHERE ae.evento.id = e.id"
                    + "                    AND ae.usuario.id = :usuarioId"
                    + "                    AND ae.estado = 'CONFIRMADA'))"
                    + "  )"
                    + ") "
                    + "ORDER BY e.fechaHora ASC")
    List<Evento> findProximosEventosByUsuarioId(
            @Param("usuarioId") Long usuarioId,
            @Param("ahora") LocalDateTime ahora,
            @Param("limiteMargen") LocalDateTime limiteMargen);

    /**
     * Obtiene todos los eventos (pasados y futuros) de las comunidades del usuario. Incluye
     * cancelados si se especifica. Usado para el historial completo.
     *
     * @param usuarioId Identificador del usuario.
     * @return Lista de todos los eventos del usuario.
     */
    @Query(
            "SELECT DISTINCT e FROM Evento e "
                    + "JOIN MiembroComunidad mc ON mc.comunidad.id = e.comunidad.id "
                    + "WHERE mc.usuario.id = :usuarioId "
                    + "ORDER BY e.fechaHora DESC")
    List<Evento> findAllEventosByUsuarioId(@Param("usuarioId") Long usuarioId);

    /**
     * Obtiene eventos próximos de un usuario en un rango de tiempo específico. Usado por el
     * scheduler de alertas para detectar eventos que requieren notificación.
     *
     * @param usuarioId Identificador del usuario.
     * @param desde Inicio del rango de tiempo.
     * @param hasta Fin del rango de tiempo.
     * @return Lista de eventos en el rango de tiempo.
     */
    @Query(
            "SELECT DISTINCT e FROM Evento e "
                    + "JOIN MiembroComunidad mc ON mc.comunidad.id = e.comunidad.id "
                    + "WHERE mc.usuario.id = :usuarioId "
                    + "AND e.cancelado = false "
                    + "AND e.fechaHora BETWEEN :desde AND :hasta")
    List<Evento> findEventosByUsuarioIdInRange(
            @Param("usuarioId") Long usuarioId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    /**
     * Obtiene todos los eventos que están en un rango de tiempo (para el scheduler global). No
     * filtra por usuario; el scheduler itera sobre asistentes.
     *
     * @param desde Inicio del rango.
     * @param hasta Fin del rango.
     * @return Lista de eventos en el rango.
     */
    @Query(
            "SELECT e FROM Evento e "
                    + "WHERE e.cancelado = false "
                    + "AND e.fechaHora BETWEEN :desde AND :hasta")
    List<Evento> findEventosInRange(
            @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    /**
     * Obtiene eventos a los que el usuario ha confirmado asistencia y están próximos o en curso.
     *
     * @param usuarioId Identificador del usuario.
     * @param ahora Momento actual.
     * @return Lista de eventos con asistencia confirmada y próximos o en curso.
     */
    @Query(
            "SELECT e FROM Evento e "
                    + "JOIN AsistenciaEvento ae ON ae.evento.id = e.id "
                    + "WHERE ae.usuario.id = :usuarioId "
                    + "AND ae.estado = 'CONFIRMADA' "
                    + "AND e.cancelado = false "
                    + "AND (e.fechaHora >= :ahora "
                    + "     OR (e.fechaFin IS NOT NULL AND e.fechaFin >= :ahora)) "
                    + "ORDER BY e.fechaHora ASC")
    List<Evento> findConfirmedEventosByUsuarioId(
            @Param("usuarioId") Long usuarioId, @Param("ahora") LocalDateTime ahora);

    /** Obtiene todos los eventos creados por un usuario. */
    List<Evento> findByCreadorId(Long usuarioId);

    /** Elimina todos los eventos creados por un usuario. */
    @Modifying
    @Query("DELETE FROM Evento e WHERE e.creador.id = :usuarioId")
    void deleteByUsuarioId(@Param("usuarioId") Long usuarioId);

    /**
     * Obtiene todos los eventos cuya fecha de fin (o fechaHora si no tiene fin) es anterior a la
     * fecha límite proporcionada. Usado para borrado automático.
     *
     * @param limite Fecha límite (ahora - 24h).
     * @return Lista de eventos pasados a eliminar.
     */
    @Query(
            "SELECT e FROM Evento e "
                    + "WHERE (e.fechaFin IS NOT NULL AND e.fechaFin < :limite) "
                    + "OR (e.fechaFin IS NULL AND e.fechaHora < :limite)")
    List<Evento> findEventosPasadosParaEliminar(@Param("limite") LocalDateTime limite);
}
