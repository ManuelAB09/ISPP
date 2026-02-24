package es.us.meerkat.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import es.us.meerkat.backend.entity.Usuario;

/**
 * Repositorio JPA para la entidad {@link Usuario}.
 *
 * <p>Permite realizar operaciones CRUD sobre los usuarios en la base de datos.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {}
