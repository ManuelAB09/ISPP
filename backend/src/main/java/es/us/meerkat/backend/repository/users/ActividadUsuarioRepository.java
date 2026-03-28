package es.us.meerkat.backend.repository.users;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import es.us.meerkat.backend.entity.users.ActividadUsuario;

@Repository
public interface ActividadUsuarioRepository extends JpaRepository<ActividadUsuario, Long> {

    /** Temas más buscados/vistos por el usuario en los últimos N días. */
    @Query(
            """
                SELECT a.terminosBusqueda FROM ActividadUsuario a
                WHERE a.usuario.id = :usuarioId
                  AND a.terminosBusqueda IS NOT NULL
                  AND a.createdAt >= :desde
                GROUP BY a.terminosBusqueda
                ORDER BY COUNT(a.terminosBusqueda) DESC
            """)
    List<String> findTemasInteres(
            @Param("usuarioId") Long usuarioId, @Param("desde") LocalDateTime desde);

    /** Versión sin fecha (compatibilidad con código existente). */
    @Query(
            """
                SELECT a.terminosBusqueda FROM ActividadUsuario a
                WHERE a.usuario.id = :usuarioId
                  AND a.terminosBusqueda IS NOT NULL
                GROUP BY a.terminosBusqueda
                ORDER BY COUNT(a.terminosBusqueda) DESC
            """)
    List<String> findTemasInteres(@Param("usuarioId") Long usuarioId);

    /** Categorías de objetos más visitados por el usuario. */
    @Query(
            """
                SELECT a.categoriaObjeto, COUNT(a) as total
                FROM ActividadUsuario a
                WHERE a.usuario.id = :usuarioId
                  AND a.createdAt >= :desde
                GROUP BY a.categoriaObjeto
                ORDER BY total DESC
            """)
    List<Object[]> findCategoriasPopulares(
            @Param("usuarioId") Long usuarioId, @Param("desde") LocalDateTime desde);

    /** IDs de objetos ya visitados de una categoría (para no re-recomendar). */
    @Query(
            """
                SELECT DISTINCT a.idObjeto FROM ActividadUsuario a
                WHERE a.usuario.id = :usuarioId
                  AND a.categoriaObjeto = :categoria
                  AND a.idObjeto IS NOT NULL
            """)
    List<Long> findObjetosVisitados(
            @Param("usuarioId") Long usuarioId, @Param("categoria") String categoria);

    /** Rendimiento promedio en cuestionarios por tema. */
    @Query(
            """
                SELECT a.terminosBusqueda, AVG(CAST(a.datosAdicionales AS double))
                FROM ActividadUsuario a
                WHERE a.usuario.id = :usuarioId
                  AND a.tipoActividad = 'QUIZ_COMPLETADO'
                  AND a.terminosBusqueda IS NOT NULL
                  AND a.datosAdicionales IS NOT NULL
                GROUP BY a.terminosBusqueda
                ORDER BY AVG(CAST(a.datosAdicionales AS double)) ASC
            """)
    List<Object[]> findRendimientoQuizPorTema(@Param("usuarioId") Long usuarioId);

    /** Actividades recientes del usuario. */
    List<ActividadUsuario> findByUsuarioIdAndCreatedAtAfterOrderByCreatedAtDesc(
            Long usuarioId, LocalDateTime desde);
}
