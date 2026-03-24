package es.us.meerkat.backend.dto;

import java.util.List;

import es.us.meerkat.backend.entity.NivelDificultad;
import es.us.meerkat.backend.entity.TipoPregunta;
import jakarta.validation.constraints.NotBlank;
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

    private Integer tiempoEstimadoMinutos;

    private Boolean activo;

    private Boolean publicado;

    private List<PreguntaRequest> preguntas;

    /** Lista de ids de comunidades asociadas */
    private List<Long> comunidadesIds;

    /** Lista de ids de usuarios (alumnos) asociados */
    private List<Long> alumnosIds;

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
