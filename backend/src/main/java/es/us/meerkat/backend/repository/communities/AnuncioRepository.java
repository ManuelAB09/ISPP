package es.us.meerkat.backend.repository.communities;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import es.us.meerkat.backend.entity.Anuncio;
import es.us.meerkat.backend.entity.Comunidad;

/** Repositorio para la gestión de anuncios en la base de datos. */
@Repository
public interface AnuncioRepository extends JpaRepository<Anuncio, Long> {

    /**
     * Obtiene los anuncios de una comunidad de forma paginada, ordenados por fecha descendente.
     *
     * @param comunidad la comunidad
     * @param pageable la paginación
     * @return página de anuncios
     */
    Page<Anuncio> findByComunidadOrderByCreatedAtDesc(Comunidad comunidad, Pageable pageable);

    /**
     * Obtiene todos los anuncios de una comunidad, ordenados por fecha descendente.
     *
     * @param comunidad la comunidad
     * @return lista de anuncios
     */
    List<Anuncio> findByComunidadOrderByCreatedAtDesc(Comunidad comunidad);

    /**
     * Cuenta los anuncios de una comunidad.
     *
     * @param comunidad la comunidad
     * @return número de anuncios
     */
    long countByComunidad(Comunidad comunidad);
}
