package es.us.meerkat.backend.repository.recommendations;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import es.us.meerkat.backend.entity.Recomendacion;
import es.us.meerkat.backend.entity.TipoRecomendacion;

@Repository
public interface RecomendacionRepository extends JpaRepository<Recomendacion, Long> {

    /** Recomendaciones activas (no expiradas) por tipo. */
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

    /** Todas las recomendaciones activas del usuario. */
    @Query(
            """
                SELECT r FROM Recomendacion r
                WHERE r.usuario.id = :usuarioId
                  AND (r.fechaExpiracion IS NULL OR r.fechaExpiracion > CURRENT_TIMESTAMP)
                ORDER BY r.puntuacionRelevancia DESC
            """)
    Page<Recomendacion> findRecomendacionesActivas(
            @Param("usuarioId") Long usuarioId, Pageable pageable);

    /** Verificar si ya existe recomendación para evitar duplicados. */
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

    /** Eliminar recomendaciones expiradas de un usuario. */
    @Modifying
    @Query(
            """
                DELETE FROM Recomendacion r
                WHERE r.usuario.id = :usuarioId
                  AND r.fechaExpiracion IS NOT NULL
                  AND r.fechaExpiracion < :ahora
            """)
    void deleteExpiradas(@Param("usuarioId") Long usuarioId, @Param("ahora") LocalDateTime ahora);

    /** Eliminar recomendaciones de un tipo para regenerarlas. */
    @Modifying
    @Query("DELETE FROM Recomendacion r WHERE r.usuario.id = :usuarioId AND r.tipo = :tipo")
    void deleteByUsuarioIdAndTipo(
            @Param("usuarioId") Long usuarioId, @Param("tipo") TipoRecomendacion tipo);

    /** Contar feedback favorable/desfavorable para ajuste de algoritmo. */
    long countByUsuarioIdAndEsFavorable(Long usuarioId, Boolean esFavorable);

    /** Top N recomendaciones cross-tipo para sección "Para Ti". */
    @Query(
            """
                SELECT r FROM Recomendacion r
                WHERE r.usuario.id = :usuarioId
                  AND (r.fechaExpiracion IS NULL OR r.fechaExpiracion > CURRENT_TIMESTAMP)
                ORDER BY r.puntuacionRelevancia DESC
                LIMIT :limite
            """)
    List<Recomendacion> findTopParaTi(
            @Param("usuarioId") Long usuarioId, @Param("limite") int limite);

    /** Recomendaciones no vistas — para badge en el frontend. */
    @Query(
            """
                SELECT r FROM Recomendacion r
                WHERE r.usuario.id = :usuarioId
                  AND r.vista = false
                  AND (r.fechaExpiracion IS NULL OR r.fechaExpiracion > CURRENT_TIMESTAMP)
                ORDER BY r.puntuacionRelevancia DESC
            """)
    Page<Recomendacion> findNoVistas(@Param("usuarioId") Long usuarioId, Pageable pageable);
}
