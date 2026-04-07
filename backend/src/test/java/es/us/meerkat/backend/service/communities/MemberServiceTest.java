package es.us.meerkat.backend.service.communities;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import es.us.meerkat.backend.entity.communities.Comunidad;
import es.us.meerkat.backend.entity.communities.MiembroComunidad;
import es.us.meerkat.backend.entity.communities.RolComunidad;
import es.us.meerkat.backend.entity.communities.TipoGrupo;
import es.us.meerkat.backend.entity.subscriptions.Suscripcion;
import es.us.meerkat.backend.entity.subscriptions.TipoPlan;
import es.us.meerkat.backend.entity.subscriptions.TipoPlanComunidad;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.communities.ComunidadRepository;
import es.us.meerkat.backend.repository.communities.MiembroComunidadRepository;
import es.us.meerkat.backend.repository.events.AsistenciaEventoRepository;
import es.us.meerkat.backend.repository.google.ComunidadClassroomRepository;
import es.us.meerkat.backend.repository.users.UsuarioRepository;
import es.us.meerkat.backend.service.google.GoogleClassroomService;
import es.us.meerkat.backend.service.subscriptions.SuscripcionService;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock private MiembroComunidadRepository miembroComunidadRepository;
    @Mock private ComunidadRepository comunidadRepository;
    @Mock private ComunidadClassroomRepository comunidadClassroomRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AsistenciaEventoRepository asistenciaEventoRepository;
    @Mock private AuthorizationService authorizationService;
    @Mock private CommunityService communityService;
    @Mock private GoogleClassroomService googleClassroomService;
    @Mock private SuscripcionService suscripcionService;

    @InjectMocks private MemberService memberService;

    @Test
    void joinPublicCommunityShouldCreateMembershipWhenCommunityIsPublicAndHasSpace() {
        Long userId = 1L;
        Long communityId = 10L;
        Usuario usuario = buildUsuario(userId);
        Comunidad comunidad = buildComunidad(communityId, TipoGrupo.COMUNIDAD_PUBLICA);

        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));
        when(comunidadRepository.findById(communityId)).thenReturn(Optional.of(comunidad));
        when(authorizationService.isMemberOf(userId, communityId)).thenReturn(false);
        when(communityService.countMembers(communityId)).thenReturn(10L);
        when(communityService.getMaxMembers(communityId)).thenReturn(50);
        when(miembroComunidadRepository.save(any(MiembroComunidad.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MiembroComunidad miembro = memberService.joinPublicCommunity(userId, communityId, null);

        verify(miembroComunidadRepository).save(miembro);
        assertThat(miembro.getUsuario()).isEqualTo(usuario);
        assertThat(miembro.getComunidad()).isEqualTo(comunidad);
        assertThat(miembro.getRol()).isEqualTo(RolComunidad.ALUMNO);
    }

    @Test
    void joinPublicCommunityShouldFailForPrivateCommunity() {
        Long userId = 1L;
        Long communityId = 10L;
        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(buildUsuario(userId)));
        when(comunidadRepository.findById(communityId))
                .thenReturn(Optional.of(buildComunidad(communityId, TipoGrupo.GRUPO_PRIVADO)));

        assertThatThrownBy(() -> memberService.joinPublicCommunity(userId, communityId, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("comunidad privada");
    }

    @Test
    void joinPublicCommunityShouldFailWhenCommunityIsFull() {
        Long userId = 1L;
        Long communityId = 10L;
        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(buildUsuario(userId)));
        when(comunidadRepository.findById(communityId))
                .thenReturn(Optional.of(buildComunidad(communityId, TipoGrupo.COMUNIDAD_PUBLICA)));
        when(authorizationService.isMemberOf(userId, communityId)).thenReturn(false);
        when(communityService.countMembers(communityId)).thenReturn(50L);
        when(communityService.getMaxMembers(communityId)).thenReturn(50);

        assertThatThrownBy(() -> memberService.joinPublicCommunity(userId, communityId, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La comunidad está llena");
    }

    @Test
    void leaveCommunityShouldFailWhenAdminIsOnlyAdmin() {
        Long userId = 1L;
        Long communityId = 10L;
        MiembroComunidad admin = MiembroComunidad.builder().rol(RolComunidad.ADMIN).build();

        when(miembroComunidadRepository.findByUsuarioIdAndComunidadId(userId, communityId))
                .thenReturn(Optional.of(admin));
        when(miembroComunidadRepository.countByComunidadIdAndRol(communityId, RolComunidad.ADMIN))
                .thenReturn(1L);
        when(miembroComunidadRepository.countByComunidadId(communityId)).thenReturn(5L);

        assertThatThrownBy(() -> memberService.leaveCommunity(userId, communityId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("único admin");

        verify(miembroComunidadRepository, never()).delete(admin);
    }

    @Test
    void expelMemberShouldFailWhenAdminTriesToExpelSelf() {
        when(authorizationService.isAdminOf(1L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> memberService.expelMember(1L, 10L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No puedes expulsarte a ti mismo");
    }

    @Test
    void transferAdminShouldChangeRolesWhenBothUsersAreMembers() {
        Long adminId = 1L;
        Long newAdminId = 2L;
        Long communityId = 10L;

        MiembroComunidad actual = MiembroComunidad.builder().rol(RolComunidad.ADMIN).build();
        MiembroComunidad nuevo = MiembroComunidad.builder().rol(RolComunidad.ALUMNO).build();

        when(authorizationService.isAdminOf(adminId, communityId)).thenReturn(true);
        when(miembroComunidadRepository.findByUsuarioIdAndComunidadId(newAdminId, communityId))
                .thenReturn(Optional.of(nuevo));
        when(miembroComunidadRepository.findByUsuarioIdAndComunidadId(adminId, communityId))
                .thenReturn(Optional.of(actual));
        when(miembroComunidadRepository.save(any(MiembroComunidad.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        // Community is FREE — downgrade block should not trigger
        when(comunidadRepository.findById(communityId))
                .thenReturn(Optional.of(buildComunidad(communityId, TipoGrupo.COMUNIDAD_PUBLICA)));

        MiembroComunidad result =
                memberService.transferAdmin(adminId, communityId, newAdminId, null);

        assertThat(actual.getRol()).isEqualTo(RolComunidad.ALUMNO);
        assertThat(nuevo.getRol()).isEqualTo(RolComunidad.ADMIN);
        assertThat(result.getRol()).isEqualTo(RolComunidad.ADMIN);
    }

    @Test
    void transferAdminShouldDowngradePremiumCommunityWhenNewAdminIsNotPremium() {
        Long adminId = 1L;
        Long newAdminId = 2L;
        Long communityId = 10L;

        MiembroComunidad actual = MiembroComunidad.builder().rol(RolComunidad.ADMIN).build();
        MiembroComunidad nuevo = MiembroComunidad.builder().rol(RolComunidad.ALUMNO).build();
        Comunidad comunidadPremium =
                Comunidad.builder()
                        .id(communityId)
                        .tipoPlan(TipoPlanComunidad.PREMIUM)
                        .maxMiembros(75)
                        .build();

        when(authorizationService.isAdminOf(adminId, communityId)).thenReturn(true);
        when(miembroComunidadRepository.findByUsuarioIdAndComunidadId(newAdminId, communityId))
                .thenReturn(Optional.of(nuevo));
        when(miembroComunidadRepository.findByUsuarioIdAndComunidadId(adminId, communityId))
                .thenReturn(Optional.of(actual));
        when(miembroComunidadRepository.save(any(MiembroComunidad.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(comunidadRepository.findById(communityId)).thenReturn(Optional.of(comunidadPremium));
        when(suscripcionService.obtenerMiSuscripcion(newAdminId)).thenReturn(Optional.empty());

        memberService.transferAdmin(adminId, communityId, newAdminId, null);

        assertThat(comunidadPremium.getTipoPlan()).isEqualTo(TipoPlanComunidad.FREE);
        assertThat(comunidadPremium.getMaxMiembros()).isEqualTo(30);
        verify(comunidadRepository).save(comunidadPremium);
    }

    @Test
    void transferAdminShouldKeepPremiumWhenNewAdminHasPremiumSubscription() {
        Long adminId = 1L;
        Long newAdminId = 2L;
        Long communityId = 10L;

        MiembroComunidad actual = MiembroComunidad.builder().rol(RolComunidad.ADMIN).build();
        MiembroComunidad nuevo = MiembroComunidad.builder().rol(RolComunidad.ALUMNO).build();
        Comunidad comunidadPremium =
                Comunidad.builder()
                        .id(communityId)
                        .tipoPlan(TipoPlanComunidad.PREMIUM)
                        .maxMiembros(75)
                        .build();
        Suscripcion suscripcionPremium = new Suscripcion();
        suscripcionPremium.setPlan(TipoPlan.PREMIUM);
        suscripcionPremium.setActiva(true);

        when(authorizationService.isAdminOf(adminId, communityId)).thenReturn(true);
        when(miembroComunidadRepository.findByUsuarioIdAndComunidadId(newAdminId, communityId))
                .thenReturn(Optional.of(nuevo));
        when(miembroComunidadRepository.findByUsuarioIdAndComunidadId(adminId, communityId))
                .thenReturn(Optional.of(actual));
        when(miembroComunidadRepository.save(any(MiembroComunidad.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(comunidadRepository.findById(communityId)).thenReturn(Optional.of(comunidadPremium));
        when(suscripcionService.obtenerMiSuscripcion(newAdminId))
                .thenReturn(Optional.of(suscripcionPremium));

        memberService.transferAdmin(adminId, communityId, newAdminId, null);

        assertThat(comunidadPremium.getTipoPlan()).isEqualTo(TipoPlanComunidad.PREMIUM);
        verify(comunidadRepository, never()).save(comunidadPremium);
    }

    @Test
    void promoteToAdminShouldSetTargetRoleToAdmin() {
        Long adminId = 1L;
        Long targetUserId = 2L;
        Long communityId = 10L;

        MiembroComunidad target = MiembroComunidad.builder().rol(RolComunidad.ALUMNO).build();

        when(authorizationService.isAdminOf(adminId, communityId)).thenReturn(true);
        when(miembroComunidadRepository.findByUsuarioIdAndComunidadId(targetUserId, communityId))
                .thenReturn(Optional.of(target));
        when(miembroComunidadRepository.save(any(MiembroComunidad.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MiembroComunidad result = memberService.promoteToAdmin(adminId, communityId, targetUserId);

        assertThat(target.getRol()).isEqualTo(RolComunidad.ADMIN);
        assertThat(result.getRol()).isEqualTo(RolComunidad.ADMIN);
    }

    @Test
    void promoteToAdminShouldFailWhenTargetAlreadyAdmin() {
        Long adminId = 1L;
        Long targetUserId = 2L;
        Long communityId = 10L;

        MiembroComunidad target = MiembroComunidad.builder().rol(RolComunidad.ADMIN).build();

        when(authorizationService.isAdminOf(adminId, communityId)).thenReturn(true);
        when(miembroComunidadRepository.findByUsuarioIdAndComunidadId(targetUserId, communityId))
                .thenReturn(Optional.of(target));

        assertThatThrownBy(() -> memberService.promoteToAdmin(adminId, communityId, targetUserId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya es admin");

        verify(miembroComunidadRepository, never()).save(any(MiembroComunidad.class));
    }

    @Test
    void addAdminShouldFailWhenCommunityIsNotCorporate() {
        Long adminId = 1L;
        Long targetUserId = 2L;
        Long communityId = 10L;

        when(authorizationService.isAdminOf(adminId, communityId)).thenReturn(true);
        when(communityService.isCommunityCorporate(communityId)).thenReturn(false);

        assertThatThrownBy(() -> memberService.addAdmin(adminId, communityId, targetUserId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("comunidades corporativas");

        verify(miembroComunidadRepository, never()).save(any(MiembroComunidad.class));
    }

    @Test
    void addAdminShouldPromoteMemberWhenCommunityIsCorporate() {
        Long adminId = 1L;
        Long targetUserId = 2L;
        Long communityId = 10L;

        MiembroComunidad targetMember = MiembroComunidad.builder().rol(RolComunidad.ALUMNO).build();

        when(authorizationService.isAdminOf(adminId, communityId)).thenReturn(true);
        when(communityService.isCommunityCorporate(communityId)).thenReturn(true);
        when(miembroComunidadRepository.findByUsuarioIdAndComunidadId(targetUserId, communityId))
                .thenReturn(Optional.of(targetMember));
        when(miembroComunidadRepository.save(any(MiembroComunidad.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MiembroComunidad result = memberService.addAdmin(adminId, communityId, targetUserId);

        assertThat(result.getRol()).isEqualTo(RolComunidad.ADMIN);
        verify(miembroComunidadRepository).save(targetMember);
    }

    private Usuario buildUsuario(final Long id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNombre("Usuario " + id);
        usuario.setEmail("user" + id + "@meerkat.es");
        return usuario;
    }

    private Comunidad buildComunidad(final Long id, final TipoGrupo tipoGrupo) {
        return Comunidad.builder().id(id).nombre("Comunidad").tipoGrupo(tipoGrupo).build();
    }
}
