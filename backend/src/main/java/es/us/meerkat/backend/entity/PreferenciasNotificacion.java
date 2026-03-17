package es.us.meerkat.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad que almacena las preferencias de notificación por email de un usuario.
 *
 * <p>Permite a cada usuario configurar qué recordatorios quiere recibir y si desea recibirlos en
 * absoluto.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreferenciasNotificacion {

    /** Identificador único. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Usuario al que pertenecen estas preferencias. Relación 1:1. */
    @OneToOne
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    /** Si el usuario desea recibir emails de recordatorio en general. */
    private Boolean emailsActivados;

    /** Si desea recibir recordatorio 24 horas antes del evento. */
    private Boolean recordatorio24h;

    /** Si desea recibir recordatorio 1 hora antes del evento. */
    private Boolean recordatorio1h;

    /** Si desea recibir recordatorio 30 minutos antes del evento. */
    private Boolean recordatorio30min;

    /** Frecuencia de notificaciones. SIEMPRE, RESUMEN_DIARIO, NUNCA */
    @Enumerated(EnumType.STRING)
    private FrecuenciaNotificacion frecuenciaMensajeComunidad;

    /** Frecuencia de notificaciones. SIEMPRE, RESUMEN_DIARIO, NUNCA */
    @Enumerated(EnumType.STRING)
    private FrecuenciaNotificacion frecuenciaMenciones;

    /** Frecuencia de notificaciones. SIEMPRE, RESUMEN_DIARIO, NUNCA */
    @Enumerated(EnumType.STRING)
    private FrecuenciaNotificacion frecuenciaInvitaciones;

    /** Frecuencia de notificaciones. SIEMPRE, RESUMEN_DIARIO, NUNCA */
    @Enumerated(EnumType.STRING)
    private FrecuenciaNotificacion frecuenciaAnuncios;

    /** Frecuencia de notificaciones. SIEMPRE, RESUMEN_DIARIO, NUNCA */
    @Enumerated(EnumType.STRING)
    private FrecuenciaNotificacion frecuenciaSolicitudAcceso;

    /** Frecuencia de notificaciones. SIEMPRE, RESUMEN_DIARIO, NUNCA */
    @Enumerated(EnumType.STRING)
    private FrecuenciaNotificacion frecuenciaCambiosDeEventos;

    /**
     * Canal por defecto para las alarmas personalizadas. Se usa cuando el usuario crea una alarma
     * sin especificar canal. Por defecto: AMBOS.
     */
    @Enumerated(EnumType.STRING)
    private TipoCanal canalAlarmasPorDefecto;

    /**
     * Inicializa preferencias por defecto: todo activado. Se aplica al crear la entidad por primera
     * vez.
     */
    @PrePersist
    public void prePersist() {
        if (this.emailsActivados == null) {
            this.emailsActivados = true;
        }
        if (this.recordatorio24h == null) {
            this.recordatorio24h = true;
        }
        if (this.recordatorio1h == null) {
            this.recordatorio1h = true;
        }
        if (this.recordatorio30min == null) {
            this.recordatorio30min = false;
        }
        if (this.canalAlarmasPorDefecto == null) {
            this.canalAlarmasPorDefecto = TipoCanal.AMBOS;
        }
        if (this.frecuenciaMensajeComunidad == null) {
            this.frecuenciaMensajeComunidad = FrecuenciaNotificacion.NUNCA;
        }
        if (this.frecuenciaMenciones == null) {
            this.frecuenciaMenciones = FrecuenciaNotificacion.NUNCA;
        }
        if (this.frecuenciaInvitaciones == null) {
            this.frecuenciaInvitaciones = FrecuenciaNotificacion.NUNCA;
        }
        if (this.frecuenciaAnuncios == null) {
            this.frecuenciaAnuncios = FrecuenciaNotificacion.NUNCA;
        }
        if (this.frecuenciaSolicitudAcceso == null) {
            this.frecuenciaSolicitudAcceso = FrecuenciaNotificacion.NUNCA;
        }
        if (this.frecuenciaCambiosDeEventos == null) {
            this.frecuenciaCambiosDeEventos = FrecuenciaNotificacion.NUNCA;
        }
    }

    /**
     * Indica si el usuario quiere recibir un recordatorio del tipo dado.
     *
     * @param tipo Tipo de recordatorio a comprobar.
     * @return true si debe enviarse ese recordatorio.
     */
    public boolean quiereRecordatorio(final TipoRecordatorio tipo) {
        if (!Boolean.TRUE.equals(emailsActivados)) {
            return false;
        }
        return switch (tipo) {
            case HORAS_24 -> Boolean.TRUE.equals(recordatorio24h);
            case HORA_1 -> Boolean.TRUE.equals(recordatorio1h);
            case MINUTOS_30 -> Boolean.TRUE.equals(recordatorio30min);
        };
    }
}
