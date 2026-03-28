package es.us.meerkat.backend.repository.google;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import es.us.meerkat.backend.entity.google.RecursoClassroom;

/** Repositorio JPA para la entidad {@link RecursoClassroom}. */
public interface RecursoClassroomRepository extends JpaRepository<RecursoClassroom, Long> {

    /** Obtiene todos los recursos visibles de una comunidad, ordenados. */
    List<RecursoClassroom> findByComunidadIdAndVisibleTrueOrderByOrdenAsc(Long comunidadId);

    /** Obtiene todos los recursos de una comunidad (incluyendo no visibles). */
    List<RecursoClassroom> findByComunidadIdOrderByOrdenAsc(Long comunidadId);

    /** Obtiene recursos de un tipo específico. */
    List<RecursoClassroom> findByComunidadIdAndTipoOrderByOrdenAsc(Long comunidadId, String tipo);

    /** Obtiene un recurso por su ID en Classroom. */
    Optional<RecursoClassroom> findByClassroomResourceId(String classroomResourceId);

    /** Obtiene un recurso por su ID en Google Drive. */
    Optional<RecursoClassroom> findByGoogleDriveFileId(String googleDriveFileId);

    /** Cuenta recursos en una comunidad. */
    long countByComunidadId(Long comunidadId);

    /** Encuentra el orden máximo en una comunidad. */
    Integer findMaxOrdenByComunidadId(Long comunidadId);
}
