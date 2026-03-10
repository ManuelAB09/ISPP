package es.us.meerkat.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import es.us.meerkat.backend.entity.EstadoContratacion;
import es.us.meerkat.backend.entity.TutorContratacion;

/** Repositorio para manejar operaciones de base de datos sobre contrataciones de tutores. */
@Repository
public interface TutorContratacionRepository extends JpaRepository<TutorContratacion, Long> {

    /**
     * Busca las contrataciones de un tutor específico.
     *
     * @param tutorId el ID del tutor
     * @param pageable información de paginación
     * @return página con las contrataciones
     */
    Page<TutorContratacion> findByTutorId(Long tutorId, Pageable pageable);

    /**
     * Busca las contrataciones de una comunidad específica.
     *
     * @param comunidadId el ID de la comunidad
     * @param pageable información de paginación
     * @return página con las contrataciones
     */
    Page<TutorContratacion> findByComunidadId(Long comunidadId, Pageable pageable);

    /**
     * Busca la contratación activa de una comunidad.
     *
     * @param comunidadId el ID de la comunidad
     * @return Optional con la contratación activa si la encuentra
     */
    @Query("SELECT tc FROM TutorContratacion tc WHERE tc.comunidad.id = :comunidadId AND tc.estado = :estado")
    Optional<TutorContratacion> findByComunidadIdAndEstado(@Param("comunidadId") Long comunidadId, @Param("estado") EstadoContratacion estado);
    
    default Optional<TutorContratacion> findActivaByComunidadId(Long comunidadId) {
        return findByComunidadIdAndEstado(comunidadId, EstadoContratacion.ACTIVA);
    }

    /**
     * Busca una contratación por tutor y comunidad.
     *
     * @param tutorId el ID del tutor
     * @param comunidadId el ID de la comunidad
     * @return Optional con la contratación si la encuentra
     */
    Optional<TutorContratacion> findByTutorIdAndComunidadId(Long tutorId, Long comunidadId);

    /**
     * Busca todas las contrataciones de un tutor.
     *
     * @param tutorId el ID del tutor
     * @return lista de contrataciones
     */
    List<TutorContratacion> findByTutorId(Long tutorId);

    /**
     * Busca todas las contrataciones de una comunidad.
     *
     * @param comunidadId el ID de la comunidad
     * @return lista de contrataciones
     */
    List<TutorContratacion> findByComunidadId(Long comunidadId);
    
    /**
     * Busca las solicitudes pendientes de aprobación para un tutor.
     *
     * @param tutorId el ID del tutor
     * @param estado el estado de la contratación
     * @param pageable información de paginación
     * @return página con las solicitudes
     */
    Page<TutorContratacion> findByTutorIdAndEstado(Long tutorId, EstadoContratacion estado, Pageable pageable);
    
    /**
     * Busca las solicitudes pendientes con todas las relaciones cargadas (usando EntityGraph).
     *
     * @param tutorId el ID del tutor
     * @param estado el estado de la contratación
     * @param pageable información de paginación
     * @return página con las solicitudes y sus relaciones cargadas
     */
    @EntityGraph(attributePaths = {"comunidad", "comunidad.creador", "tutor", "tutor.usuario"})
    @Query("SELECT tc FROM TutorContratacion tc WHERE tc.tutor.id = :tutorId AND tc.estado = :estado")
    Page<TutorContratacion> findByTutorIdAndEstadoWithRelations(
            @Param("tutorId") Long tutorId, 
            @Param("estado") EstadoContratacion estado, 
            Pageable pageable);
    
    /**
     * Busca una contratación específica de un tutor por ID y valida que pertenece al tutor.
     *
     * @param id el ID de la contratación
     * @param tutorId el ID del tutor
     * @return Optional con la contratación si existe y pertenece al tutor
     */
    Optional<TutorContratacion> findByIdAndTutorId(Long id, Long tutorId);
}
