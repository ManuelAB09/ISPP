package es.us.meerkat.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import es.us.meerkat.backend.entity.MiembroComunidad;
import es.us.meerkat.backend.entity.RolComunidad;

@Repository
public interface MiembroComunidadRepository extends JpaRepository<MiembroComunidad, Long> {

    Optional<MiembroComunidad> findByUsuarioIdAndComunidadId(Long usuarioId, Long comunidadId);

    Page<MiembroComunidad> findByComunidadId(Long comunidadId, Pageable pageable);

    Page<MiembroComunidad> findByComunidadIdAndRol(
            Long comunidadId, RolComunidad rol, Pageable pageable);

    long countByComunidadId(Long comunidadId);

    List<MiembroComunidad> findByUsuarioIdAndRol(Long usuarioId, RolComunidad rol);

    long countByComunidadIdAndRol(Long comunidadId, RolComunidad rol);
}
