package es.us.meerkat.backend.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Entidad que representa un usuario en la plataforma.
 *
 * <p>Contiene información de login, rol de tutor y relación con tutores.
 */
@Entity
@Data
@ToString(exclude = {"tutores", "intereses"})
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    /** Identificador único del usuario. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Correo electrónico del usuario (único en la plataforma). */
    @Column(unique = true, nullable = false)
    private String email;

    /** Contraseña del usuario (almacenada cifrada con BCrypt). */
    @Column(nullable = false)
    private String password;

    /** Nombre completo del usuario. */
    private String nombre;

    /** URL/ruta de la foto de perfil del usuario. Puede ser nula si no tiene foto. */
    @Column(columnDefinition = "TEXT")
    private String foto;

    /** Color de fondo para la foto de perfil (ej: #ffffff). Por defecto blanco. */
    @Column(length = 7)
    private String fotoBackgroundColor = "#ffffff";

    /** Universidad del usuario. */
    private String universidad;

    /** Grado del usuario. */
    private String grado;

    /** Nivel de estudios del usuario. */
    private String nivelEstudios;

    /** Base formativa del usuario. */
    private String baseFormativa;

    /** Ubicación del usuario. */
    private String ubicacion;

    /** Breve biografía del usuario. */
    private String bio;

    /** Lista de intereses del usuario. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "usuario_intereses", joinColumns = @JoinColumn(name = "usuario_id"))
    @Column(name = "interes")
    private List<String> intereses = new ArrayList<>();

    /** Identificador de Google para autenticación OAuth2. */
    private String googleId;

    /**
     * Indica si el perfil del usuario es visible en listados públicos y resultados de búsqueda. Por
     * defecto es visible.
     */
    @Column(nullable = false)
    private Boolean visibleEnListados = true;

    /** Indica si el usuario tiene rol de tutor. */
    @Column(nullable = false)
    private Boolean esTutor = false;

    /** Indica si la autenticación de dos factores está habilitada para el usuario. */
    @Column(nullable = false)
    private Boolean autenticacionDosFactores = false;

    /** Indica si el usuario quiere recibir notificaciones por email. */
    @Column(nullable = false)
    private Boolean notificacionesEmail = true;

    /** Indica si el usuario quiere recibir notificaciones push. */
    @Column(nullable = false)
    private Boolean notificacionesPush = false;

    // AÑADIR tipo plan cuando se cree la clase
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoPlan plan = TipoPlan.FREE;

    /** Fecha y hora de creación de la cuenta. */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Lista de tutores asociados al usuario (si es tutor). */
    @OneToMany(mappedBy = "us", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Tutor> tutores = new ArrayList<>();

    /** Inicializa campos antes de persistir la entidad. */
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.visibleEnListados == null) {
            this.visibleEnListados = true;
        }
        if (this.esTutor == null) {
            this.esTutor = false;
        }
        if (this.autenticacionDosFactores == null) {
            this.autenticacionDosFactores = false;
        }
        if (this.notificacionesEmail == null) {
            this.notificacionesEmail = true;
        }
        if (this.notificacionesPush == null) {
            this.notificacionesPush = true;
        }
    }
}
