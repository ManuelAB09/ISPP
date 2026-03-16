package es.us.meerkat.backend.entity;

import java.time.LocalDateTime;

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
 * Entidad que representa una alerta generada para un usuario sobre un evento próximo.
 *
 * <p>Las alertas se generan automáticamente cuando un evento está a punto de comenzar (15 minutos
 * antes) o cuando está en las próximas 24 horas.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertaEvento {

    /** Identificador único de la alerta. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Tipo de alerta (PROXIMA_24H o INMINENTE_15MIN). */
    @Enumerated(EnumType.STRING)
    private TipoAlerta tipo;

    /** Mensaje descriptivo de la alerta. */
    private String mensaje;

    /** Indica si el usuario ya ha leído/visto la alerta. */
    private Boolean leida;

    /** Fecha y hora en que se creó la alerta. */
    private LocalDateTime createdAt;

    /** Fecha y hora en que el usuario marcó la alerta como leída. */
    private LocalDateTime leidaAt;

    /** Evento al que hace referencia la alerta. */
    @ManyToOne
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    /** Usuario destinatario de la alerta. */
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /** Inicializa valores por defecto antes de persistir. */
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.leida == null) {
            this.leida = false;
        }
    }

    /** Marca la alerta como leída. */
    public void marcarComoLeida() {
        this.leida = true;
        this.leidaAt = LocalDateTime.now();
    }
}
