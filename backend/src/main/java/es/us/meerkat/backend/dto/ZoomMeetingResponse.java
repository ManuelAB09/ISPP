package es.us.meerkat.backend.dto;

import java.time.LocalDateTime;

/** Respuesta de una reunion Zoom gestionada por la app. */
public record ZoomMeetingResponse(
        Long id,
        String zoomMeetingId,
        String topic,
        String joinUrl,
        String password,
        String status,
        Long communityId,
        String communityName,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime endedAt) {}
