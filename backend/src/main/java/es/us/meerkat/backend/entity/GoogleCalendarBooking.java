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
 * Mapeo entre una solicitud de contratación directa y su evento en Google Calendar.
 *
 * <p>Cuando un alumno paga una clase, se crea un evento en Google Calendar tanto para el alumno
 * como para el tutor (si tienen GCal conectado). Este mapeo guarda el googleEventId para poder
 * actualizar o eliminar el evento en el futuro.
 *
 * <p>Hay una fila por cada par (usuario, solicitud) sincronizado.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoogleCalendarBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ID del evento en Google Calendar. */
    @Column(nullable = false, length = 256)
    private String googleEventId;

    /** Solicitud de contratación directa asociada. */
    @ManyToOne
    @JoinColumn(name = "solicitud_id", nullable = false)
    private SolicitudContratacionDirecta solicitud;

    /** Usuario propietario del evento de Google Calendar. */
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /** Fecha de creación del mapeo. */
    private LocalDateTime createdAt;

    /** Fecha de última sincronización. */
    private LocalDateTime lastSyncAt;

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
