package es.us.meerkat.backend.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.us.meerkat.backend.entity.Comunidad;
import es.us.meerkat.backend.entity.EstadoSolicitud;
import es.us.meerkat.backend.entity.MiembroComunidad;
import es.us.meerkat.backend.entity.RolComunidad;
import es.us.meerkat.backend.entity.SolicitudComunidad;
import es.us.meerkat.backend.entity.TipoGrupo;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.ComunidadRepository;
import es.us.meerkat.backend.repository.MiembroComunidadRepository;
import es.us.meerkat.backend.repository.SolicitudComunidadRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class RequestService {

    private final SolicitudComunidadRepository solicitudComunidadRepository;
    private final ComunidadRepository comunidadRepository;
    private final MiembroComunidadRepository miembroComunidadRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuthorizationService authorizationService;
    private final CommunityService communityService;

    /** Solicita acceso a una comunidad privada. */
    public SolicitudComunidad requestAccess(Long userId, Long communityId, String mensaje) {
        Usuario usuario =
                usuarioRepository
                        .findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Comunidad comunidad =
                comunidadRepository
                        .findById(communityId)
                        .orElseThrow(() -> new IllegalArgumentException("Comunidad no encontrada"));

        // Validar que sea privada
        if (comunidad.getTipoGrupo() == TipoGrupo.COMUNIDAD_PUBLICA) {
            throw new IllegalArgumentException(
                    "No necesitas solicitar acceso a una comunidad pública");
        }

        // Validar que no sea ya miembro
        if (authorizationService.isMemberOf(userId, communityId)) {
            throw new IllegalArgumentException("Ya eres miembro de esta comunidad");
        }

        // Validar que no haya solicitud pendiente
        SolicitudComunidad existing =
                solicitudComunidadRepository
                        .findBySolicitanteIdAndComunidadIdAndEstado(
                                userId, communityId, EstadoSolicitud.PENDIENTE)
                        .orElse(null);

        if (existing != null) {
            throw new IllegalArgumentException(
                    "Ya tienes una solicitud pendiente para esta comunidad");
        }

        // Crear solicitud
        SolicitudComunidad solicitud =
                SolicitudComunidad.builder()
                        .solicitante(usuario)
                        .comunidad(comunidad)
                        .mensaje(mensaje)
                        .estado(EstadoSolicitud.PENDIENTE)
                        .build();

        return solicitudComunidadRepository.save(solicitud);
    }

    /** Lista las solicitudes de una comunidad (solo ADMIN). */
    @Transactional(readOnly = true)
    public Page<SolicitudComunidad> listRequests(
            Long userId, Long communityId, EstadoSolicitud estado, Pageable pageable) {
        if (!authorizationService.isAdminOf(userId, communityId)) {
            throw new IllegalArgumentException("Solo admins pueden ver solicitudes");
        }

        if (estado != null) {
            return solicitudComunidadRepository.findByComunidadIdAndEstado(
                    communityId, estado, pageable);
        } else {
            return solicitudComunidadRepository.findByComunidadId(communityId, pageable);
        }
    }

    /** Responde a una solicitud de acceso (acepta o rechaza). */
    public SolicitudComunidad respondToRequest(
            Long userId, Long communityId, Long requestId, boolean aceptado) {
        if (!authorizationService.isAdminOf(userId, communityId)) {
            throw new IllegalArgumentException("Solo admins pueden responder solicitudes");
        }

        SolicitudComunidad solicitud =
                solicitudComunidadRepository
                        .findById(requestId)
                        .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));

        // Validar que la solicitud pertenezca a esta comunidad
        if (!solicitud.getComunidad().getId().equals(communityId)) {
            throw new IllegalArgumentException("La solicitud no pertenece a esta comunidad");
        }

        // Validar que esté pendiente
        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new IllegalArgumentException("Esta solicitud ya fue respondida");
        }

        Usuario admin =
                usuarioRepository
                        .findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("Admin no encontrado"));

        solicitud.setRespondidaPor(admin);
        solicitud.setFechaRespuesta(LocalDateTime.now());

        if (aceptado) {
            // Validar aforo
            long currentMembers = communityService.countMembers(communityId);
            int maxMembers = communityService.getMaxMembers(communityId);
            if (currentMembers >= maxMembers) {
                throw new IllegalArgumentException(
                        "La comunidad está llena, no se puede aceptar más miembros");
            }

            solicitud.setEstado(EstadoSolicitud.ACEPTADA);

            // Crear membresía
            MiembroComunidad miembro =
                    MiembroComunidad.builder()
                            .usuario(solicitud.getSolicitante())
                            .comunidad(solicitud.getComunidad())
                            .rol(RolComunidad.ALUMNO)
                            .build();

            miembroComunidadRepository.save(miembro);
        } else {
            solicitud.setEstado(EstadoSolicitud.RECHAZADA);
        }

        return solicitudComunidadRepository.save(solicitud);
    }

    /** Obtiene una solicitud por ID (interno). */
    @Transactional(readOnly = true)
    protected SolicitudComunidad getRequest(Long requestId) {
        return solicitudComunidadRepository
                .findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));
    }

    /** Comprueba si el usuario tiene una solicitud PENDIENTE en la comunidad. */
    @Transactional(readOnly = true)
    public boolean hasPendingRequest(Long userId, Long communityId) {
        return solicitudComunidadRepository
                .findBySolicitanteIdAndComunidadIdAndEstado(
                        userId, communityId, EstadoSolicitud.PENDIENTE)
                .isPresent();
    }
}
