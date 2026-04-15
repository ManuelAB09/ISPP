package es.us.meerkat.backend.dto.google;

import java.time.LocalDateTime;

public record ClassroomLinkRequestResponse(
        Long id, Long comunidadId, Long tutorId, String estado, LocalDateTime fechaCreacion) {}
