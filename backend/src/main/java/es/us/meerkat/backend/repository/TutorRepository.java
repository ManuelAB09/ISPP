package es.us.meerkat.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import es.us.meerkat.backend.entity.Tutor;
import es.us.meerkat.backend.entity.Usuario;

/**
 * Repositorio JPA para la entidad {@link Tutor}.
 *
 * Permite realizar operaciones CRUD y consultas específicas
 * sobre tutores en la base de datos.
 */
public interface TutorRepository extends JpaRepository<Tutor, Long> {

    /**
     * Busca un tutor asociado a un usuario específico.
     *
     * @param us Usuario asociado al tutor.
     * @return Optional que contiene el tutor si existe.
     */
    Optional<Tutor> findByUs(Usuario us);

    /**
     * Devuelve todos los tutores que estén verificados.
     *
     * @return Lista de tutores verificados.
     */
    List<Tutor> findByVerificadoTrue();

}
