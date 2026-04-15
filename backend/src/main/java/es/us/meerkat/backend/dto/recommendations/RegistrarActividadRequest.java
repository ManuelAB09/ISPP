package es.us.meerkat.backend.dto.recommendations;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request para registrar una actividad del usuario que alimentará el motor de recomendaciones.
 *
 * <p>Ejemplos de tipoActividad: BUSQUEDA, VISUALIZACION, CLIC, LIKE, UNIRSE, QUIZ_COMPLETADO,
 * CONTENIDO_COMPLETADO
 *
 * <p>Ejemplos de categoriaObjeto: Tutor, Comunidad, Contenido, Cuestionario
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrarActividadRequest {

    @NotBlank(message = "El tipo de actividad es obligatorio")
    private String tipoActividad;

    @NotBlank(message = "La categoría del objeto es obligatoria")
    private String categoriaObjeto;

    /** ID del recurso con el que interactuó (puede ser null en búsquedas genéricas) */
    private Long idObjeto;

    /** Términos buscados (para BUSQUEDA) o materia relacionada */
    private String terminosBusqueda;

    /** Duración de visualización en segundos */
    private Long duracionSegundos;

    /**
     * Datos adicionales en formato libre. Para QUIZ_COMPLETADO: puntuación obtenida (0.0 - 1.0)
     * como string. Ejemplo: "0.85"
     */
    private String datosAdicionales;
}
