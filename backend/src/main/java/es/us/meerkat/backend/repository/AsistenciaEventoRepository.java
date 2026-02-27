package es.us.meerkat.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import es.us.meerkat.backend.entity.AsistenciaEvento;

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
}
