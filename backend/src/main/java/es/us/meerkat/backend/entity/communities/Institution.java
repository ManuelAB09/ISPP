package es.us.meerkat.backend.entity.communities;

import java.time.LocalDateTime;

import es.us.meerkat.backend.entity.subscriptions.TipoPlanCorporativo;
import es.us.meerkat.backend.entity.users.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad que representa una institución educativa/academia.
 *
 * <p>Contiene información sobre instituciones que pueden contratar planes corporativos para ofrecer
 * a usuarios y comunidades.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "institucion")
public class Institution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre de la institución. */
    @Column(nullable = false, length = 200)
    private String nombre;

    /** Descripción de la institución. */
    @Column(length = 1000)
    private String descripcion;

    /** Email de contacto principal. */
    @Column(nullable = false)
    private String emailContacto;

    /** Teléfono de contacto. */
    @Column(length = 20)
    private String telefonoContacto;

    /** Dominio de email para validación de usuarios. */
    @Column(nullable = false, unique = true)
    private String dominioEmail;

    /** Ubicación/ciudad. */
    @Column(length = 100)
    private String ubicacion;

    /** Sitio web. */
    @Column(length = 255)
    private String sitioweb;

    /** URL del logo. */
    @Column(length = 500)
    private String logoUrl;

    /** Indica si la institución está verificada. */
    @Builder.Default
    @Column(nullable = false)
    private Boolean verificada = false;

    /** Tipo de plan corporativo contratado. */
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private TipoPlanCorporativo planCorporativo;

    /** Indica si el plan está activo. */
    @Builder.Default
    @Column(nullable = false)
    private Boolean planActivo = false;

    /** Fecha de inicio del plan corporativo. */
    private LocalDateTime fechaInicioPlan;

    /** Fecha de fin del plan corporativo. */
    private LocalDateTime fechaFinPlan;

    /** Número de usuarios permitidos. */
    private Integer numUsuariosPermitidos;

    /** Usuario administrador de la institución. */
    @ManyToOne
    @JoinColumn(name = "usuario_admin_id", nullable = false)
    private Usuario usuarioAdmin;

    /** Fecha de creación. */
    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Fecha de actualización. */
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
