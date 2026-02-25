package es.us.meerkat.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import es.us.meerkat.backend.entity.Ubicacion;

/**
 * Repositorio JPA para la entidad {@link Ubicacion}.
 *
 * Permite realizar operaciones CRUD y consultas específicas
 * sobre ubicaciones en la base de datos.
 */
public interface UbicacionRepository extends JpaRepository<Ubicacion, Long> {

    /**
     * Busca una ubicación por su nombre exacto.
     *
     * @param nombre Nombre de la ubicación.
     * @return Optional con la ubicación si existe.
     */
    Optional<Ubicacion> findByNombre(String nombre);

    /**
     * Devuelve todas las ubicaciones cuyo nombre contenga la cadena dada.
     *
     * Útil para buscar lugares recomendados o filtrar en el mapa.
     *
     * @param nombreFragment Fragmento de nombre a buscar.
     * @return Lista de ubicaciones coincidentes.
     */
    List<Ubicacion> findByNombreContainingIgnoreCase(String nombreFragment);

}