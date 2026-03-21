package es.us.meerkat.backend.dto;

/** Request DTO para agregar una grabación de clase. */
public record AgregarGrabacionRequest(
        String titulo,
        String descripcion,
        String urlGrabacion,
        String googleDriveFileId,
        Long durationSeconds,
        Boolean compartirEnClassroom) {}
