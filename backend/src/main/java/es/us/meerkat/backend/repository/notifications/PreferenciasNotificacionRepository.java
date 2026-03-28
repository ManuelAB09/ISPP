package es.us.meerkat.backend.repository.notifications;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import es.us.meerkat.backend.entity.PreferenciasNotificacion;

/** Repositorio JPA para {@link PreferenciasNotificacion}. */
public interface PreferenciasNotificacionRepository
        extends JpaRepository<PreferenciasNotificacion, Long> {

    /**
     * Busca las preferencias de notificación de un usuario.
     *
     * @param usuarioId Identificador del usuario.
     * @return Optional con las preferencias si existen.
     */
    Optional<PreferenciasNotificacion> findByUsuarioId(Long usuarioId);
}
