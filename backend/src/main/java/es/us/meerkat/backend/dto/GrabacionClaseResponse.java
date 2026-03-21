package es.us.meerkat.backend.dto;

import java.time.LocalDateTime;

/** Response DTO para una grabación de clase. */
public record GrabacionClaseResponse(
        Long id,
        String titulo,
        String descripcion,
        String urlGrabacion,
        String googleDriveFileId,
        Long durationSeconds,
        Boolean sincronizadoClassroom,
        String estado,
        Long subidoPorId,
        String subidoPorNombre,
        LocalDateTime createdAt) {}
