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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad que representa un anuncio dentro de una comunidad.
 *
 * <p>Los anuncios son mensajes importantes publicados por administradores, separados del chat
 * principal y destacados visualmente. Se quedan fijados en la parte superior de la sección de
 * anuncios.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "anuncios")
public class Anuncio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Título del anuncio. */
    @Column(nullable = false, length = 200)
    private String titulo;

    /** Contenido del anuncio. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenido;

    /** Comunidad a la que pertenece el anuncio. */
    @ManyToOne
    @JoinColumn(name = "comunidad_id", nullable = false)
    private Comunidad comunidad;

    /** Usuario administrador que creó el anuncio. */
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /** Indica si permite comentarios de miembros. */
    @Builder.Default
    @Column(nullable = false)
    private Boolean permitirComentarios = true;

    /** Fecha y hora de creación del anuncio. */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Fecha y hora de la última actualización. */
    @Column private LocalDateTime updatedAt;

    /** Indica si el anuncio ha sido editado. */
    @Builder.Default
    @Column(nullable = false)
    private Boolean editado = false;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.permitirComentarios == null) {
            this.permitirComentarios = true;
        }
        if (this.editado == null) {
            this.editado = false;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
        this.editado = true;
    }
}
