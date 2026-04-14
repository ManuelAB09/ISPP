package es.us.meerkat.backend.entity.chats;

import es.us.meerkat.backend.entity.users.Usuario;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "mensajes_leidos",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"mensaje_id", "usuario_id"})})
public class MensajeLeido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mensaje_id", nullable = false)
    private Mensaje mensaje;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private java.time.LocalDateTime leidoAt;
}
