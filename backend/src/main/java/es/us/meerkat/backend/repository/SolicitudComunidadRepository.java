package es.us.meerkat.backend.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import es.us.meerkat.backend.entity.EstadoSolicitud;
import es.us.meerkat.backend.entity.SolicitudComunidad;

@Repository
public interface SolicitudComunidadRepository extends JpaRepository<SolicitudComunidad, Long> {

    Optional<SolicitudComunidad> findBySolicitanteIdAndComunidadIdAndEstado(
            Long solicitanteId, Long comunidadId, EstadoSolicitud estado);

    Page<SolicitudComunidad> findByComunidadIdAndEstado(
            Long comunidadId, EstadoSolicitud estado, Pageable pageable);

    Page<SolicitudComunidad> findByComunidadId(Long comunidadId, Pageable pageable);

    Optional<SolicitudComunidad> findBySolicitanteIdAndComunidadId(
            Long solicitanteId, Long comunidadId);
}
