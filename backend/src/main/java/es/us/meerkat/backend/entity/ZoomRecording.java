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

/** Grabacion de Zoom almacenada como metadatos dentro de la app. */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ZoomRecording {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "zoom_meeting_id", nullable = false)
    private ZoomMeeting zoomMeeting;

    @Column(nullable = false, unique = true)
    private String zoomRecordingId;

    @Column(nullable = false)
    private String fileType;

    @Column(length = 2048)
    private String playUrl;

    @Column(length = 2048)
    private String downloadUrl;

    @Column(length = 2048)
    private String localFilePath;

    @Column(length = 255)
    private String storageProvider;

    @Column(length = 1024)
    private String storageObjectKey;

    private Long fileSizeBytes;

    @Column(nullable = false)
    @Builder.Default
    private Boolean storedInApp = false;

    private LocalDateTime recordingStart;

    private LocalDateTime recordingEnd;

    private LocalDateTime expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private String status = "AVAILABLE";

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null || this.status.isBlank()) {
            this.status = "AVAILABLE";
        }
        if (this.storedInApp == null) {
            this.storedInApp = false;
        }
    }
}
