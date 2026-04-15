package es.us.meerkat.backend.entity.emails;

import java.time.LocalDateTime;

import es.us.meerkat.backend.entity.events.Evento;
import es.us.meerkat.backend.entity.notifications.TipoRecordatorio;
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
 * Entidad que registra cada recordatorio por email enviado.
 *
 * <p>Actúa como log de envíos y como mecanismo anti-duplicado: antes de enviar un recordatorio se
 * comprueba que no exista ya un registro para ese usuario + evento + tipo.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecordatorioEmail {

    /** Identificador único. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Tipo de recordatorio enviado. */
    @Enumerated(EnumType.STRING)
    private TipoRecordatorio tipo;

    /** Fecha y hora en que se envió el email. */
    private LocalDateTime enviadoAt;

    /** Si el envío fue exitoso o hubo error. */
    private Boolean exitoso;

    /** Mensaje de error si el envío falló (null si fue exitoso). */
    private String errorMensaje;

    /** Evento al que corresponde el recordatorio. */
    @ManyToOne
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    /** Usuario destinatario del recordatorio. */
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @PrePersist
    public void prePersist() {
        this.enviadoAt = LocalDateTime.now();
        if (this.exitoso == null) {
            this.exitoso = false;
        }
    }
}
