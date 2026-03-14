package es.us.meerkat.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.us.meerkat.backend.entity.MiembroComunidad;
import es.us.meerkat.backend.entity.RolComunidad;
import es.us.meerkat.backend.repository.MiembroComunidadRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthorizationService {

    private final MiembroComunidadRepository miembroComunidadRepository;

    /** Verifica si un usuario es administrador de una comunidad específica. */
    public boolean isAdminOf(Long userId, Long communityId) {
        return miembroComunidadRepository
                .findByUsuarioIdAndComunidadId(userId, communityId)
                .map(m -> m.getRol() == RolComunidad.ADMIN)
                .orElse(false);
    }

    /** Verifica si un usuario es miembro de una comunidad específica. */
    public boolean isMemberOf(Long userId, Long communityId) {
        return miembroComunidadRepository
                .findByUsuarioIdAndComunidadId(userId, communityId)
                .isPresent();
    }

    /** Obtiene el rol de un usuario en una comunidad específica. Retorna null si no es miembro. */
    public RolComunidad getUserRoleInCommunity(Long userId, Long communityId) {
        return miembroComunidadRepository
                .findByUsuarioIdAndComunidadId(userId, communityId)
                .map(MiembroComunidad::getRol)
                .orElse(null);
    }

    /** Obtiene la membresía de un usuario en una comunidad. Retorna null si no es miembro. */
    public MiembroComunidad getMembership(Long userId, Long communityId) {
        return miembroComunidadRepository
                .findByUsuarioIdAndComunidadId(userId, communityId)
                .orElse(null);
    }

    /**
     * Obtiene el rol como String de un usuario en una comunidad específica. Retorna null si no es
     * miembro.
     */
    public String getUserRoleInCommunityAsString(Long userId, Long communityId) {
        RolComunidad rol = getUserRoleInCommunity(userId, communityId);
        return rol != null ? rol.name() : null;
    }

    /** Verifica si un usuario es ADMIN o PROFESOR en una comunidad. */
    public boolean isAdminOrProfesor(Long userId, Long communityId) {
        return miembroComunidadRepository
                .findByUsuarioIdAndComunidadId(userId, communityId)
                .map(
                        m -> {
                            RolComunidad r = m.getRol();
                            return r == RolComunidad.ADMIN || r == RolComunidad.PROFESOR;
                        })
                .orElse(false);
    }
}
