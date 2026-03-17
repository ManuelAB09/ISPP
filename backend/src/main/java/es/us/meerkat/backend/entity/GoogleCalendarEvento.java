package es.us.meerkat.backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mapeo entre un evento de la plataforma y su evento correspondiente en Google Calendar.
 *
 * <p>Cuando se crea un evento de Google Calendar para un usuario, Google devuelve un {@code
 * googleEventId}. Este ID se guarda aquí para poder actualizarlo o eliminarlo en el futuro cuando
 * el evento cambie o se cancele.
 *
 * <p>Hay una fila por cada par (usuario, evento) sincronizado.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoogleCalendarEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID del evento en Google Calendar. Se obtiene de la respuesta de la API al crear el evento.
     */
    @Column(nullable = false, length = 256)
    private String googleEventId;

    /** Fecha de creación del mapeo. */
    private LocalDateTime createdAt;

    /** Fecha de última sincronización (actualización). */
    private LocalDateTime lastSyncAt;

    /** Evento de la plataforma. */
    @ManyToOne
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    /** Usuario propietario del evento de Google Calendar. */
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.lastSyncAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.lastSyncAt = LocalDateTime.now();
    }
}
