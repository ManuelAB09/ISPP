package es.us.meerkat.backend.dto.zoom;

/** Datos de acceso que la app da automaticamente a un miembro para entrar en Zoom. */
public record ZoomJoinResponse(
        String zoomMeetingId, String topic, String joinUrl, String password) {}
