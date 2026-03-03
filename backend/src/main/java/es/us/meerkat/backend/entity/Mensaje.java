package es.us.meerkat.backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String contenido;

    @Column(length = 2048)
    private String archivoUrl;

    @Column(length = 255)
    private String archivoNombre;

    @Column(length = 100)
    private String archivoMimeType;

    @Column private Long archivoTamano;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "archivo_data")
    private byte[] archivoData;

    @Column(nullable = false)
    private Boolean editado = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // Usuario que envía el mensaje
    @ManyToOne
    @JoinColumn(name = "emisor_id", nullable = false)
    private Usuario emisor;

    // Usuario que recibe el mensaje
    @ManyToOne
    @JoinColumn(name = "receptor_id", nullable = false)
    private Usuario receptor;

    // Tutor relacionado (opcional pero recomendado)
    @ManyToOne
    @JoinColumn(name = "tutor_id", nullable = true)
    private Tutor tutor;

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

    public void editar(String nuevoContenido) {
        this.contenido = nuevoContenido;
        this.editado = true;
    }
}
