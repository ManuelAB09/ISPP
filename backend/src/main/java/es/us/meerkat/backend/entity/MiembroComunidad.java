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
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
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
    private RolComunidad rol = RolComunidad.MIEMBRO;

    @Builder.Default private LocalDateTime fechaIngreso = LocalDateTime.now();
}
