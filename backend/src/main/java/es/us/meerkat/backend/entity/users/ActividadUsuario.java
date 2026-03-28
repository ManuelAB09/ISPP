package es.us.meerkat.backend.entity.users;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad que registra las actividades del usuario para análisis y recomendaciones.
 *
 * <p>Rastrear búsquedas, visualizaciones, clics, etc. para mejorar recomendaciones.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "actividad_usuario",
        indexes = {
            @Index(name = "idx_actividad_usuario", columnList = "usuario_id"),
            @Index(name = "idx_actividad_tipo", columnList = "tipo_actividad"),
            @Index(name = "idx_actividad_fecha", columnList = "createdAt")
        })
public class ActividadUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Usuario que realiza la actividad. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /** Tipo de actividad (BUSQUEDA, VISUALIZACION, CLIC, LIKE, UNIRSE, CREAR, etc). */
    @Column(nullable = false)
    private String tipoActividad;

    /** Categoría del objeto observado (Tutor, Comunidad, Contenido, etc). */
    @Column(nullable = false)
    private String categoriaObjeto;

    /** ID del objeto con el que interactuó. */
    private Long idObjeto;

    /** Términos de búsqueda si aplica. */
    @Column(columnDefinition = "TEXT")
    private String terminosBusqueda;

    /** Duración de visualización en segundos si aplica. */
    private Long duracionSegundos;

    /** IP o dispositivo desde donde se realizó la actividad. */
    private String dispositivo;

    /** Ubicación geográfica si está disponible. */
    private String ubicacion;

    /** Información adicional en JSON. */
    @Column(columnDefinition = "TEXT")
    private String datosAdicionales;

    /** Fecha de la actividad. */
    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        // No se actualiza
    }
}
