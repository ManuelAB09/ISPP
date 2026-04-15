package es.us.meerkat.backend.entity.google;

import java.time.LocalDateTime;

import es.us.meerkat.backend.entity.events.TipoEvento;
import es.us.meerkat.backend.entity.users.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Almacena los tokens OAuth 2.0 de Google Calendar de un usuario.
 *
 * <p>Cuando el usuario autoriza la integración, Google devuelve:
 *
 * <ul>
 *   <li>{@code accessToken} — token de corta duración para llamar a la API.
 *   <li>{@code refreshToken} — token permanente para obtener nuevos accessTokens.
 * </ul>
 *
 * <p>El accessToken caduca (normalmente en 1 hora). El servicio lo renueva automáticamente con el
 * refreshToken antes de cada llamada a la API.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoogleCalendarToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Usuario propietario del token. Relación 1:1. */
    @OneToOne
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    /** Token de acceso de corta duración. */
    @Column(length = 2048)
    private String accessToken;

    /**
     * Token de refresco permanente. Solo se recibe la primera vez que el usuario autoriza. Se
     * almacena de forma segura y no se expone en ninguna respuesta.
     */
    @Column(length = 512)
    private String refreshToken;

    /** Fecha y hora de expiración del accessToken actual. */
    private LocalDateTime expiresAt;

    /** Si la sincronización con Google Calendar está activa. */
    private Boolean sincronizacionActiva;

    /**
     * Tipos de evento que el usuario quiere sincronizar. Almacenado como CSV:
     * "REUNION,EXAMEN,TUTORIA" Si es null o vacío → sincroniza todos.
     */
    @Column(length = 256)
    private String tiposEventoSincronizados;

    /** Fecha de creación del registro. */
    private LocalDateTime createdAt;

    /** Fecha de última actualización. */
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.sincronizacionActiva == null) {
            this.sincronizacionActiva = true;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /** Indica si el accessToken ha caducado o está a punto de caducar (margen de 5 min). */
    public boolean accessTokenCaducado() {
        if (expiresAt == null) {
            return true;
        }

        return LocalDateTime.now().isAfter(expiresAt.minusMinutes(5));
    }

    /**
     * Indica si el usuario quiere sincronizar un tipo de evento concreto.
     *
     * @param tipo Tipo de evento a comprobar.
     * @return true si debe sincronizarse.
     */
    public boolean sincronizaTipo(final TipoEvento tipo) {
        if (!Boolean.TRUE.equals(sincronizacionActiva)) {
            return false;
        }

        if (tiposEventoSincronizados == null || tiposEventoSincronizados.isBlank()) {
            return true; // sin filtro → sincroniza todos
        }
        return tiposEventoSincronizados.contains(tipo.name());
    }
}
