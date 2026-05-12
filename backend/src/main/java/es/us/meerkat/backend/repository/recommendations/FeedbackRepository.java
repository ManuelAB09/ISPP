package es.us.meerkat.backend.repository.recommendations;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import es.us.meerkat.backend.entity.recommendations.Feedback;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    Page<Feedback> findByComunidadId(Long comunidadId, Pageable pageable);

    Page<Feedback> findByAlumnoId(Long alumnoId, Pageable pageable);

    /** Desvincula el feedback de una comunidad (preserva el feedback como histórico). */
    @Modifying
    @Query("UPDATE Feedback f SET f.comunidad = null WHERE f.comunidad.id = :comunidadId")
    void disassociateFromComunidad(@Param("comunidadId") Long comunidadId);
}
