package es.us.meerkat.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import es.us.meerkat.backend.entity.Comunidad;
import es.us.meerkat.backend.entity.MiembroComunidad;
import es.us.meerkat.backend.entity.RolComunidad;
import es.us.meerkat.backend.entity.TipoGrupo;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.ComunidadRepository;
import es.us.meerkat.backend.repository.MiembroComunidadRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock private MiembroComunidadRepository miembroComunidadRepository;
    @Mock private ComunidadRepository comunidadRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AuthorizationService authorizationService;
    @Mock private CommunityService communityService;

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

        MiembroComunidad miembro = memberService.joinPublicCommunity(userId, communityId);

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

        assertThatThrownBy(() -> memberService.joinPublicCommunity(userId, communityId))
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

        assertThatThrownBy(() -> memberService.joinPublicCommunity(userId, communityId))
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

        MiembroComunidad result = memberService.transferAdmin(adminId, communityId, newAdminId);

        assertThat(actual.getRol()).isEqualTo(RolComunidad.ALUMNO);
        assertThat(nuevo.getRol()).isEqualTo(RolComunidad.ADMIN);
        assertThat(result.getRol()).isEqualTo(RolComunidad.ADMIN);
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
