package es.us.meerkat.backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
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
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Entidad para almacenar reuniones Zoom vinculadas a una comunidad. */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ZoomMeeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "comunidad_id", nullable = false)
    private Comunidad comunidad;

    @ManyToOne(optional = false)
    @JoinColumn(name = "creador_id", nullable = false)
    private Usuario creador;

    @Column(nullable = false, unique = true)
    private String zoomMeetingId;

    @Column(nullable = false)
    private String topic;

    @Column(nullable = false, length = 2048)
    private String joinUrl;

    @Column(nullable = false, length = 2048)
    private String startUrl;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ZoomMeetingStatus status = ZoomMeetingStatus.ACTIVE;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime endedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = ZoomMeetingStatus.ACTIVE;
        }
    }

    /** Marca una reunion como finalizada. */
    public void endMeeting() {
        this.status = ZoomMeetingStatus.ENDED;
        this.endedAt = LocalDateTime.now();
    }
}
