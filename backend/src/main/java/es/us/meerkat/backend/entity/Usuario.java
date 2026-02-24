package es.us.meerkat.backend.entity;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad que representa un usuario en la plataforma.
 *
 * <p>Contiene información de login, rol de tutor y relación con tutores.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    /** Identificador único del usuario. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Correo electrónico del usuario. */
    private String email;

    /** Contraseña del usuario (almacenada cifrada). */
    private String password;

    /** Nombre completo del usuario. */
    private String nombre;

    /** Indica si el usuario tiene rol de tutor. */
    private Boolean esTutor;

    /** Lista de tutores asociados al usuario (si es tutor). */
    @OneToMany(mappedBy = "us", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Tutor> tutores;
}
