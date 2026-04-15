package es.us.meerkat.backend.entity.forms;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Opcion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String texto;

    private Integer orden;

    @ManyToOne(fetch = FetchType.LAZY)
    private Pregunta pregunta;

    /** Indica si esta opción es correcta (válido para tipo TEST y VERDADERO_FALSO). */
    @Builder.Default private Boolean correcta = false;
}
