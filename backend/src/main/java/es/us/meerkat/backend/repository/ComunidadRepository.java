package es.us.meerkat.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import es.us.meerkat.backend.entity.Comunidad;
import es.us.meerkat.backend.entity.EstadoComunidad;
import es.us.meerkat.backend.entity.TipoGrupo;
import es.us.meerkat.backend.entity.TipoPlanComunidad;

@Repository
public interface ComunidadRepository extends JpaRepository<Comunidad, Long> {

    Page<Comunidad> findByTipoGrupoAndEstado(
            TipoGrupo tipoGrupo, EstadoComunidad estado, Pageable pageable);

    long countByCreadorIdAndTipoPlan(Long userId, TipoPlanComunidad tipoPlan);

    List<Comunidad> findByCreadorId(Long userId);

    Page<Comunidad> findByTipoGrupoAndNombreContainingIgnoreCaseAndEstado(
            TipoGrupo tipoGrupo, String nombre, EstadoComunidad estado, Pageable pageable);

    @EntityGraph(attributePaths = {"creador"})
    Optional<Comunidad> findWithCreadorById(Long id);

    Page<Comunidad> findByEstado(EstadoComunidad estado, Pageable pageable);

    Page<Comunidad> findByNombreContainingIgnoreCaseAndEstado(
            String nombre, EstadoComunidad estado, Pageable pageable);
}
