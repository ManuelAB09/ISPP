package es.us.meerkat.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
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
}
