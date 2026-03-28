package es.us.meerkat.backend.dto.users;

import java.time.LocalDateTime;
import java.util.List;

import es.us.meerkat.backend.dto.events.AttendanceResponse;
import es.us.meerkat.backend.dto.recommendations.FeedbackResponse;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserActivityResponse {
    private List<QuizResult> cuestionarios;
    private List<AttendanceResponse> asistencias;
    private java.util.List<FeedbackResponse> feedbacks;

    @Data
    @Builder
    public static class QuizResult {
        private Long intentoId;
        private Long cuestionarioId;
        private String titulo;
        private Double puntuacion;
        private LocalDateTime fecha;
    }
}
