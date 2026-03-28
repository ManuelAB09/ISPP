package es.us.meerkat.backend.entity.events;

/**
 * Enumeración que representa los distintos tipos de eventos en la plataforma.
 *
 * <p>Cada tipo tiene un nombre descriptivo y un icono asociado para su visualización en el
 * frontend.
 */
public enum TipoEvento {

    /** Reunión de estudio o de comunidad. */
    REUNION("Reunión", "👥"),

    /** Examen o prueba formal. */
    EXAMEN("Examen", "📝"),

    /** Cuestionario o quiz. */
    CUESTIONARIO("Cuestionario", "❓"),

    /** Sesión de tutoría. */
    TUTORIA("Tutoría", "🎓"),

    /** Clase o sesión educativa. */
    CLASE("Clase", "📚"),

    /** Otro tipo de evento no categorizado. */
    OTRO("Otro", "📅");

    /** Nombre legible del tipo de evento. */
    private final String nombre;

    /** Icono emoji representativo del tipo de evento. */
    private final String icono;

    TipoEvento(final String nombre, final String icono) {
        this.nombre = nombre;
        this.icono = icono;
    }

    public String getNombre() {
        return nombre;
    }

    public String getIcono() {
        return icono;
    }
}
