package es.us.meerkat.backend.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import es.us.meerkat.backend.entity.Recomendacion;
import es.us.meerkat.backend.entity.TipoRecomendacion;

/** Repositorio JPA para la entidad {@link Recomendacion}. */
public interface RecomendacionRepository extends JpaRepository<Recomendacion, Long> {

    /** Obtiene recomendaciones activas (no expiradas) de un usuario. */
    @Query(
            """
            SELECT r FROM Recomendacion r
            WHERE r.usuario.id = :usuarioId
            AND (r.fechaExpiracion IS NULL OR r.fechaExpiracion > CURRENT_TIMESTAMP)
            ORDER BY r.puntuacionRelevancia DESC
            """)
    Page<Recomendacion> findRecomendacionesActivas(
            @Param("usuarioId") Long usuarioId, Pageable pageable);

    /** Obtiene recomendaciones de un tipo específico. */
    @Query(
            """
            SELECT r FROM Recomendacion r
            WHERE r.usuario.id = :usuarioId
            AND r.tipo = :tipo
            AND (r.fechaExpiracion IS NULL OR r.fechaExpiracion > CURRENT_TIMESTAMP)
            ORDER BY r.puntuacionRelevancia DESC
            """)
    Page<Recomendacion> findPorTipo(
            @Param("usuarioId") Long usuarioId,
            @Param("tipo") TipoRecomendacion tipo,
            Pageable pageable);

    /** Obtiene recomendaciones no vistas aún. */
    List<Recomendacion> findByUsuarioIdAndVistaFalseOrderByPuntuacionRelevanciaDesc(Long usuarioId);

    /** Obtiene recomendaciones bien valoradas por el usuario. */
    @Query(
            """
            SELECT r FROM Recomendacion r
            WHERE r.usuario.id = :usuarioId
            AND r.esFavorable = true
            ORDER BY r.updatedAt DESC
            """)
    List<Recomendacion> findFavorables(@Param("usuarioId") Long usuarioId);

    /** Obtiene recomendaciones no bien valoradas. */
    @Query(
            """
            SELECT r FROM Recomendacion r
            WHERE r.usuario.id = :usuarioId
            AND r.esFavorable = false
            ORDER BY r.updatedAt DESC
            """)
    List<Recomendacion> findNoFavorables(@Param("usuarioId") Long usuarioId);

    /** Cuenta recomendaciones activas de un tipo para un usuario. */
    @Query(
            """
            SELECT COUNT(r) FROM Recomendacion r
            WHERE r.usuario.id = :usuarioId
            AND r.tipo = :tipo
            AND (r.fechaExpiracion IS NULL OR r.fechaExpiracion > CURRENT_TIMESTAMP)
            """)
    long countActivasPorTipo(
            @Param("usuarioId") Long usuarioId, @Param("tipo") TipoRecomendacion tipo);

    /** Busca una recomendación específica. */
    @Query(
            """
            SELECT r FROM Recomendacion r
            WHERE r.usuario.id = :usuarioId
            AND r.tipo = :tipo
            AND r.idObjetoRecomendado = :idObjeto
            """)
    List<Recomendacion> findByUsuarioTipoObjeto(
            @Param("usuarioId") Long usuarioId,
            @Param("tipo") TipoRecomendacion tipo,
            @Param("idObjeto") Long idObjeto);
}
