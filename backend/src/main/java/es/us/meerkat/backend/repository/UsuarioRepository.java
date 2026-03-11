package es.us.meerkat.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import es.us.meerkat.backend.entity.Usuario;

/**
 * Repositorio JPA para la entidad {@link Usuario}.
 *
 * <p>Permite realizar operaciones CRUD sobre los usuarios en la base de datos.
 *
 * <p>Permite realizar operaciones CRUD sobre los usuarios en la base de datos.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Busca un usuario por su dirección de email.
     *
     * @param email Email del usuario.
     * @return Optional con el usuario si existe.
     */
    Optional<Usuario> findByEmail(String email);

    /**
     * Comprueba si ya existe un usuario con el email dado.
     *
     * @param email Email a verificar.
     * @return true si el email ya está registrado.
     */
    boolean existsByEmail(String email);

    /**
     * Devuelve todos los usuarios cuyo perfil es visible en listados públicos.
     *
     * @return Lista de usuarios con visibleEnListados a true.
     */
    List<Usuario> findByVisibleEnListadosTrue();

    @Query("SELECT mc.usuario FROM MiembroComunidad mc WHERE mc.comunidad.id = :comunidadId")
    List<Usuario> findMiembrosByComunidadId(@Param("comunidadId") Long comunidadId);
}
