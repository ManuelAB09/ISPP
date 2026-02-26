package es.us.meerkat.backend.service;

import es.us.meerkat.backend.entity.Comunidad;
import es.us.meerkat.backend.entity.MiembroComunidad;
import es.us.meerkat.backend.entity.RolComunidad;
import es.us.meerkat.backend.entity.TipoGrupo;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.ComunidadRepository;
import es.us.meerkat.backend.repository.MiembroComunidadRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final MiembroComunidadRepository miembroComunidadRepository;
    private final ComunidadRepository comunidadRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuthorizationService authorizationService;
    private final CommunityService communityService;

    /**
     * Se une a una comunidad pública (verifica aforo y tipo).
     */
    public MiembroComunidad joinPublicCommunity(Long userId, Long communityId) {
        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Comunidad comunidad = comunidadRepository.findById(communityId)
                .orElseThrow(() -> new IllegalArgumentException("Comunidad no encontrada"));

        // Validar que sea pública
        if (comunidad.getTipoGrupo() == TipoGrupo.GRUPO_PRIVADO) {
            throw new IllegalArgumentException("No puedes unirte a una comunidad privada directamente. Solicita acceso.");
        }

        // Validar que no sea miembro ya
        if (authorizationService.isMemberOf(userId, communityId)) {
            throw new IllegalArgumentException("Ya eres miembro de esta comunidad");
        }

        // Validar aforo
        long currentMembers = communityService.countMembers(communityId);
        int maxMembers = communityService.getMaxMembers(communityId);
        if (currentMembers >= maxMembers) {
            throw new IllegalArgumentException("La comunidad está llena");
        }

        // Crear membresía
        MiembroComunidad miembro = MiembroComunidad.builder()
                .usuario(usuario)
                .comunidad(comunidad)
                .rol(RolComunidad.MIEMBRO)
                .build();

        return miembroComunidadRepository.save(miembro);
    }

    /**
     * Abandona una comunidad. Si es único admin, lanza error.
     */
    public void leaveCommunity(Long userId, Long communityId) {
        MiembroComunidad miembro = miembroComunidadRepository.findByUsuarioIdAndComunidadId(userId, communityId)
                .orElseThrow(() -> new IllegalArgumentException("No eres miembro de esta comunidad"));

        // Si es ADMIN, verificar que haya otros ADMINs
        if (miembro.getRol() == RolComunidad.ADMIN) {
            long adminCount = miembroComunidadRepository.countByComunidadIdAndRol(communityId, RolComunidad.ADMIN);
            if (adminCount <= 1) {
                throw new IllegalArgumentException("No puedes abandonar siendo el único admin. Transfiere la administración primero.");
            }
        }

        miembroComunidadRepository.delete(miembro);
    }

    /**
     * Obtiene la membresía de un usuario en una comunidad.
     */
    @Transactional(readOnly = true)
    public MiembroComunidad getMyMembership(Long userId, Long communityId) {
        return miembroComunidadRepository.findByUsuarioIdAndComunidadId(userId, communityId)
                .orElseThrow(() -> new IllegalArgumentException("No eres miembro de esta comunidad"));
    }

    /**
     * Expulsa a un miembro (solo ADMIN).
     */
    public void expelMember(Long userId, Long communityId, Long targetUserId) {
        if (!authorizationService.isAdminOf(userId, communityId)) {
            throw new IllegalArgumentException("Solo admins pueden expulsar miembros");
        }

        if (userId.equals(targetUserId)) {
            throw new IllegalArgumentException("No puedes expulsarte a ti mismo");
        }

        MiembroComunidad targetMiembro = miembroComunidadRepository.findByUsuarioIdAndComunidadId(targetUserId, communityId)
                .orElseThrow(() -> new IllegalArgumentException("El usuario no es miembro de esta comunidad"));

        // No se puede expulsar al único admin
        if (targetMiembro.getRol() == RolComunidad.ADMIN) {
            long adminCount = miembroComunidadRepository.countByComunidadIdAndRol(communityId, RolComunidad.ADMIN);
            if (adminCount <= 1) {
                throw new IllegalArgumentException("No puedes expulsar al único admin");
            }
        }

        miembroComunidadRepository.delete(targetMiembro);
    }

    /**
     * Lista los miembros de una comunidad.
     */
    @Transactional(readOnly = true)
    public Page<MiembroComunidad> listMembers(Long communityId, Pageable pageable) {
        return miembroComunidadRepository.findByComunidadId(communityId, pageable);
    }

    /**
     * Transfiere el rol ADMIN a otro miembro (solo ADMIN actual).
     */
    public MiembroComunidad transferAdmin(Long userId, Long communityId, Long newAdminId) {
        if (!authorizationService.isAdminOf(userId, communityId)) {
            throw new IllegalArgumentException("Solo admins pueden transferir administración");
        }

        if (userId.equals(newAdminId)) {
            throw new IllegalArgumentException("No puedes transferir a ti mismo");
        }

        // Verificar que el nuevo admin sea miembro
        MiembroComunidad nuevoAdmin = miembroComunidadRepository.findByUsuarioIdAndComunidadId(newAdminId, communityId)
                .orElseThrow(() -> new IllegalArgumentException("El nuevo admin debe ser miembro de la comunidad"));

        MiembroComunidad usuarioActual = miembroComunidadRepository.findByUsuarioIdAndComunidadId(userId, communityId)
                .orElseThrow(() -> new IllegalArgumentException("No eres miembro de esta comunidad"));

        // Cambiar roles
        usuarioActual.setRol(RolComunidad.MIEMBRO);
        nuevoAdmin.setRol(RolComunidad.ADMIN);

        miembroComunidadRepository.save(usuarioActual);
        return miembroComunidadRepository.save(nuevoAdmin);
    }

    /**
     * Cuenta los ADMINs de una comunidad específica.
     */
    @Transactional(readOnly = true)
    public long countAdmins(Long communityId) {
        return miembroComunidadRepository.countByComunidadIdAndRol(communityId, RolComunidad.ADMIN);
    }
}
