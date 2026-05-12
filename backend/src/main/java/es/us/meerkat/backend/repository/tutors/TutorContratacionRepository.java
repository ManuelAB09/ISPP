package es.us.meerkat.backend.repository.tutors;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import es.us.meerkat.backend.entity.tutors.TutorContratacion;

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
    Optional<TutorContratacion> findActivaByComunidadId(Long comunidadId);

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

    /** Elimina todas las contrataciones de una comunidad (bulk). */
    @Modifying
    @Query("DELETE FROM TutorContratacion tc WHERE tc.comunidad.id = :comunidadId")
    void deleteByComunidadId(@Param("comunidadId") Long comunidadId);
}
