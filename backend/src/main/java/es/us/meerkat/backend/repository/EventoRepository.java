package es.us.meerkat.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
}
