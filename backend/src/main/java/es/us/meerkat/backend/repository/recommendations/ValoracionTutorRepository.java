package es.us.meerkat.backend.repository.recommendations;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import es.us.meerkat.backend.entity.ValoracionTutor;

@Repository
public interface ValoracionTutorRepository extends JpaRepository<ValoracionTutor, Long> {

    /** Valoración media de un tutor (0.0 si no tiene ninguna). */
    @Query(
            "SELECT COALESCE(AVG(v.puntuacion), 0.0) FROM ValoracionTutor v WHERE v.tutor.id ="
                    + " :tutorId")
    Double findMediaByTutorId(@Param("tutorId") Long tutorId);

    /** Número de valoraciones de un tutor. */
    long countByTutorId(Long tutorId);

    /** Valoración existente de un usuario sobre un tutor (para upsert). */
    Optional<ValoracionTutor> findByTutorIdAndUsuarioId(Long tutorId, Long usuarioId);
}
