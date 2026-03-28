package es.us.meerkat.backend.repository.chats;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import es.us.meerkat.backend.entity.MensajeComunidad;

/** Repositorio para acceder a mensajes de comunidades en la base de datos. */
@Repository
public interface MensajeComunidadRepository extends JpaRepository<MensajeComunidad, Long> {

    /**
     * Encontrar todos los mensajes de una comunidad, ordenados por fecha de creación ascendente.
     *
     * @param comunidadId ID de la comunidad.
     * @return lista de mensajes de la comunidad.
     */
    List<MensajeComunidad> findByComunidadIdOrderByCreatedAtAsc(Long comunidadId);

    /**
     * Encontrar todos los mensajes de una comunidad, ordenados por fecha de creación descendente.
     *
     * @param comunidadId ID de la comunidad.
     * @return lista de mensajes de la comunidad (más nuevos primero).
     */
    List<MensajeComunidad> findByComunidadIdOrderByCreatedAtDesc(Long comunidadId);

    /**
     * Encontrar los últimos N mensajes de una comunidad.
     *
     * @param comunidadId ID de la comunidad.
     * @param limit número máximo de mensajes a retornar.
     * @return lista de hasta 'limit' mensajes ordenados descendentemente.
     */
    List<MensajeComunidad> findTopByOrderByCreatedAtDesc(Long limit);

    /** Elimina todos los mensajes de comunidad enviados por un usuario. */
    @Modifying
    @Query("DELETE FROM MensajeComunidad m WHERE m.usuario.id = :usuarioId")
    void deleteByUsuarioId(@Param("usuarioId") Long usuarioId);

    @Query(
            "SELECT m.usuario.id, COUNT(m) FROM MensajeComunidad m WHERE m.comunidad.id ="
                    + " :comunidadId GROUP BY m.usuario.id")
    List<Object[]> countMensajesByComunidad(@Param("comunidadId") Long comunidadId);

    @Query(
            "SELECT mc.comunidad.id, (COUNT(m.id) - COUNT(ml.id)) "
                    + "FROM MiembroComunidad mc "
                    + "LEFT JOIN MensajeComunidad m ON m.comunidad.id = mc.comunidad.id "
                    + "LEFT JOIN MensajeComunidadLeido ml ON ml.mensajeComunidad.id = m.id AND"
                    + " ml.usuario.id = :usuarioId "
                    + "WHERE mc.usuario.id = :usuarioId "
                    + "GROUP BY mc.comunidad.id "
                    + "HAVING COUNT(m.id) > 0")
    List<Object[]> countNoLeidosByComunidadParaUsuario(@Param("usuarioId") Long usuarioId);
}
