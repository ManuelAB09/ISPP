package es.us.meerkat.backend.dto.forms;

import java.util.List;

import lombok.Data;

/** DTO para enviar un intento de cuestionario desde el cliente. */
@Data
public class SubmitAttemptRequest {
    /** Lista de respuestas por pregunta. */
    private List<Answer> answers;

    @Data
    public static class Answer {
        private Long preguntaId;

        /** Ids de opciones seleccionadas (para TEST / V/F). */
        private List<Long> opcionIds;

        /** Texto de respuesta (para respuesta corta). */
        private String respuestaTexto;
    }
}
