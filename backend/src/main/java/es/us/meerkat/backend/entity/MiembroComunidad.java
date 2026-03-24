package es.us.meerkat.backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Setter
@ToString(of = "id")
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "miembros_comunidad",
        uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "comunidad_id"}))
public class MiembroComunidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne private Usuario usuario;

    @ManyToOne private Comunidad comunidad;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private RolComunidad rol = RolComunidad.ALUMNO;

    /**
     * Rol docente secundario: solo relevante cuando rol=ADMIN. Indica si el creador actúa como
     * PROFESOR (eventos valorables) o ALUMNO.
     */
    @Enumerated(EnumType.STRING)
    private RolComunidad rolDocente;

    @Builder.Default private LocalDateTime fechaIngreso = LocalDateTime.now();
}
