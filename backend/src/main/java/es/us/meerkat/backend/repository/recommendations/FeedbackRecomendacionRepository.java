package es.us.meerkat.backend.repository.recommendations;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import es.us.meerkat.backend.entity.recommendations.FeedbackRecomendacion;

@Repository
public interface FeedbackRecomendacionRepository
        extends JpaRepository<FeedbackRecomendacion, Long> {

    /** Buscar feedback existente para evitar duplicados (un usuario, una recomendación). */
    Optional<FeedbackRecomendacion> findByRecomendacionIdAndUsuarioId(
            Long recomendacionId, Long usuarioId);

    /** Contar feedbacks útiles para un usuario (para ajustar el algoritmo). */
    @Query(
            """
                SELECT COUNT(f) FROM FeedbackRecomendacion f
                WHERE f.usuario.id = :usuarioId AND f.esUtil = true
            """)
    long countUtilesByUsuario(@Param("usuarioId") Long usuarioId);

    /** Contar feedbacks no útiles. */
    @Query(
            """
                SELECT COUNT(f) FROM FeedbackRecomendacion f
                WHERE f.usuario.id = :usuarioId AND f.esUtil = false
            """)
    long countNoUtilesByUsuario(@Param("usuarioId") Long usuarioId);
}
