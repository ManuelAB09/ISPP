package es.us.meerkat.backend.dto;

import java.time.LocalDateTime;

/** Respuesta de grabaciones de Zoom disponibles para la app. */
public record ZoomRecordingResponse(
        String zoomRecordingId,
        String zoomMeetingId,
        Long communityId,
        String communityName,
        String fileType,
        String playUrl,
        String downloadUrl,
        Boolean storedInApp,
        String localFilePath,
        Long fileSizeBytes,
        LocalDateTime recordingStart,
        LocalDateTime recordingEnd,
        LocalDateTime createdAt) {}
