package es.us.meerkat.backend.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
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
     * Obtiene todos los eventos visibles en el mapa.
     *
     * @return Lista de eventos visibles en mapa.
     */
    @Query(
            "SELECT e FROM Evento e WHERE e.visibleMapa = true AND e.cancelado = false AND"
                    + " e.privado = false")
    List<Evento> findVisibleOnMap();

    /**
     * Obtiene todos los eventos públicos de una comunidad.
     *
     * @param comunidadCount Identificador de la comunidad.
     * @return Lista de eventos públicos.
     */
    @Query("SELECT e FROM Evento e WHERE e.privado = false AND e.cancelado = false")
    List<Evento> findPublicEvents();

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

    /**
     * Obtiene los eventos no cancelados de una comunidad.
     *
     * @param comunidadId Identificador de la comunidad.
     * @return Lista de eventos activos de la comunidad.
     */
    List<Evento> findByComunidadIdAndCanceladoFalse(Long comunidadId);

    // -----------------------------------------------
    // NUEVAS CONSULTAS: Mis Eventos
    // -----------------------------------------------

    /**
     * Obtiene todos los eventos futuros de las comunidades a las que pertenece el usuario,
     * incluyendo tanto eventos públicos como privados de esas comunidades. Ordena por fecha
     * ascendente para mostrar primero los más próximos.
     *
     * @param usuarioId Identificador del usuario.
     * @param ahora Momento actual para filtrar eventos futuros.
     * @return Lista de eventos próximos del usuario.
     */
    @Query(
            "SELECT DISTINCT e FROM Evento e "
                    + "JOIN MiembroComunidad mc ON mc.comunidad.id = e.comunidad.id "
                    + "WHERE mc.usuario.id = :usuarioId "
                    + "AND e.cancelado = false "
                    + "AND e.fechaHora >= :ahora "
                    + "ORDER BY e.fechaHora ASC")
    List<Evento> findProximosEventosByUsuarioId(
            @Param("usuarioId") Long usuarioId, @Param("ahora") LocalDateTime ahora);

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
     * Obtiene eventos a los que el usuario ha confirmado asistencia y están próximos.
     *
     * @param usuarioId Identificador del usuario.
     * @param ahora Momento actual.
     * @return Lista de eventos con asistencia confirmada y próximos.
     */
    @Query(
            "SELECT e FROM Evento e "
                    + "JOIN AsistenciaEvento ae ON ae.evento.id = e.id "
                    + "WHERE ae.usuario.id = :usuarioId "
                    + "AND ae.estado = 'CONFIRMADA' "
                    + "AND e.cancelado = false "
                    + "AND e.fechaHora >= :ahora "
                    + "ORDER BY e.fechaHora ASC")
    List<Evento> findConfirmedEventosByUsuarioId(
            @Param("usuarioId") Long usuarioId, @Param("ahora") LocalDateTime ahora);
}
