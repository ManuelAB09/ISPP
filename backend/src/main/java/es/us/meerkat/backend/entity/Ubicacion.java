package es.us.meerkat.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad que representa una ubicación geográfica
 */
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

    /** Latitud geográfica. */
    @Column(nullable = false)
    private Double latitud;

    /** Longitud geográfica. */
    @Column(nullable = false)
    private Double longitud;

}