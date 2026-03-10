package es.us.meerkat.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import es.us.meerkat.backend.entity.ZoomMeeting;
import es.us.meerkat.backend.entity.ZoomMeetingStatus;

/** Repositorio de reuniones Zoom. */
public interface ZoomMeetingRepository extends JpaRepository<ZoomMeeting, Long> {

    Optional<ZoomMeeting> findFirstByComunidadIdAndStatusOrderByCreatedAtDesc(
            Long comunidadId, ZoomMeetingStatus status);

    Optional<ZoomMeeting> findByZoomMeetingId(String zoomMeetingId);
}
