package es.us.meerkat.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import es.us.meerkat.backend.entity.Comunidad;
import es.us.meerkat.backend.entity.ComunidadClassroom;

/**
 * Repositorio JPA para la entidad {@link ComunidadClassroom}.
 *
 * <p>Permite gestionar la vinculación entre comunidades y cursos de Google Classroom.
 */
public interface ComunidadClassroomRepository extends JpaRepository<ComunidadClassroom, Long> {

    Optional<ComunidadClassroom> findByComunidad(Comunidad comunidad);

    Optional<ComunidadClassroom> findByComunidadId(Long comunidadId);

    boolean existsByComunidadId(Long comunidadId);

    void deleteByComunidadId(Long comunidadId);
}
