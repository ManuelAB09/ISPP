package es.us.meerkat.backend.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import es.us.meerkat.backend.entity.ActividadUsuario;

/** Repositorio JPA para la entidad {@link ActividadUsuario}. */
public interface ActividadUsuarioRepository extends JpaRepository<ActividadUsuario, Long> {

    /** Obtiene actividades recientes de un usuario. */
    List<ActividadUsuario> findByUsuarioIdOrderByCreatedAtDesc(Long usuarioId);

    /** Obtiene actividades de un usuario en un período. */
    @Query(
            """
            SELECT a FROM ActividadUsuario a
            WHERE a.usuario.id = :usuarioId
            AND a.createdAt BETWEEN :desde AND :hasta
            ORDER BY a.createdAt DESC
            """)
    List<ActividadUsuario> findEnPeriodo(
            @Param("usuarioId") Long usuarioId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    /** Obtiene actividades de un tipo específico. */
    List<ActividadUsuario> findByUsuarioIdAndTipoActividadOrderByCreatedAtDesc(
            Long usuarioId, String tipoActividad);

    /** Obtiene actividades por categoría. */
    List<ActividadUsuario> findByUsuarioIdAndCategoriaObjetoOrderByCreatedAtDesc(
            Long usuarioId, String categoriaObjeto);

    /** Obtiene búsquedas de un usuario. */
    @Query(
            """
            SELECT a FROM ActividadUsuario a
            WHERE a.usuario.id = :usuarioId
            AND a.tipoActividad = 'BUSQUEDA'
            AND a.terminosBusqueda IS NOT NULL
            ORDER BY a.createdAt DESC
            """)
    List<ActividadUsuario> findBusquedas(@Param("usuarioId") Long usuarioId);

    /** Obtiene interacciones recientes (últimos N días). */
    @Query(
            """
            SELECT a FROM ActividadUsuario a
            WHERE a.usuario.id = :usuarioId
            AND a.createdAt > :desde
            AND a.tipoActividad IN ('CLIC', 'LIKE', 'UNIRSE', 'CREAR')
            ORDER BY a.createdAt DESC
            """)
    List<ActividadUsuario> findInteraccionesRecientes(
            @Param("usuarioId") Long usuarioId, @Param("desde") LocalDateTime desde);

    /** Cuenta actividades de un usuario. */
    long countByUsuarioId(Long usuarioId);

    /** Obtiene los temas/categorías más buscados. */
    @Query(
            """
            SELECT DISTINCT a.terminosBusqueda FROM ActividadUsuario a
            WHERE a.usuario.id = :usuarioId
            AND a.tipoActividad = 'BUSQUEDA'
            AND a.terminosBusqueda IS NOT NULL
            ORDER BY a.createdAt DESC
            """)
    List<String> findTemasInteres(@Param("usuarioId") Long usuarioId);
}
