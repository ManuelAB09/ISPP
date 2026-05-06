package es.us.meerkat.backend.repository.communities;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import es.us.meerkat.backend.entity.communities.Comunidad;
import es.us.meerkat.backend.entity.communities.EstadoComunidad;
import es.us.meerkat.backend.entity.communities.TipoGrupo;
import es.us.meerkat.backend.entity.subscriptions.TipoPlanComunidad;

@Repository
public interface ComunidadRepository extends JpaRepository<Comunidad, Long> {

    Page<Comunidad> findByTipoGrupoAndEstado(
            TipoGrupo tipoGrupo, EstadoComunidad estado, Pageable pageable);

    long countByCreadorIdAndTipoPlan(Long userId, TipoPlanComunidad tipoPlan);

    long countByCreadorIdAndInstitutionIsNull(Long userId);

    @Query(
            """
SELECT COUNT(DISTINCT c.id)
FROM Comunidad c
LEFT JOIN MiembroComunidad mc ON mc.comunidad.id = c.id
WHERE c.institution IS NULL
        AND (
                                c.creador.id = :userId
                                OR (
                                                mc.usuario.id = :userId
                                                AND mc.rol = es.us.meerkat.backend.entity.communities.RolComunidad.ADMIN
                                )
                        )
""")
    long countManagedNonInstitutionCommunities(@Param("userId") Long userId);

    long countByInstitutionId(Long institutionId);

    boolean existsByNombreIgnoreCase(String nombre);

    List<Comunidad> findByCreadorId(Long userId);

    Page<Comunidad> findByTipoGrupoAndNombreContainingIgnoreCaseAndEstado(
            TipoGrupo tipoGrupo, String nombre, EstadoComunidad estado, Pageable pageable);

    Page<Comunidad> findByEstado(EstadoComunidad estado, Pageable pageable);

    Page<Comunidad> findByNombreContainingIgnoreCaseAndEstado(
            String nombre, EstadoComunidad estado, Pageable pageable);

    @Query(
            value =
                    """
SELECT DISTINCT c
FROM Comunidad c
LEFT JOIN c.institution inst
LEFT JOIN c.categorias cat
WHERE c.estado = :estado
  AND (:tipoGrupos IS NULL OR c.tipoGrupo IN :tipoGrupos)
    AND (:tipoPlanes IS NULL OR c.tipoPlan IN :tipoPlanes)
  AND (
          :categorias IS NULL
          OR LOWER(COALESCE(cat.nombre, '')) IN :categorias
      )
  AND (
        :institucion IS NULL
        OR :institucion = ''
        OR LOWER(COALESCE(inst.nombre, '')) LIKE LOWER(CONCAT('%', :institucion, '%'))
      )
  AND (
        :search IS NULL
        OR :search = ''
        OR LOWER(c.nombre) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(COALESCE(c.descripcion, '')) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(COALESCE(cat.nombre, '')) LIKE LOWER(CONCAT('%', :search, '%'))
      )
""",
            countQuery =
                    """
SELECT COUNT(DISTINCT c.id)
FROM Comunidad c
LEFT JOIN c.institution inst
LEFT JOIN c.categorias cat
WHERE c.estado = :estado
    AND (:tipoGrupos IS NULL OR c.tipoGrupo IN :tipoGrupos)
    AND (:tipoPlanes IS NULL OR c.tipoPlan IN :tipoPlanes)
  AND (
      :categorias IS NULL
      OR LOWER(COALESCE(cat.nombre, '')) IN :categorias
      )
  AND (
        :institucion IS NULL
        OR :institucion = ''
        OR LOWER(COALESCE(inst.nombre, '')) LIKE LOWER(CONCAT('%', :institucion, '%'))
      )
  AND (
        :search IS NULL
        OR :search = ''
        OR LOWER(c.nombre) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(COALESCE(c.descripcion, '')) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(COALESCE(cat.nombre, '')) LIKE LOWER(CONCAT('%', :search, '%'))
      )
""")
    Page<Comunidad> searchActiveCommunities(
            @Param("search") String search,
            @Param("categorias") List<String> categorias,
            @Param("institucion") String institucion,
            @Param("tipoGrupos") List<TipoGrupo> tipoGrupos,
            @Param("tipoPlanes") List<TipoPlanComunidad> tipoPlanes,
            @Param("estado") EstadoComunidad estado,
            Pageable pageable);
}
