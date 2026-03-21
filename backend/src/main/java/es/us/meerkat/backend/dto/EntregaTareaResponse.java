package es.us.meerkat.backend.dto;

import java.time.LocalDateTime;

/** Response DTO para una entrega de tarea. */
public record EntregaTareaResponse(
        Long id,
        Long tareaId,
        String tareaTitulo,
        Long alumnoId,
        String alumnoNombre,
        String urlArchivo,
        String contenido,
        String estado,
        Integer calificacion,
        String comentarioRetroalimentacion,
        Boolean sincronizadoClassroom,
        LocalDateTime createdAt,
        LocalDateTime fechaCalificacion) {}
