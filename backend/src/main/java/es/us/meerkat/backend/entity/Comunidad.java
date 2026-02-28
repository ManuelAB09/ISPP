package es.us.meerkat.backend.entity;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "comunidades")
public class Comunidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es requerido")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    private String nombre;

    @Size(max = 1000, message = "La descripción no puede exceder 1000 caracteres")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    private TipoGrupo tipoGrupo;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EstadoComunidad estado = EstadoComunidad.ACTIVA;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TipoPlanComunidad tipoPlan = TipoPlanComunidad.FREE;

    private String imagenUrl;

    @ManyToOne
    @JoinColumn(name = "institution_id")
    private Institution institution;

    @Builder.Default private Integer maxMiembros = 50;

    @ManyToOne private Usuario creador;

    @Builder.Default private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "comunidad", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MiembroComunidad> miembros;

    @OneToMany(mappedBy = "comunidad", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SolicitudComunidad> solicitudes;

    @OneToMany(mappedBy = "comunidad", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Categoria> categorias;
}
