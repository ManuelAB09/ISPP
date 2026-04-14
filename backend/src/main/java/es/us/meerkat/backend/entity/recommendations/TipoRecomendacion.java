package es.us.meerkat.backend.entity.recommendations;

/** Tipos de recomendaciones que el sistema puede generar. */
public enum TipoRecomendacion {
    /** Recomendación de un profesor/tutor */
    PROFESOR,
    /** Recomendación de contenido educativo */
    CONTENIDO,
    /** Recomendación de un cuestionario o quiz */
    CUESTIONARIO,
    /** Recomendación de una comunidad */
    COMUNIDAD,
    /** Evento recomendado */
    EVENTO
}
