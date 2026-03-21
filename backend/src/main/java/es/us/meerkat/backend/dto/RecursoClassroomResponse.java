package es.us.meerkat.backend.dto;

import java.time.LocalDateTime;

/** Response DTO para un recurso educativo de Google Classroom. */
public record RecursoClassroomResponse(
        Long id,
        String tipo,
        String titulo,
        String descripcion,
        String url,
        String googleDriveFileId,
        Boolean sincronizadoClassroom,
        Boolean visible,
        Integer orden,
        Long subidoPorId,
        String subidoPorNombre,
        LocalDateTime createdAt) {}
