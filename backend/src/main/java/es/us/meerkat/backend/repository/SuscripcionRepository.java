package es.us.meerkat.backend.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import es.us.meerkat.backend.entity.Suscripcion;

/** Repositorio para gestionar la persistencia de entidades Suscripcion. */
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

    /**
     * Elimina todas las suscripciones de un usuario.
     *
     * @param usuarioId ID del usuario
     */
    void deleteByUsuarioId(Long usuarioId);

    /**
     * Busca suscripciones activas cuya fecha de fin ya pasó (expiradas).
     *
     * @param fecha Fecha límite (normalmente LocalDate.now())
     * @return Lista de suscripciones expiradas
     */
    List<Suscripcion> findByActivaTrueAndFechaFinBefore(LocalDate fecha);
}
