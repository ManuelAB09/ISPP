package es.us.meerkat.backend.repository.communities;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import es.us.meerkat.backend.entity.communities.Anuncio;
import es.us.meerkat.backend.entity.communities.ComentarioAnuncio;

@Repository
public interface ComentarioAnuncioRepository extends JpaRepository<ComentarioAnuncio, Long> {
    List<ComentarioAnuncio> findByAnuncioOrderByCreatedAtDesc(Anuncio anuncio);

    void deleteByAnuncio(Anuncio anuncio);

    /** Elimina todos los comentarios de los anuncios de una comunidad (bulk). */
    @Modifying
    @Query(
            "DELETE FROM ComentarioAnuncio c "
                    + "WHERE c.anuncio.id IN (SELECT a.id FROM Anuncio a WHERE a.comunidad.id ="
                    + " :comunidadId)")
    void deleteByComunidadId(@Param("comunidadId") Long comunidadId);
}
