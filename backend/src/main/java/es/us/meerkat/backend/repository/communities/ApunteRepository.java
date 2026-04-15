package es.us.meerkat.backend.repository.communities;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import es.us.meerkat.backend.entity.communities.Apunte;

/** Repositorio para la entidad Apunte. */
@Repository
public interface ApunteRepository extends JpaRepository<Apunte, Long> {

    /**
     * Obtiene todos los apuntes de una comunidad.
     *
     * @param comunidadId ID de la comunidad
     * @param pageable Información de paginación
     * @return Página con los apuntes de la comunidad
     */
    Page<Apunte> findByComunidadIdOrderByCreatedAtDesc(Long comunidadId, Pageable pageable);

    /**
     * Obtiene todos los apuntes de una comunidad (sin paginación).
     *
     * @param comunidadId ID de la comunidad
     * @return Lista de apuntes ordenados por fecha de creación descendente
     */
    List<Apunte> findByComunidadIdOrderByCreatedAtDesc(Long comunidadId);

    /**
     * Obtiene los apuntes subidos por un usuario en una comunidad.
     *
     * @param comunidadId ID de la comunidad
     * @param usuarioId ID del usuario
     * @return Lista de apuntes del usuario
     */
    List<Apunte> findByComunidadIdAndUsuarioIdOrderByCreatedAtDesc(
            Long comunidadId, Long usuarioId);

    /**
     * Busca apuntes por título en una comunidad (búsqueda case-insensitive).
     *
     * @param comunidadId ID de la comunidad
     * @param titulo Título a buscar
     * @param pageable Información de paginación
     * @return Página con los apuntes que coinciden
     */
    @Query(
            "SELECT a FROM Apunte a WHERE a.comunidad.id = :comunidadId AND LOWER(a.titulo) LIKE"
                    + " LOWER(CONCAT('%', :titulo, '%')) ORDER BY a.createdAt DESC")
    Page<Apunte> searchByTituloInComunidad(
            @Param("comunidadId") Long comunidadId,
            @Param("titulo") String titulo,
            Pageable pageable);

    /**
     * Cuenta los apuntes de una comunidad.
     *
     * @param comunidadId ID de la comunidad
     * @return Número de apuntes
     */
    Long countByComunidadId(Long comunidadId);

    /**
     * Cuenta los apuntes subidos por un usuario.
     *
     * @param usuarioId ID del usuario
     * @return Número de apuntes del usuario
     */
    Long countByUsuarioId(Long usuarioId);
}
