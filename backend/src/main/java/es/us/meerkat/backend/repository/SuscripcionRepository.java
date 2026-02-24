package es.us.meerkat.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import es.us.meerkat.backend.entity.Suscripcion;

/** Repositorio para gestionar la persistencia de entidades Suscripcion. */
@Repository
public interface SuscripcionRepository extends JpaRepository<Suscripcion, Long> {

    /**
     * Busca la suscripción activa de un usuario.
     *
     * @param usuarioId ID del usuario
     * @param activa Indica si la suscripción está activa
     * @return Suscripción si existe
     */
    Optional<Suscripcion> findByUsuarioIdAndActiva(Long usuarioId, Boolean activa);

    /**
     * Busca cualquier suscripción de un usuario, independientemente de su estado.
     *
     * @param usuarioId ID del usuario
     * @return Suscripción si existe
     */
    Optional<Suscripcion> findByUsuarioId(Long usuarioId);
}
