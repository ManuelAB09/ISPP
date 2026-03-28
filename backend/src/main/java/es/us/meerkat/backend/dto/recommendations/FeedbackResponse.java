package es.us.meerkat.backend.dto.recommendations;

import java.time.LocalDateTime;

import es.us.meerkat.backend.entity.recommendations.Feedback;

public record FeedbackResponse(
        Long id,
        Long profesorId,
        Long alumnoId,
        Long comunidadId,
        String contenido,
        Integer calificacion,
        LocalDateTime createdAt) {

    public static FeedbackResponse fromEntity(Feedback f) {
        return new FeedbackResponse(
                f.getId(),
                f.getProfesor() != null ? f.getProfesor().getId() : null,
                f.getAlumno() != null ? f.getAlumno().getId() : null,
                f.getComunidad() != null ? f.getComunidad().getId() : null,
                f.getContenido(),
                f.getCalificacion(),
                f.getCreatedAt());
    }
}
