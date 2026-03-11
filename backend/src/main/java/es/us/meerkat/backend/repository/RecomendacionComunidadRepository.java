package es.us.meerkat.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import es.us.meerkat.backend.entity.Comunidad;
import es.us.meerkat.backend.entity.RecomendacionComunidad;
import es.us.meerkat.backend.entity.Usuario;

/** Repositorio para la gestión de recomendaciones de comunidades en la base de datos. */
@Repository
public interface RecomendacionComunidadRepository
        extends JpaRepository<RecomendacionComunidad, Long> {

    /**
     * Obtiene las recomendaciones de un usuario de forma paginada, ordenadas por relevancia.
     *
     * @param usuario el usuario
     * @param pageable la paginación
     * @return página de recomendaciones
     */
    Page<RecomendacionComunidad> findByUsuarioOrderByRelevanciaDesc(
            Usuario usuario, Pageable pageable);

    /**
     * Obtiene las recomendaciones no vistas de un usuario, ordenadas por relevancia.
     *
     * @param usuario el usuario
     * @param pageable la paginación
     * @return página de recomendaciones no vistas
     */
    Page<RecomendacionComunidad> findByUsuarioAndVistaFalseOrderByRelevanciaDesc(
            Usuario usuario, Pageable pageable);

    /**
     * Obtiene todas las recomendaciones de un usuario.
     *
     * @param usuario el usuario
     * @return lista de recomendaciones
     */
    List<RecomendacionComunidad> findByUsuarioOrderByRelevanciaDesc(Usuario usuario);

    /**
     * Cuenta las recomendaciones no vistas de un usuario.
     *
     * @param usuario el usuario
     * @return número de recomendaciones no vistas
     */
    long countByUsuarioAndVistaFalse(Usuario usuario);

    /**
     * Cuenta las recomendaciones de un usuario.
     *
     * @param usuario el usuario
     * @return número total de recomendaciones
     */
    long countByUsuario(Usuario usuario);

    /**
     * Obtiene una recomendación de un usuario para una comunidad específica.
     *
     * @param usuario el usuario
     * @param comunidad la comunidad
     * @return la recomendación si existe
     */
    Optional<RecomendacionComunidad> findByUsuarioAndComunidad(
            Usuario usuario, Comunidad comunidad);
}
