package es.us.meerkat.backend.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.*;

/** Pregunta dentro de un `Cuestionario`. */
@Entity
@Getter
@Setter
@ToString(of = "id")
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pregunta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String enunciado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoPregunta tipo;

    /** Opciones para preguntas tipo TEST. */
    @OneToMany(
            mappedBy = "pregunta",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER)
    @Builder.Default
    private List<Opcion> opciones = new ArrayList<>();

    /**
     * Respuestas aceptables para preguntas de tipo RESPUESTA_CORTA. Se usa para comparar la
     * respuesta del alumno (normalizar/trim/ignore-case según la lógica del cliente/servicio).
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "pregunta_respuestas_aceptables",
            joinColumns = @JoinColumn(name = "pregunta_id"))
    @Column(name = "respuesta")
    @Builder.Default
    private List<String> respuestasAceptables = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuestionario_id")
    private Cuestionario cuestionario;
}
