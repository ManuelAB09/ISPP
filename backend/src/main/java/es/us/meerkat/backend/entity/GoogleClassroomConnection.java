package es.us.meerkat.backend.entity;

import java.time.Instant;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Representa la conexión OAuth de un usuario con Google Classroom. */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoogleClassroomConnection {
    /** Identificador único */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Usuario propietario de la conexión */
    @OneToOne
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    /** Access token actual */
    @Column(nullable = false, length = 2048)
    private String accessToken;

    /** Refresh token (necesario para renovar acceso) */
    @Column(length = 2048)
    private String refreshToken;

    /** Fecha de expiración del access token */
    @Column(nullable = false)
    private Instant expiresAt;

    /** Indica si la conexión está activa */
    @Column(nullable = false)
    private Boolean activa = true;

    /** Verifica si el token está expirado */
    public boolean estaExpirado() {
        return Instant.now().isAfter(this.expiresAt);
    }

    /** Desactiva la conexión */
    public void desactivar() {
        this.activa = false;
    }
}
