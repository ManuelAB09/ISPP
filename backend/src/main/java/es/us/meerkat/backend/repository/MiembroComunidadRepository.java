package es.us.meerkat.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import es.us.meerkat.backend.entity.Comunidad;
import es.us.meerkat.backend.entity.MiembroComunidad;
import es.us.meerkat.backend.entity.RolComunidad;
import es.us.meerkat.backend.entity.Usuario;

@Repository
public interface MiembroComunidadRepository extends JpaRepository<MiembroComunidad, Long> {

    Optional<MiembroComunidad> findByUsuarioIdAndComunidadId(Long usuarioId, Long comunidadId);

    Page<MiembroComunidad> findByComunidadId(Long comunidadId, Pageable pageable);

    Page<MiembroComunidad> findByComunidadIdAndRol(
            Long comunidadId, RolComunidad rol, Pageable pageable);

    List<MiembroComunidad> findByComunidadIdAndRol(Long comunidadId, RolComunidad rol);

    Page<MiembroComunidad> findByUsuarioId(Long usuarioId, Pageable pageable);

    long countByComunidadId(Long comunidadId);

    List<MiembroComunidad> findByUsuarioIdAndRol(Long usuarioId, RolComunidad rol);

    void deleteByUsuarioId(Long usuarioId);

    long countByComunidadIdAndRol(Long comunidadId, RolComunidad rol);

    /**
     * Verifica si un usuario es miembro de una comunidad.
     *
     * @param usuario el usuario
     * @param comunidad la comunidad
     * @return true si es miembro, false en caso contrario
     */
    boolean existsByUsuarioAndComunidad(Usuario usuario, Comunidad comunidad);
}
