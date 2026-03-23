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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
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
@ToString(exclude = {"intereses", "tutor", "institution"})
@NoArgsConstructor
@AllArgsConstructor
@lombok.Builder
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

    /** Ubicación del usuario (opcional). */
    @ManyToOne
    @JoinColumn(name = "ubicacion_id", nullable = true)
    private Ubicacion ubicacion;

    /** Nivel de estudios del usuario. */
    private String nivelEstudios;

    /** Base formativa del usuario. */
    private String baseFormativa;

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

    /** Se almacena la clave TOTP activa (Base32) cuando 2FA está habilitado. */
    @Column(length = 128)
    private String totpSecret;

    /** Clave TOTP temporal en el proceso de activación (no habilitada hasta verificar). */
    @Column(length = 128)
    private String totpTempSecret;

    /**
     * Códigos de respaldo (hasheados) para recuperar acceso cuando el usuario no puede usar su app.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "usuario_backup_codes", joinColumns = @JoinColumn(name = "usuario_id"))
    @Column(name = "codigo_hash", length = 80)
    private List<String> backupCodeHashes = new ArrayList<>();

    /** Indica si el usuario quiere recibir notificaciones por email. */
    @Column(nullable = false)
    private Boolean notificacionesEmail = true;

    /** Indica si el usuario quiere recibir notificaciones push. */
    @Column(nullable = true)
    private Boolean notificacionesPush = false;

    // AÑADIR tipo plan cuando se cree la clase
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoPlan plan = TipoPlan.FREE;

    /** Indica si el email del usuario ha sido verificado. */
    @Column(nullable = false)
    private Boolean emailVerificado = false;

    /** Token de verificación de email (UUID). */
    @Column(length = 36)
    private String verificationToken;

    /** Fecha de expiración del token de verificación. */
    private LocalDateTime tokenExpiration;

    /** Fecha y hora de creación de la cuenta. */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Perfil de tutor del usuario (null si no es tutor). */
    @OneToOne(
            mappedBy = "usuario",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private Tutor tutor;

    /** Institución a la que pertenece el usuario (puede ser null). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id", nullable = true)
    private Institution institution;

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
        if (this.backupCodeHashes == null) {
            this.backupCodeHashes = new ArrayList<>();
        }
        if (this.notificacionesEmail == null) {
            this.notificacionesEmail = true;
        }
        if (this.notificacionesPush == null) {
            this.notificacionesPush = true;
        }
        if (this.emailVerificado == null) {
            this.emailVerificado = false;
        }
    }
}
