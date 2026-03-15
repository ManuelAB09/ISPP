package es.us.meerkat.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import es.us.meerkat.backend.entity.GoogleClassroomConnection;
import es.us.meerkat.backend.entity.Usuario;

/**
 * Repositorio JPA para la entidad {@link GoogleClassroomConnection}.
 *
 * <p>Permite realizar operaciones CRUD sobre las conexiones de Google Classroom y buscar la
 * conexión asociada a un usuario específico.
 */
public interface GoogleClassroomConnectionRepository
        extends JpaRepository<GoogleClassroomConnection, Long> {

    Optional<GoogleClassroomConnection> findByUsuario(Usuario usuario);
}
