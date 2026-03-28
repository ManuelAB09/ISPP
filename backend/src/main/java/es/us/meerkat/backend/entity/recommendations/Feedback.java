package es.us.meerkat.backend.entity.recommendations;

import java.time.LocalDateTime;

import es.us.meerkat.backend.entity.communities.Comunidad;
import es.us.meerkat.backend.entity.users.Usuario;
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

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "profesor_id")
    private Usuario profesor;

    @ManyToOne
    @JoinColumn(name = "alumno_id")
    private Usuario alumno;

    @ManyToOne
    @JoinColumn(name = "comunidad_id")
    private Comunidad comunidad;

    @Column(columnDefinition = "TEXT")
    private String contenido;

    private Integer calificacion;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
