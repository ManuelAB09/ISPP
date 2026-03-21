package es.us.meerkat.backend.dto;

import java.time.LocalDateTime;

/** Response DTO para una tarea de Google Classroom. */
public record TareaClassroomResponse(
        Long id,
        String titulo,
        String descripcion,
        LocalDateTime fechaVencimiento,
        Integer puntosMaximos,
        Boolean publicada,
        String classroomCourseWorkId,
        String estado,
        Long creadaPorId,
        String creadaPorNombre,
        LocalDateTime createdAt,
        long totalEntregas) {}
