package es.us.meerkat.backend.entity;

/** Enumeración que define los factores por los cuales se puede recomendar una comunidad. */
public enum FactorRecomendacion {
    /** Recomendación basada en intereses similares. */
    INTERES_SIMILAR,
    /** Recomendación basada en ubicación geográfica. */
    UBICACION,
    /** Recomendación basada en nivel educativo similar. */
    NIVEL_EDUCATIVO,
    /** Recomendación basada en comunidades similares a las del usuario. */
    COMUNIDAD_SIMILAR,
    /** Recomendación basada en popularidad o tendencia. */
    POPULARIDAD,
    /** Recomendación personalizada basada en actividad previa. */
    ACTIVIDAD_SIMILAR
}
