package es.us.meerkat.backend.repository;

import es.us.meerkat.backend.entity.ComentarioAnuncio;
import es.us.meerkat.backend.entity.Anuncio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComentarioAnuncioRepository extends JpaRepository<ComentarioAnuncio, Long> {
    List<ComentarioAnuncio> findByAnuncioOrderByCreatedAtDesc(Anuncio anuncio);
}
