package es.us.meerkat.backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

/** Registra intentos/completados de un usuario en un cuestionario con su puntuación. */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CuestionarioIntento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuestionario_id", nullable = false)
    private Cuestionario cuestionario;

    /** Puntuación numérica obtenida (por ejemplo 0-100). */
    private Double puntuacion;

    /** Fecha y hora del intento/completado */
    @Column(nullable = false)
    @lombok.Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
