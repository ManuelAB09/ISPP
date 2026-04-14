package es.us.meerkat.backend.dto.zoom;

import java.time.LocalDateTime;

/** Respuesta de una reunion Zoom gestionada por la app. */
public record ZoomMeetingResponse(
        Long id,
        String zoomMeetingId,
        String topic,
        String joinUrl,
        String startUrl,
        String password,
        String status,
        Long communityId,
        String communityName,
        Integer durationMinutes,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime endedAt) {}
