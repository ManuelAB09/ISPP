package es.us.meerkat.backend.repository.google;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import es.us.meerkat.backend.entity.google.ClassroomLinkRequest;
import es.us.meerkat.backend.entity.google.ClassroomLinkRequestStatus;

public interface ClassroomLinkRequestRepository extends JpaRepository<ClassroomLinkRequest, Long> {

    List<ClassroomLinkRequest> findByTutorIdAndEstado(
            Long tutorId, ClassroomLinkRequestStatus estado);

    Optional<ClassroomLinkRequest> findByIdAndTutorId(Long id, Long tutorId);

    boolean existsByComunidadIdAndTutorIdAndEstado(
            Long comunidadId, Long tutorId, ClassroomLinkRequestStatus estado);
}
