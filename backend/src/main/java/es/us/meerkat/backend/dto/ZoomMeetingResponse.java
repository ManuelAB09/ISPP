package es.us.meerkat.backend.dto;

/** Respuesta de una reunion Zoom gestionada por la app. */
public record ZoomMeetingResponse(
        Long id,
        String zoomMeetingId,
        String topic,
        String joinUrl,
        String password,
        String status,
        Long communityId,
        String communityName) {}
