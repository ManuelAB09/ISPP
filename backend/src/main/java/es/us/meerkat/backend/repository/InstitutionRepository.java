package es.us.meerkat.backend.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import es.us.meerkat.backend.entity.Institution;

/** Repositorio para manejar operaciones de base de datos sobre instituciones. */
@Repository
public interface InstitutionRepository extends JpaRepository<Institution, Long> {

    /**
     * Busca una institución por su dominio de email.
     *
     * @param dominioEmail el dominio de email
     * @return Optional con la institución si la encuentra
     */
    Optional<Institution> findByDominioEmail(String dominioEmail);

    /**
     * Busca instituciones verificadas.
     *
     * @param verificada el estado de verificación
     * @return lista de instituciones
     */
    Optional<Institution> findByVerificada(Boolean verificada);

    /** Elimina instituciones cuyo administrador sea el usuario indicado. */
    void deleteByUsuarioAdminId(Long usuarioId);

    // Comunidades que pertenecen directamente a la institución
    @Query("SELECT COUNT(c) FROM Comunidad c WHERE c.institution.id = :institutionId")
    long countComunidadesByInstitutionId(@Param("institutionId") Long institutionId);

    // Usuarios cuyo email pertenece al dominio de la institución
    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.email LIKE CONCAT('%@', :dominioEmail)")
    long countUsuariosByDominioEmail(@Param("dominioEmail") String dominioEmail);

    /**
     * Busca instituciones con plan activo cuya fecha de fin ya pasó (expiradas).
     *
     * @param fecha Fecha límite (normalmente LocalDateTime.now())
     * @return Lista de instituciones con planes expirados
     */
    List<Institution> findByPlanActivoTrueAndFechaFinPlanBefore(LocalDateTime fecha);
}
