package es.us.meerkat.backend.dto.forms;

import java.util.List;

import es.us.meerkat.backend.entity.forms.NivelDificultad;
import es.us.meerkat.backend.entity.recommendations.TipoPregunta;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO para crear un Cuestionario. Comunidades y alumnos se pasan como listas de ids. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCuestionarioRequest {

    @NotBlank private String titulo;

    private String descripcion;

    private String imagenUrl;

    @NotBlank private String materia;

    private List<String> tags;

    private NivelDificultad dificultad;

    private String nivelEducativo;

    private Integer numPreguntas;

    @Min(value = 1, message = "El tiempo estimado debe ser al menos 1 minuto")
    @Max(value = 1440, message = "El tiempo estimado no puede superar 1440 minutos (24 horas)")
    private Integer tiempoEstimadoMinutos;

    private Boolean activo;

    private Boolean publicado;

    private List<PreguntaRequest> preguntas;

    /** Lista de ids de comunidades asociadas */
    private List<Long> comunidadesIds;

    /** Lista de ids de usuarios (alumnos) asociados */
    private List<Long> alumnosIds;

    /** Lista de emails de alumnos asociados (alternativa a alumnosIds) */
    private List<String> alumnosEmails;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PreguntaRequest {
        private String enunciado;
        private TipoPregunta tipo;
        private List<OpcionRequest> opciones;
        private List<String> respuestasAceptables;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OpcionRequest {
        private String texto;
        private Integer orden;
        private Boolean correcta;
    }
}
