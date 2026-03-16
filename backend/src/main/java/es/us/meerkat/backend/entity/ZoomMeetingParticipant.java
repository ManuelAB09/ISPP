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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Participante de una reunion Zoom para saber quien esta en cada llamada. */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ZoomMeetingParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "zoom_meeting_id", nullable = false)
    private ZoomMeeting zoomMeeting;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false)
    private String zoomParticipantId;

    @Column(nullable = false)
    private String displayName;

    private String email;

    @Column(nullable = false)
    private LocalDateTime joinedAt;

    private LocalDateTime leftAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean inCall = true;

    @PrePersist
    public void prePersist() {
        if (this.joinedAt == null) {
            this.joinedAt = LocalDateTime.now();
        }
        if (this.inCall == null) {
            this.inCall = true;
        }
    }

    /** Marca la salida de un participante de la llamada. */
    public void markLeft() {
        this.inCall = false;
        this.leftAt = LocalDateTime.now();
    }
}
