package es.us.meerkat.backend.repository.communities;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import es.us.meerkat.backend.entity.Anuncio;
import es.us.meerkat.backend.entity.ComentarioAnuncio;

@Repository
public interface ComentarioAnuncioRepository extends JpaRepository<ComentarioAnuncio, Long> {
    List<ComentarioAnuncio> findByAnuncioOrderByCreatedAtDesc(Anuncio anuncio);
}
