package es.us.meerkat.backend.dto;

import java.time.LocalDateTime;

/** Llamadas activas donde esta un usuario. */
public record ZoomUserCallResponse(
        Long communityId,
        String communityName,
        String zoomMeetingId,
        String topic,
        LocalDateTime joinedAt) {}
