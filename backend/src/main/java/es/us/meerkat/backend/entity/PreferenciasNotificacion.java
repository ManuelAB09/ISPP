package es.us.meerkat.backend.entity;

import jakarta.persistence.Entity;
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
