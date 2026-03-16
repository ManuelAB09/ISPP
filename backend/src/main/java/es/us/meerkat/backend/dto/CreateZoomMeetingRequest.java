package es.us.meerkat.backend.dto;

/** Peticion para crear una reunion Zoom en una comunidad. */
public record CreateZoomMeetingRequest(String topic, Integer durationMinutes) {}
