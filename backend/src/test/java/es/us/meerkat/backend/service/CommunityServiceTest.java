package es.us.meerkat.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import es.us.meerkat.backend.entity.Comunidad;
import es.us.meerkat.backend.entity.EstadoComunidad;
import es.us.meerkat.backend.entity.MiembroComunidad;
import es.us.meerkat.backend.entity.RolComunidad;
import es.us.meerkat.backend.entity.TipoGrupo;
import es.us.meerkat.backend.entity.TipoPlanComunidad;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.ComunidadRepository;
import es.us.meerkat.backend.repository.MiembroComunidadRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class CommunityServiceTest {

    @Mock private ComunidadRepository comunidadRepository;
    @Mock private MiembroComunidadRepository miembroComunidadRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AuthorizationService authorizationService;

    @InjectMocks private CommunityService communityService;

    @Test
    void createCommunityShouldCreateFreeCommunityAndAssignAdmin() {
        Long userId = 1L;
        Usuario usuario = buildUsuario(userId);

        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));
        when(comunidadRepository.countByCreadorIdAndTipoPlan(userId, TipoPlanComunidad.FREE))
                .thenReturn(0L);
        when(comunidadRepository.save(any(Comunidad.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Comunidad comunidad =
                communityService.createCommunity(
                        userId,
                        "Comunidad de estudio",
                        "Descripción",
                        TipoGrupo.COMUNIDAD_PUBLICA,
                        "img");

        ArgumentCaptor<Comunidad> comunidadCaptor = ArgumentCaptor.forClass(Comunidad.class);
        verify(comunidadRepository).save(comunidadCaptor.capture());
        Comunidad saved = comunidadCaptor.getValue();

        assertThat(saved.getNombre()).isEqualTo("Comunidad de estudio");
        assertThat(saved.getTipoPlan()).isEqualTo(TipoPlanComunidad.FREE);
        assertThat(saved.getMaxMiembros()).isEqualTo(50);
        assertThat(saved.getCreador()).isEqualTo(usuario);

        ArgumentCaptor<MiembroComunidad> memberCaptor =
                ArgumentCaptor.forClass(MiembroComunidad.class);
        verify(miembroComunidadRepository).save(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getRol()).isEqualTo(RolComunidad.ADMIN);

        assertThat(comunidad).isNotNull();
        assertThat(comunidad.getTipoPlan()).isEqualTo(TipoPlanComunidad.FREE);
    }

    @Test
    void createCommunityShouldFailWhenFreeLimitReached() {
        Long userId = 1L;
        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(buildUsuario(userId)));
        when(comunidadRepository.countByCreadorIdAndTipoPlan(userId, TipoPlanComunidad.FREE))
                .thenReturn(3L);

        assertThatThrownBy(
                        () ->
                                communityService.createCommunity(
                                        userId,
                                        "Comunidad",
                                        "Desc",
                                        TipoGrupo.COMUNIDAD_PUBLICA,
                                        null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("límite de 3 comunidades gratuitas");
    }

    @Test
    void getCommunityByIdShouldFailForPrivateCommunityWhenUserIsNotMember() {
        Comunidad comunidad = buildComunidad(10L, TipoGrupo.GRUPO_PRIVADO, TipoPlanComunidad.FREE);
        when(comunidadRepository.findById(10L)).thenReturn(Optional.of(comunidad));
        when(authorizationService.isMemberOf(2L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> communityService.getCommunityById(10L, 2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("comunidad privada");
    }

    @Test
    void updatePrivacyShouldPersistNewTypeWhenUserIsAdmin() {
        Comunidad comunidad =
                buildComunidad(10L, TipoGrupo.COMUNIDAD_PUBLICA, TipoPlanComunidad.FREE);
        when(authorizationService.isAdminOf(1L, 10L)).thenReturn(true);
        when(comunidadRepository.findById(10L)).thenReturn(Optional.of(comunidad));
        when(comunidadRepository.save(any(Comunidad.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Comunidad updated = communityService.updatePrivacy(1L, 10L, TipoGrupo.GRUPO_PRIVADO);

        assertThat(updated.getTipoGrupo()).isEqualTo(TipoGrupo.GRUPO_PRIVADO);
        verify(comunidadRepository).save(comunidad);
    }

    @Test
    void upgradeToPremiumShouldIncreaseCapacityAndSetPremiumPlan() {
        Comunidad comunidad =
                buildComunidad(10L, TipoGrupo.COMUNIDAD_PUBLICA, TipoPlanComunidad.FREE);
        when(authorizationService.isAdminOf(1L, 10L)).thenReturn(true);
        when(comunidadRepository.findById(10L)).thenReturn(Optional.of(comunidad));
        when(comunidadRepository.save(any(Comunidad.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Comunidad updated = communityService.upgradeToPremium(1L, 10L);

        assertThat(updated.getTipoPlan()).isEqualTo(TipoPlanComunidad.PREMIUM);
        assertThat(updated.getMaxMiembros()).isEqualTo(200);
        verify(comunidadRepository).save(comunidad);
    }

    @Test
    void listPublicCommunitiesShouldUseSearchFilterWhenSearchProvided() {
        when(comunidadRepository.findByTipoGrupoAndNombreContainingIgnoreCaseAndEstado(
                        TipoGrupo.COMUNIDAD_PUBLICA,
                        "java",
                        EstadoComunidad.ACTIVA,
                        PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(java.util.List.of()));

        communityService.listPublicCommunities("java", PageRequest.of(0, 20));

        verify(comunidadRepository)
                .findByTipoGrupoAndNombreContainingIgnoreCaseAndEstado(
                        TipoGrupo.COMUNIDAD_PUBLICA,
                        "java",
                        EstadoComunidad.ACTIVA,
                        PageRequest.of(0, 20));
    }

    private Usuario buildUsuario(final Long id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNombre("Usuario " + id);
        usuario.setEmail("user" + id + "@meerkat.es");
        return usuario;
    }

    private Comunidad buildComunidad(
            final Long id, final TipoGrupo tipo, final TipoPlanComunidad plan) {
        return Comunidad.builder()
                .id(id)
                .nombre("Comunidad")
                .descripcion("Desc")
                .tipoGrupo(tipo)
                .tipoPlan(plan)
                .maxMiembros(50)
                .creador(buildUsuario(1L))
                .build();
    }
}
