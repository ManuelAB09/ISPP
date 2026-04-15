package es.us.meerkat.backend.entity.maps;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import es.us.meerkat.backend.entity.events.Evento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** Entidad que representa una ubicación geográfica */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ubicacion {

    /** Identificador único de la ubicación. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre del lugar (ej. Biblioteca Central). */
    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String direccion;

    /** Latitud geográfica. */
    @Column(nullable = false)
    private Double latitud;

    /** Longitud geográfica. */
    @Column(nullable = false)
    private Double longitud;

    /** Tipo de ubicación (ej. biblioteca, universidad, etc.). */
    @Column(nullable = false)
    private String tipo;

    /** Coste asociado al lugar (e.g., "gratis", "de pago", "desconocido"). */
    @Column(nullable = false)
    private String coste;

    /** Eventos asociados a esta ubicación. */
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "ubicacion")
    @Builder.Default
    private List<Evento> eventos = new ArrayList<>();
}
