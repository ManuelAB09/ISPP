package es.us.meerkat.backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "notificaciones")
public class Notificacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    @Column(nullable = false)
    private String tipo; // Ej: ANUNCIO, EVENTO, MENSAJE, etc.

    @Column(nullable = false)
    private Boolean leida = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // --- NUEVOS CAMPOS PARA ANUNCIOS Y EVENTOS ---
    /** Id del anuncio relacionado (si aplica). */
    private Long anuncioId;

    /** Id del evento relacionado (si aplica). */
    private Long eventoId;

    /** Id de la comunidad relacionada (si aplica). */
    private Long comunidadId;

    /** Nombre de la comunidad relacionada (si aplica). */
    private String comunidadNombre;

    /** URL de la imagen de la comunidad relacionada (si aplica). */
    private String comunidadImagenUrl;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.leida == null) {
            this.leida = false;
        }
    }
}
