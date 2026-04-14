package es.us.meerkat.backend.entity.notifications;

import java.time.LocalDateTime;

import es.us.meerkat.backend.entity.events.Evento;
import es.us.meerkat.backend.entity.users.Usuario;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Alarma personalizada que un usuario configura para un evento concreto.
 *
 * <p>A diferencia de los recordatorios globales (configurados en preferencias), estas alarmas son
 * específicas por evento y las elige el usuario manualmente, ya sea al confirmar asistencia o desde
 * la página del evento en cualquier momento.
 *
 * <p>Cada alarma define:
 *
 * <ul>
 *   <li>Cuántos minutos antes del evento debe dispararse.
 *   <li>Por qué canal: PLATAFORMA, EMAIL o AMBOS.
 *   <li>Si ya fue disparada (para el anti-duplicado del scheduler).
 * </ul>
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlarmaPersonalizada {

    /** Identificador único de la alarma. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Minutos de antelación antes del inicio del evento en que se dispara la alarma. Ejemplos: 2880
     * = 2 días, 1440 = 1 día, 120 = 2 horas, 30 = 30 minutos.
     */
    private Integer minutosAntes;

    /** Canal de notificación elegido por el usuario. PLATAFORMA, EMAIL o AMBOS. */
    @Enumerated(EnumType.STRING)
    private TipoCanal canal;

    /** Fecha y hora calculada en que debe dispararse la alarma. Se calcula al guardar. */
    private LocalDateTime fechaDisparo;

    /** Si la alarma ya fue disparada por el scheduler. Evita disparos duplicados. */
    private Boolean disparada;

    /** Fecha y hora en que se disparó realmente la alarma. */
    private LocalDateTime disparadaAt;

    /** Fecha y hora en que se creó la alarma. */
    private LocalDateTime createdAt;

    /** Evento al que pertenece esta alarma. */
    @ManyToOne
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    /** Usuario que configuró la alarma. */
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.disparada == null) {
            this.disparada = false;
        }
        // Calcular fechaDisparo a partir del evento y los minutos de antelación
        if (this.evento != null
                && this.evento.getFechaHora() != null
                && this.minutosAntes != null) {
            this.fechaDisparo = this.evento.getFechaHora().minusMinutes(this.minutosAntes);
        }
    }

    /** Marca la alarma como disparada. */
    public void marcarDisparada() {
        this.disparada = true;
        this.disparadaAt = LocalDateTime.now();
    }

    /**
     * Etiqueta legible de la antelación para mostrar en la UI. Ej: 2880 → "2 días antes", 60 → "1
     * hora antes".
     */
    public String getEtiqueta() {
        if (minutosAntes == null) {
            return "";
        }
        ;
        if (minutosAntes >= 1440 && minutosAntes % 1440 == 0) {
            long dias = minutosAntes / 1440;
            return dias == 1 ? "1 día antes" : dias + " días antes";
        }
        if (minutosAntes >= 60 && minutosAntes % 60 == 0) {
            long horas = minutosAntes / 60;
            return horas == 1 ? "1 hora antes" : horas + " horas antes";
        }
        return minutosAntes + " minutos antes";
    }
}
