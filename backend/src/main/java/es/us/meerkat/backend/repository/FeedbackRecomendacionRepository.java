package es.us.meerkat.backend.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import es.us.meerkat.backend.entity.FeedbackRecomendacion;

/** Repositorio JPA para la entidad {@link FeedbackRecomendacion}. */
public interface FeedbackRecomendacionRepository
        extends JpaRepository<FeedbackRecomendacion, Long> {

    /** Obtiene el feedback de un usuario para una recomendación. */
    Optional<FeedbackRecomendacion> findByRecomendacionIdAndUsuarioId(
            Long recomendacionId, Long usuarioId);

    /** Obtiene todos los feedbacks de un usuario. */
    List<FeedbackRecomendacion> findByUsuarioIdOrderByCreatedAtDesc(Long usuarioId);

    /** Obtiene feedbacks positivos de un usuario. */
    List<FeedbackRecomendacion> findByUsuarioIdAndEsUtilTrueOrderByCreatedAtDesc(Long usuarioId);

    /** Obtiene feedbacks negativos de un usuario. */
    List<FeedbackRecomendacion> findByUsuarioIdAndEsUtilFalseOrderByCreatedAtDesc(Long usuarioId);

    /** Obtiene promedio de satisfacción de un usuario. */
    @Query(
            """
            SELECT AVG(f.satisfaccion) FROM FeedbackRecomendacion f
            WHERE f.usuario.id = :usuarioId AND f.satisfaccion IS NOT NULL
            """)
    Double getPromedioSatisfaccion(@Param("usuarioId") Long usuarioId);

    /** Obtiene el percentage de útil para un usuario. */
    @Query(
            """
            SELECT (COUNT(CASE WHEN f.esUtil = true THEN 1 END) * 100.0 / COUNT(f))
            FROM FeedbackRecomendacion f
            WHERE f.usuario.id = :usuarioId
            """)
    Double getPercentageUtilidad(@Param("usuarioId") Long usuarioId);

    /** Obtiene feedbacks dentro de un período. */
    @Query(
            """
            SELECT f FROM FeedbackRecomendacion f
            WHERE f.usuario.id = :usuarioId
            AND f.createdAt BETWEEN :desde AND :hasta
            ORDER BY f.createdAt DESC
            """)
    List<FeedbackRecomendacion> findEnPeriodo(
            @Param("usuarioId") Long usuarioId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
}
