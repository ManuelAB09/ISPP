package es.us.meerkat.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import es.us.meerkat.backend.entity.ZoomMeetingParticipant;

/** Repositorio de participantes en llamadas Zoom. */
public interface ZoomMeetingParticipantRepository
        extends JpaRepository<ZoomMeetingParticipant, Long> {

    List<ZoomMeetingParticipant> findByZoomMeetingIdAndInCallTrueOrderByJoinedAtAsc(
            Long zoomMeetingId);

    List<ZoomMeetingParticipant> findByUsuarioIdAndInCallTrueOrderByJoinedAtDesc(Long usuarioId);

    Optional<ZoomMeetingParticipant>
            findFirstByZoomMeetingZoomMeetingIdAndZoomParticipantIdAndInCallTrueOrderByJoinedAtDesc(
                    String zoomMeetingId, String zoomParticipantId);

    Optional<ZoomMeetingParticipant>
            findFirstByZoomMeetingIdAndUsuarioIdAndInCallTrueOrderByJoinedAtDesc(
                    Long zoomMeetingId, Long usuarioId);
}
