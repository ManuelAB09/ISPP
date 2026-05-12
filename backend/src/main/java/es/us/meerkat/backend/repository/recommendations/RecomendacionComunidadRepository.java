package es.us.meerkat.backend.repository.recommendations;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import es.us.meerkat.backend.entity.recommendations.RecomendacionComunidad;

public interface RecomendacionComunidadRepository
        extends JpaRepository<RecomendacionComunidad, Long> {

    /** Elimina todas las recomendaciones que apuntan a una comunidad (bulk). */
    @Modifying
    @Query("DELETE FROM RecomendacionComunidad r WHERE r.comunidad.id = :comunidadId")
    void deleteByComunidadId(@Param("comunidadId") Long comunidadId);
}
