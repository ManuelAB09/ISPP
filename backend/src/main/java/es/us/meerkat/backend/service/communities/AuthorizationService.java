package es.us.meerkat.backend.service.communities;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.us.meerkat.backend.entity.communities.MiembroComunidad;
import es.us.meerkat.backend.entity.communities.RolComunidad;
import es.us.meerkat.backend.entity.communities.TipoGrupo;
import es.us.meerkat.backend.repository.communities.ComunidadRepository;
import es.us.meerkat.backend.repository.communities.MiembroComunidadRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthorizationService {

    private final MiembroComunidadRepository miembroComunidadRepository;
    private final ComunidadRepository comunidadRepository;

    /**
     * Verifica si un usuario es administrador de una comunidad específica. Devuelve true si tiene
     * rol ADMIN en la membresía O si es el creador de la comunidad.
     */
    public boolean isAdminOf(Long userId, Long communityId) {
        boolean adminPorRol =
                miembroComunidadRepository
                        .findByUsuarioIdAndComunidadId(userId, communityId)
                        .map(m -> m.getRol() == RolComunidad.ADMIN)
                        .orElse(false);
        if (adminPorRol) {
            return true;
        }
        return comunidadRepository
                .findById(communityId)
                .map(c -> c.getCreador() != null && c.getCreador().getId().equals(userId))
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

    /** Permite participar si la comunidad es publica o si el usuario ya es miembro. */
    public boolean canParticipate(Long userId, Long communityId) {
        return comunidadRepository
                .findById(communityId)
                .map(
                        comunidad ->
                                comunidad.getTipoGrupo() == TipoGrupo.COMUNIDAD_PUBLICA
                                        || isMemberOf(userId, communityId))
                .orElse(false);
    }
}
