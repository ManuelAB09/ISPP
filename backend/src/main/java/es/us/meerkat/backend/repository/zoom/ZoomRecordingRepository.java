package es.us.meerkat.backend.repository.zoom;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import es.us.meerkat.backend.entity.ZoomRecording;

/** Repositorio para las grabaciones sincronizadas desde Zoom. */
public interface ZoomRecordingRepository extends JpaRepository<ZoomRecording, Long> {

    List<ZoomRecording> findByZoomMeetingIdOrderByCreatedAtDesc(Long zoomMeetingId);

    List<ZoomRecording> findByZoomMeetingComunidadIdOrderByCreatedAtDesc(Long comunidadId);

    Optional<ZoomRecording> findByZoomMeetingComunidadIdAndZoomRecordingId(
            Long comunidadId, String zoomRecordingId);

    List<ZoomRecording> findByExpiresAtBefore(LocalDateTime expiresAt);

    Optional<ZoomRecording> findByZoomRecordingId(String zoomRecordingId);
}
