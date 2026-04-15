package es.us.meerkat.backend.entity.communities;

import java.time.LocalDateTime;

import es.us.meerkat.backend.entity.users.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad que representa una invitación a un usuario para unirse a una comunidad.
 *
 * <p>Los administradores pueden invitar nuevos miembros (alumnos y profesores) mediante su
 * dirección de correo electrónico. Los destinatarios reciben un email con un enlace único para
 * unirse a la comunidad.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "invitaciones_miembro",
        uniqueConstraints = @UniqueConstraint(columnNames = {"email", "comunidad_id"}))
public class InvitacionMiembro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Email del usuario invitado. */
    @Column(nullable = false)
    private String email;

    /** Código único de invitación para el link. */
    @Column(nullable = false, unique = true, length = 255)
    private String codigo;

    /** Comunidad a la que se invita. */
    @ManyToOne
    @JoinColumn(name = "comunidad_id", nullable = false)
    private Comunidad comunidad;

    /** Usuario administrador que envió la invitación. */
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuarioInvitador;

    /** Rol que tendrá el usuario al aceptar la invitación. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RolComunidad rol;

    /** Estado de la invitación. */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private EstadoInvitacion estado = EstadoInvitacion.PENDIENTE;

    /** Usuario que aceptó la invitación (si aplica). */
    @ManyToOne
    @JoinColumn(name = "usuario_aceptador_id")
    private Usuario usuarioAceptador;

    /** Fecha de creación de la invitación. */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Fecha de expiración de la invitación. */
    @Column(nullable = false)
    private LocalDateTime fechaExpiracion;

    /** Fecha en que fue aceptada la invitación. */
    @Column private LocalDateTime fechaAceptacion;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        // Las invitaciones expiran en 30 días
        if (this.fechaExpiracion == null) {
            this.fechaExpiracion = this.createdAt.plusDays(30);
        }
        if (this.estado == null) {
            this.estado = EstadoInvitacion.PENDIENTE;
        }
    }

    /**
     * Verifica si la invitación ha expirado.
     *
     * @return true si la invitación ha expirado, false en caso contrario
     */
    public boolean estaExpirada() {
        return LocalDateTime.now().isAfter(this.fechaExpiracion);
    }

    /**
     * Marca la invitación como aceptada.
     *
     * @param usuarioAceptador el usuario que acepta la invitación
     */
    public void aceptar(Usuario usuarioAceptador) {
        if (estaExpirada()) {
            this.estado = EstadoInvitacion.EXPIRADA;
        } else {
            this.usuarioAceptador = usuarioAceptador;
            this.estado = EstadoInvitacion.ACEPTADA;
            this.fechaAceptacion = LocalDateTime.now();
        }
    }
}
