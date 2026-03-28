package es.us.meerkat.backend.entity.notifications;

/**
 * Enumeración de los tipos de recordatorio por email disponibles.
 *
 * <p>Cada valor representa un momento antes del inicio del evento en el que se enviará el
 * recordatorio.
 */
public enum TipoRecordatorio {

    /** Recordatorio enviado 24 horas antes del evento. */
    HORAS_24(24 * 60L, "24 horas antes"),

    /** Recordatorio enviado 1 hora antes del evento. */
    HORA_1(60L, "1 hora antes"),

    /** Recordatorio enviado 30 minutos antes del evento. */
    MINUTOS_30(30L, "30 minutos antes");

    /** Minutos de antelación respecto al inicio del evento. */
    private final long minutosAntes;

    /** Descripción legible para mostrar en preferencias. */
    private final String descripcion;

    TipoRecordatorio(final long minutosAntes, final String descripcion) {
        this.minutosAntes = minutosAntes;
        this.descripcion = descripcion;
    }

    public long getMinutosAntes() {
        return minutosAntes;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
