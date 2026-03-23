package es.us.meerkat.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "mensajes_comunidad_leidos",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"mensaje_comunidad_id", "usuario_id"})
        })
public class MensajeComunidadLeido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mensaje_comunidad_id", nullable = false)
    private MensajeComunidad mensajeComunidad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private java.time.LocalDateTime leidoAt;
}
