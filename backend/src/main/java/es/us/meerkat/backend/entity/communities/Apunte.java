package es.us.meerkat.backend.entity.communities;

import java.time.LocalDateTime;

import es.us.meerkat.backend.entity.users.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/** Entidad que representa un apunte o documento compartido en una comunidad. */
@Entity
@Getter
@Setter
@ToString(of = "id")
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "apuntes")
public class Apunte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El título es requerido")
    @Column(length = 200)
    private String titulo;

    @Column(length = 1000)
    private String descripcion;

    @NotNull(message = "El archivo es requerido")
    @Lob
    private byte[] contenido;

    @NotBlank(message = "El nombre del archivo es requerido")
    @Column(length = 255)
    private String nombreArchivo;

    @Column(length = 50)
    private String tipoMime;

    @NotNull(message = "El tamaño del archivo es requerido")
    private Long tamanioArchivo;

    @NotNull(message = "La comunidad es requerida")
    @ManyToOne
    @JoinColumn(name = "comunidad_id", nullable = false)
    private Comunidad comunidad;

    @NotNull(message = "El usuario es requerido")
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @NotNull(message = "La fecha de creación es requerida")
    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    private Integer descargas = 0;

    private Double valoracionMedia = 0.0;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (descargas == null) {
            descargas = 0;
        }
        if (valoracionMedia == null) {
            valoracionMedia = 0.0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
