package es.us.meerkat.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import es.us.meerkat.backend.entity.Feedback;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    Page<Feedback> findByComunidadId(Long comunidadId, Pageable pageable);

    Page<Feedback> findByAlumnoId(Long alumnoId, Pageable pageable);
}
