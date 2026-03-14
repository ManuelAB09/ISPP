package es.us.meerkat.backend.entity;

/** Canal por el que se enviará una alarma personalizada. */
public enum TipoCanal {

    /** Solo notificación en plataforma (alert en la app). */
    PLATAFORMA,

    /** Solo email. */
    EMAIL,

    /** Ambos: notificación en plataforma y email. */
    AMBOS
}
