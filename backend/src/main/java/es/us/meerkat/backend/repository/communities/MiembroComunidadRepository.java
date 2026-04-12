package es.us.meerkat.backend.repository.communities;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import es.us.meerkat.backend.entity.communities.Comunidad;
import es.us.meerkat.backend.entity.communities.MiembroComunidad;
import es.us.meerkat.backend.entity.communities.RolComunidad;
import es.us.meerkat.backend.entity.users.Usuario;

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

    @Query("SELECT m FROM MiembroComunidad m JOIN FETCH m.comunidad WHERE m.usuario.id = :userId AND m.rol = :rol")
    List<MiembroComunidad> findByUsuarioIdAndRolWithComunidad(
            @Param("userId") Long userId, @Param("rol") RolComunidad rol);

    void deleteByUsuarioId(Long usuarioId);

    long countByComunidadIdAndRol(Long comunidadId, RolComunidad rol);

    @Query("SELECT COUNT(m) FROM MiembroComunidad m WHERE m.usuario.id = :userId AND m.rol = :rol AND m.comunidad.institution IS NULL")
    long countByUsuarioIdAndRolAndComunidadInstitutionIsNull(
            @Param("userId") Long userId, @Param("rol") RolComunidad rol);

    @Query(
            "SELECT m.usuario FROM MiembroComunidad m WHERE m.comunidad.id = :comunidadId AND"
                    + " m.usuario.id != :creadorId ORDER BY m.fechaIngreso ASC")
    List<Usuario> findMiembrosMasAntiguosEnComunidad(
            @Param("comunidadId") Long comunidadId, @Param("creadorId") Long creadorId);

    @Query("SELECT m.usuario.id FROM MiembroComunidad m WHERE m.comunidad.id = :comunidadId")
    List<Long> findUsuarioIdsByComunidadId(@Param("comunidadId") Long comunidadId);

    /**
     * Verifica si un usuario es miembro de una comunidad.
     *
     * @param usuario el usuario
     * @param comunidad la comunidad
     * @return true si es miembro, false en caso contrario
     */
    boolean existsByUsuarioAndComunidad(Usuario usuario, Comunidad comunidad);
}
