package es.us.meerkat.backend.service.communities;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import es.us.meerkat.backend.entity.communities.Comunidad;
import es.us.meerkat.backend.entity.communities.EstadoComunidad;
import es.us.meerkat.backend.entity.communities.MiembroComunidad;
import es.us.meerkat.backend.entity.communities.RolComunidad;
import es.us.meerkat.backend.entity.communities.TipoGrupo;
import es.us.meerkat.backend.entity.suscriptions.Suscripcion;
import es.us.meerkat.backend.entity.suscriptions.TipoPlan;
import es.us.meerkat.backend.entity.suscriptions.TipoPlanComunidad;
import es.us.meerkat.backend.entity.tutors.Tutor;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.communities.ComunidadRepository;
import es.us.meerkat.backend.repository.communities.MiembroComunidadRepository;
import es.us.meerkat.backend.repository.tutors.TutorRepository;
import es.us.meerkat.backend.repository.users.UsuarioRepository;
import es.us.meerkat.backend.service.communities.AuthorizationService;
import es.us.meerkat.backend.service.communities.CommunityService;
import es.us.meerkat.backend.service.suscriptions.SuscripcionService;

@ExtendWith(MockitoExtension.class)
class CommunityServiceTest {

    @Mock private ComunidadRepository comunidadRepository;
    @Mock private MiembroComunidadRepository miembroComunidadRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private TutorRepository tutorRepository;
    @Mock private AuthorizationService authorizationService;
    @Mock private SuscripcionService suscripcionService;

    @InjectMocks private CommunityService communityService;

    @Test
    void createCommunityShouldCreateFreeCommunityAndAssignAdmin() {
        Long userId = 1L;
        Usuario usuario = buildUsuario(userId);

        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));
        when(suscripcionService.obtenerMiSuscripcion(userId))
                .thenReturn(Optional.of(Suscripcion.builder().plan(TipoPlan.FREE).build()));
        when(comunidadRepository.countByCreadorIdAndInstitutionIsNull(userId)).thenReturn(0L);
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
        assertThat(saved.getMaxMiembros()).isEqualTo(30);
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
        when(suscripcionService.obtenerMiSuscripcion(userId))
                .thenReturn(Optional.of(Suscripcion.builder().plan(TipoPlan.FREE).build()));
        when(comunidadRepository.countByCreadorIdAndInstitutionIsNull(userId)).thenReturn(3L);

        assertThatThrownBy(
                        () ->
                                communityService.createCommunity(
                                        userId,
                                        "Comunidad",
                                        "Desc",
                                        TipoGrupo.COMUNIDAD_PUBLICA,
                                        null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("límite de 3 comunidades");
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
        assertThat(updated.getMaxMiembros()).isEqualTo(75);
        verify(comunidadRepository).save(comunidad);
    }

    @Test
    void listActiveCommunitiesShouldUseSearchFilterWhenSearchProvided() {
        when(comunidadRepository.findByNombreContainingIgnoreCaseAndEstado(
                        "java", EstadoComunidad.ACTIVA, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(java.util.List.of()));

        communityService.listActiveCommunities("java", PageRequest.of(0, 20));

        verify(comunidadRepository)
                .findByNombreContainingIgnoreCaseAndEstado(
                        "java", EstadoComunidad.ACTIVA, PageRequest.of(0, 20));
    }

    @Test
    void createCommunityShouldSetRolDocenteWhenRolInicialProfesor() {
        Long userId = 1L;
        Usuario usuario = buildUsuario(userId);
        usuario.setEsTutor(true);

        Tutor tutor = new Tutor();
        tutor.setUsuario(usuario);
        tutor.setEspecialidades(List.of("Matematicas"));
        tutor.setTarifaHora(BigDecimal.valueOf(12));
        tutor.setBio("Tutor activo");

        MiembroComunidad savedMembership =
                MiembroComunidad.builder().id(50L).usuario(usuario).rol(RolComunidad.ADMIN).build();

        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));
        when(tutorRepository.findByUsuario(usuario)).thenReturn(Optional.of(tutor));
        when(suscripcionService.obtenerMiSuscripcion(userId))
                .thenReturn(Optional.of(Suscripcion.builder().plan(TipoPlan.FREE).build()));
        when(comunidadRepository.countByCreadorIdAndInstitutionIsNull(userId)).thenReturn(0L);
        when(comunidadRepository.save(any(Comunidad.class)))
                .thenAnswer(
                        invocation -> {
                            Comunidad c = invocation.getArgument(0);
                            c.setId(10L);
                            return c;
                        });
        when(miembroComunidadRepository.findByUsuarioIdAndComunidadId(userId, 10L))
                .thenReturn(Optional.of(savedMembership));
        when(miembroComunidadRepository.save(any(MiembroComunidad.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        communityService.createCommunity(
                userId,
                "Comunidad tutor",
                "Desc",
                TipoGrupo.COMUNIDAD_PUBLICA,
                "img",
                (Long) null,
                (Integer) null,
                RolComunidad.PROFESOR);

        assertThat(savedMembership.getRol()).isEqualTo(RolComunidad.ADMIN);
        assertThat(savedMembership.getRolDocente()).isEqualTo(RolComunidad.PROFESOR);
        verify(miembroComunidadRepository).findByUsuarioIdAndComunidadId(userId, 10L);
        verify(miembroComunidadRepository, atLeast(2)).save(any(MiembroComunidad.class));
    }

    @Test
    void createCommunityShouldFailWhenRolInicialProfesorAndTutorProfileIncomplete() {
        Long userId = 1L;
        Usuario usuario = buildUsuario(userId);
        usuario.setEsTutor(true);

        Tutor tutor = new Tutor();
        tutor.setUsuario(usuario);
        tutor.setEspecialidades(List.of());
        tutor.setTarifaHora(BigDecimal.valueOf(12));
        tutor.setBio("Bio");

        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));
        when(tutorRepository.findByUsuario(usuario)).thenReturn(Optional.of(tutor));

        assertThatThrownBy(
                        () ->
                                communityService.createCommunity(
                                        userId,
                                        "Comunidad tutor",
                                        "Desc",
                                        TipoGrupo.COMUNIDAD_PUBLICA,
                                        "img",
                                        (Long) null,
                                        (Integer) null,
                                        RolComunidad.PROFESOR))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("perfil de tutor");

        verify(comunidadRepository, never()).save(any(Comunidad.class));
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
