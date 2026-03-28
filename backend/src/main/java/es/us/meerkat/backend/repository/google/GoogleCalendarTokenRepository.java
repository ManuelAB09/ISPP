package es.us.meerkat.backend.repository.google;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import es.us.meerkat.backend.entity.GoogleCalendarToken;

/** Repositorio JPA para {@link GoogleCalendarToken}. */
public interface GoogleCalendarTokenRepository extends JpaRepository<GoogleCalendarToken, Long> {

    /**
     * Busca el token de Google Calendar de un usuario.
     *
     * @param usuarioId ID del usuario.
     * @return Optional con el token si existe.
     */
    Optional<GoogleCalendarToken> findByUsuarioId(Long usuarioId);

    /**
     * Elimina el token de Google Calendar de un usuario (desconexión).
     *
     * @param usuarioId ID del usuario.
     */
    void deleteByUsuarioId(Long usuarioId);
}
