package es.us.meerkat.backend.repository.events;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import es.us.meerkat.backend.entity.events.AsistenciaEvento;
import es.us.meerkat.backend.entity.events.EstadoAsistencia;

/**
 * Repositorio JPA para la entidad {@link AsistenciaEvento}.
 *
 * <p>Permite realizar operaciones CRUD sobre la asistencia a eventos en la base de datos, así como
 * consultas personalizadas.
 */
public interface AsistenciaEventoRepository extends JpaRepository<AsistenciaEvento, Long> {

    /**
     * Obtiene todas las asistencias confirmadas de un evento.
     *
     * @param eventoId Identificador del evento.
     * @return Lista de asistencias confirmadas.
     */
    @Query(
            "SELECT a FROM AsistenciaEvento a WHERE a.evento.id = :eventoId AND a.estado ="
                    + " es.us.meerkat.backend.entity.EstadoAsistencia.CONFIRMADA")
    List<AsistenciaEvento> findConfirmedAttendanceByEvent(@Param("eventoId") Long eventoId);

    /**
     * Obtiene todas las asistencias de un evento.
     *
     * @param eventoId Identificador del evento.
     * @return Lista de todas las asistencias del evento.
     */
    List<AsistenciaEvento> findByEventoId(Long eventoId);

    /**
     * Busca la asistencia de un usuario a un evento específico.
     *
     * @param eventoId Identificador del evento.
     * @param usuarioId Identificador del usuario.
     * @return La asistencia si existe.
     */
    @Query(
            "SELECT a FROM AsistenciaEvento a WHERE a.evento.id = :eventoId AND a.usuario.id ="
                    + " :usuarioId")
    Optional<AsistenciaEvento> findByEventoAndUsuario(
            @Param("eventoId") Long eventoId, @Param("usuarioId") Long usuarioId);

    /**
     * Cuenta el número de asistentes confirmados a un evento.
     *
     * @param eventoId Identificador del evento.
     * @return Número de asistentes confirmados.
     */
    @Query(
            "SELECT COUNT(a) FROM AsistenciaEvento a WHERE a.evento.id = :eventoId AND a.estado ="
                    + " es.us.meerkat.backend.entity.EstadoAsistencia.CONFIRMADA")
    long countConfirmedByEvent(@Param("eventoId") Long eventoId);

    /**
     * Busca la asistencia de un usuario a un evento específico.
     *
     * @param eventoId Identificador del evento.
     * @param usuarioId Identificador del usuario.
     * @return Optional con la asistencia si existe.
     */
    Optional<AsistenciaEvento> findByEventoIdAndUsuarioId(Long eventoId, Long usuarioId);

    /**
     * Obtiene todas las asistencias de un evento filtradas por estado.
     *
     * @param eventoId Identificador del evento.
     * @param estado Estado de la asistencia.
     * @return Lista de asistencias con el estado especificado.
     */
    List<AsistenciaEvento> findByEventoIdAndEstado(Long eventoId, EstadoAsistencia estado);

    /**
     * Obtiene todas las asistencias confirmadas de un usuario.
     *
     * @param usuarioId Identificador del usuario.
     * @return Lista de asistencias confirmadas.
     */
    @Query(
            "SELECT ae FROM AsistenciaEvento ae WHERE ae.usuario.id = :usuarioId "
                    + "AND ae.estado = 'CONFIRMADA' "
                    + "ORDER BY ae.evento.fechaHora ASC")
    List<AsistenciaEvento> findConfirmadasByUsuarioId(@Param("usuarioId") Long usuarioId);

    /**
     * Cuenta las asistencias confirmadas a un evento.
     *
     * @param eventoId Identificador del evento.
     * @return Número de asistencias confirmadas.
     */
    @Query(
            "SELECT COUNT(ae) FROM AsistenciaEvento ae WHERE ae.evento.id = :eventoId "
                    + "AND ae.estado = 'CONFIRMADA'")
    Integer countConfirmadasByEventoId(@Param("eventoId") Long eventoId);

    /** Elimina todas las asistencias de un usuario. */
    void deleteByUsuarioId(Long usuarioId);

    /** Elimina todas las asistencias de eventos creados por un usuario. */
    @Modifying
    @Query("DELETE FROM AsistenciaEvento a WHERE a.evento.creador.id = :usuarioId")
    void deleteByEventoCreadorId(@Param("usuarioId") Long usuarioId);

    /** Elimina todas las asistencias de un evento. */
    @Modifying
    @Query("DELETE FROM AsistenciaEvento a WHERE a.evento.id = :eventoId")
    void deleteByEventoId(@Param("eventoId") Long eventoId);

    /**
     * Cuenta las asistencias confirmadas de un usuario a eventos futuros activos de una comunidad.
     */
    @Query(
            "SELECT COUNT(a) FROM AsistenciaEvento a "
                    + "WHERE a.usuario.id = :usuarioId "
                    + "AND a.evento.comunidad.id = :comunidadId "
                    + "AND a.estado = es.us.meerkat.backend.entity.EstadoAsistencia.CONFIRMADA "
                    + "AND a.evento.cancelado = false "
                    + "AND a.evento.fechaHora >= :ahora")
    long countActiveEventAttendances(
            @Param("usuarioId") Long usuarioId,
            @Param("comunidadId") Long comunidadId,
            @Param("ahora") java.time.LocalDateTime ahora);

    /** Elimina las asistencias futuras de un usuario en una comunidad (al expulsarlo). */
    @Modifying
    @Query(
            "DELETE FROM AsistenciaEvento a WHERE a.usuario.id = :usuarioId"
                    + " AND a.evento.comunidad.id = :comunidadId"
                    + " AND a.evento.cancelado = false"
                    + " AND a.evento.fechaHora > :ahora")
    void deleteFutureAttendancesInCommunity(
            @Param("usuarioId") Long usuarioId,
            @Param("comunidadId") Long comunidadId,
            @Param("ahora") java.time.LocalDateTime ahora);
}
