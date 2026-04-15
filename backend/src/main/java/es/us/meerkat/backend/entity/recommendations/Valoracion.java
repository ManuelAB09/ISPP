package es.us.meerkat.backend.entity.recommendations;

import java.time.LocalDateTime;

import es.us.meerkat.backend.entity.events.Evento;
import es.us.meerkat.backend.entity.tutors.Tutor;
import es.us.meerkat.backend.entity.users.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Valoracion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "profesor_id")
    private Tutor profesor;

    @ManyToOne(optional = false)
    @JoinColumn(name = "alumno_id")
    private Usuario alumno;

    @ManyToOne(optional = false)
    @JoinColumn(name = "evento_id")
    private Evento evento;

    private int puntuacion; // 1 a 5

    @Column(length = 1000)
    private String comentario;

    private LocalDateTime fecha;
}
