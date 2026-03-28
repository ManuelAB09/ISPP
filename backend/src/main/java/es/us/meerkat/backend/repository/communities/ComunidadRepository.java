package es.us.meerkat.backend.repository.communities;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import es.us.meerkat.backend.entity.communities.Comunidad;
import es.us.meerkat.backend.entity.communities.EstadoComunidad;
import es.us.meerkat.backend.entity.communities.TipoGrupo;
import es.us.meerkat.backend.entity.suscriptions.TipoPlanComunidad;

@Repository
public interface ComunidadRepository extends JpaRepository<Comunidad, Long> {

    Page<Comunidad> findByTipoGrupoAndEstado(
            TipoGrupo tipoGrupo, EstadoComunidad estado, Pageable pageable);

    long countByCreadorIdAndTipoPlan(Long userId, TipoPlanComunidad tipoPlan);

    long countByCreadorIdAndInstitutionIsNull(Long userId);

    long countByInstitutionId(Long institutionId);

    List<Comunidad> findByCreadorId(Long userId);

    Page<Comunidad> findByTipoGrupoAndNombreContainingIgnoreCaseAndEstado(
            TipoGrupo tipoGrupo, String nombre, EstadoComunidad estado, Pageable pageable);

    Page<Comunidad> findByEstado(EstadoComunidad estado, Pageable pageable);

    Page<Comunidad> findByNombreContainingIgnoreCaseAndEstado(
            String nombre, EstadoComunidad estado, Pageable pageable);
}
