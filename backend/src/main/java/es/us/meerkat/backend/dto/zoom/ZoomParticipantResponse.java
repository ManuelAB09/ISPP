package es.us.meerkat.backend.dto.zoom;

import java.time.LocalDateTime;

/** Participante actual o historico de una llamada Zoom. */
public record ZoomParticipantResponse(
        Long userId,
        String displayName,
        String email,
        Boolean inCall,
        LocalDateTime joinedAt,
        LocalDateTime leftAt) {}
