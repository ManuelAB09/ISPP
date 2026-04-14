package es.us.meerkat.backend.repository.communities;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import es.us.meerkat.backend.entity.communities.SolicitudComunidad;
import es.us.meerkat.backend.entity.tutors.EstadoSolicitud;

@Repository
public interface SolicitudComunidadRepository extends JpaRepository<SolicitudComunidad, Long> {

    Optional<SolicitudComunidad> findBySolicitanteIdAndComunidadIdAndEstado(
            Long solicitanteId, Long comunidadId, EstadoSolicitud estado);

    Page<SolicitudComunidad> findByComunidadIdAndEstado(
            Long comunidadId, EstadoSolicitud estado, Pageable pageable);

    Page<SolicitudComunidad> findByComunidadId(Long comunidadId, Pageable pageable);

    Optional<SolicitudComunidad> findBySolicitanteIdAndComunidadId(
            Long solicitanteId, Long comunidadId);

    /** Elimina todas las solicitudes hechas por un usuario. */
    void deleteBySolicitanteId(Long usuarioId);

    /** Elimina todas las solicitudes respondidas por un usuario. */
    void deleteByRespondidaPorId(Long usuarioId);
}
