package es.us.meerkat.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import es.us.meerkat.backend.entity.MensajeComunidad;

/**
 * Repositorio para acceder a mensajes de comunidades en la base de datos.
 */
@Repository
public interface MensajeComunidadRepository extends JpaRepository<MensajeComunidad, Long> {

    /**
     * Encontrar todos los mensajes de una comunidad, ordenados por fecha de
     * creación ascendente.
     *
     * @param comunidadId ID de la comunidad.
     * @return lista de mensajes de la comunidad.
     */
    List<MensajeComunidad> findByComunidadIdOrderByCreatedAtAsc(Long comunidadId);

    /**
     * Encontrar todos los mensajes de una comunidad, ordenados por fecha de
     * creación descendente.
     *
     * @param comunidadId ID de la comunidad.
     * @return lista de mensajes de la comunidad (más nuevos primero).
     */
    List<MensajeComunidad> findByComunidadIdOrderByCreatedAtDesc(Long comunidadId);

    /**
     * Encontrar los últimos N mensajes de una comunidad.
     *
     * @param comunidadId ID de la comunidad.
     * @param limit       número máximo de mensajes a retornar.
     * @return lista de hasta 'limit' mensajes ordenados descendentemente.
     */
    List<MensajeComunidad> findTopByOrderByCreatedAtDesc(Long limit);
}
