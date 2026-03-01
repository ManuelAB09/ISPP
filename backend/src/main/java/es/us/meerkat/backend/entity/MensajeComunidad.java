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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad que representa un mensaje enviado en el chat de una comunidad.
 *
 * <p>Almacena mensajes públicos de miembros de una comunidad.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "mensajes_comunidad")
public class MensajeComunidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String contenido;

    @Column(nullable = false)
    private Boolean editado = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column private LocalDateTime editedAt;

    // Usuario que envía el mensaje
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // Comunidad en la que se envía el mensaje
    @ManyToOne
    @JoinColumn(name = "comunidad_id", nullable = false)
    private Comunidad comunidad;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.editado == null) {
            this.editado = false;
        }
    }

    // =====================
    // MÉTODOS DE DOMINIO
    // =====================

    /**
     * Edita el contenido del mensaje y marca como editado.
     *
     * @param nuevoContenido el nuevo contenido del mensaje.
     */
    public void editar(final String nuevoContenido) {
        this.contenido = nuevoContenido;
        this.editado = true;
        this.editedAt = LocalDateTime.now();
    }
}
