package es.us.meerkat.backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidad que representa una solicitud de vinculación entre una comunidad, un curso de Google
 * Classroom y un tutor.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassroomLinkRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "comunidad_id", nullable = false)
    private Long comunidadId;

    @Column(name = "tutor_id", nullable = false)
    private Long tutorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClassroomLinkRequestStatus estado = ClassroomLinkRequestStatus.PENDIENTE;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    private LocalDateTime fechaActualizacion;

    @PreUpdate
    void onUpdate() {
        this.fechaActualizacion = LocalDateTime.now();
    }
}
