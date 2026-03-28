package es.us.meerkat.backend.repository.communities;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import es.us.meerkat.backend.entity.Comunidad;
import es.us.meerkat.backend.entity.EstadoInvitacion;
import es.us.meerkat.backend.entity.InvitacionMiembro;

/** Repositorio para la gestión de invitaciones a miembros en la base de datos. */
@Repository
public interface InvitacionMiembroRepository extends JpaRepository<InvitacionMiembro, Long> {

    /**
     * Obtiene una invitación por su código único.
     *
     * @param codigo el código de la invitación
     * @return la invitación si existe
     */
    Optional<InvitacionMiembro> findByCodigo(String codigo);

    /**
     * Obtiene las invitaciones de una comunidad de forma paginada.
     *
     * @param comunidad la comunidad
     * @param pageable la paginación
     * @return página de invitaciones
     */
    Page<InvitacionMiembro> findByComunidad(Comunidad comunidad, Pageable pageable);

    /**
     * Obtiene todas las invitaciones de una comunidad.
     *
     * @param comunidad la comunidad
     * @return lista de invitaciones
     */
    List<InvitacionMiembro> findByComunidad(Comunidad comunidad);

    /**
     * Obtiene las invitaciones pendientes de una comunidad.
     *
     * @param comunidad la comunidad
     * @return lista de invitaciones pendientes
     */
    List<InvitacionMiembro> findByComunidadAndEstado(Comunidad comunidad, EstadoInvitacion estado);

    /**
     * Verifica si existe una invitación pendiente para un email en una comunidad.
     *
     * @param email el email del invitado
     * @param comunidad la comunidad
     * @return true si existe, false en caso contrario
     */
    boolean existsByEmailAndComunidadAndEstado(
            String email, Comunidad comunidad, EstadoInvitacion estado);

    /**
     * Obtiene una invitación por email y comunidad.
     *
     * @param email el email
     * @param comunidad la comunidad
     * @return la invitación si existe
     */
    Optional<InvitacionMiembro> findByEmailAndComunidad(String email, Comunidad comunidad);

    /**
     * Cuenta invitaciones pendientes de una comunidad.
     *
     * @param comunidad la comunidad
     * @return número de invitaciones pendientes
     */
    long countByComunidadAndEstado(Comunidad comunidad, EstadoInvitacion estado);
}
