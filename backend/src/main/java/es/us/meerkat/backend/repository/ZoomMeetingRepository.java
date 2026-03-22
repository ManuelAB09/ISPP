package es.us.meerkat.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import es.us.meerkat.backend.entity.ZoomMeeting;
import es.us.meerkat.backend.entity.ZoomMeetingStatus;

/** Repositorio de reuniones Zoom. */
public interface ZoomMeetingRepository extends JpaRepository<ZoomMeeting, Long> {

    Optional<ZoomMeeting> findFirstByComunidadIdAndStatusOrderByCreatedAtDesc(
            Long comunidadId, ZoomMeetingStatus status);

    List<ZoomMeeting> findByComunidadIdOrderByCreatedAtDesc(Long comunidadId);

    Optional<ZoomMeeting> findByZoomMeetingId(String zoomMeetingId);

    Optional<ZoomMeeting> findFirstByEventoIdAndStatusOrderByCreatedAtDesc(
            Long eventoId, ZoomMeetingStatus status);

    List<ZoomMeeting> findByEventoIdOrderByCreatedAtDesc(Long eventoId);

    /** Elimina todas las reuniones Zoom de un evento. */
    void deleteByEventoId(Long eventoId);
}
