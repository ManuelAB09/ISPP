package es.us.meerkat.backend.entity;

/** Enumeración que representa los tipos de alertas para eventos próximos. */
public enum TipoAlerta {

    /** Alerta generada cuando el evento comienza en menos de 24 horas. */
    PROXIMA_24H("El evento comienza en menos de 24 horas"),

    /** Alerta generada cuando el evento comienza en 15 minutos o menos. */
    INMINENTE_15MIN("El evento comienza en 15 minutos"),

    /** Alerta generada cuando se publica un anuncio en la comunidad. */
    ANUNCIO("Nuevo anuncio en tu comunidad");

    /** Descripción legible del tipo de alerta. */
    private final String descripcion;

    TipoAlerta(final String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
