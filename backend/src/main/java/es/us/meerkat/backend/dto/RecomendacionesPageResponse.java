package es.us.meerkat.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response completo para la página de recomendaciones del frontend. Agrupa todas las secciones:
 * "Para Ti", Profesores, Cuestionarios, etc.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecomendacionesPageResponse {

    /** Sección "Recomendado para ti" – mezcla cross-tipo de mayor relevancia */
    private List<RecomendacionResponse> paraTi;

    /** Sección "Profesores que te pueden interesar" */
    private List<RecomendacionResponse> profesores;

    /** Sección "Contenidos recomendados" */
    private List<RecomendacionResponse> contenidos;

    /** Sección "Cuestionarios sugeridos" */
    private List<RecomendacionResponse> cuestionarios;

    /** Sección "Comunidades sugeridas" */
    private List<RecomendacionResponse> comunidades;

    /** Cuándo se generó esta respuesta */
    private LocalDateTime generadoEn;
}
