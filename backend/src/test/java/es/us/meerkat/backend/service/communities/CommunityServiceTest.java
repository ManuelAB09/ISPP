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
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import es.us.meerkat.backend.entity.communities.Comunidad;
import es.us.meerkat.backend.entity.communities.EstadoComunidad;
import es.us.meerkat.backend.entity.communities.MiembroComunidad;
import es.us.meerkat.backend.entity.communities.RolComunidad;
import es.us.meerkat.backend.entity.communities.TipoGrupo;
import es.us.meerkat.backend.entity.events.Evento;
import es.us.meerkat.backend.entity.subscriptions.Suscripcion;
import es.us.meerkat.backend.entity.subscriptions.TipoPlan;
import es.us.meerkat.backend.entity.subscriptions.TipoPlanComunidad;
import es.us.meerkat.backend.entity.tutors.Tutor;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.chats.MensajeComunidadLeidoRepository;
import es.us.meerkat.backend.repository.chats.MensajeComunidadRepository;
import es.us.meerkat.backend.repository.communities.AnuncioRepository;
import es.us.meerkat.backend.repository.communities.ApunteRepository;
import es.us.meerkat.backend.repository.communities.ComentarioAnuncioRepository;
import es.us.meerkat.backend.repository.communities.ComunidadRepository;
import es.us.meerkat.backend.repository.communities.InstitutionRepository;
import es.us.meerkat.backend.repository.communities.InvitacionMiembroRepository;
import es.us.meerkat.backend.repository.communities.MiembroComunidadRepository;
import es.us.meerkat.backend.repository.events.EventoRepository;
import es.us.meerkat.backend.repository.forms.CuestionarioRepository;
import es.us.meerkat.backend.repository.google.CalificacionClassroomRepository;
import es.us.meerkat.backend.repository.google.ComunidadClassroomRepository;
import es.us.meerkat.backend.repository.google.RecursoClassroomRepository;
import es.us.meerkat.backend.repository.google.TareaClassroomRepository;
import es.us.meerkat.backend.repository.recommendations.FeedbackRepository;
import es.us.meerkat.backend.repository.recommendations.RecomendacionComunidadRepository;
import es.us.meerkat.backend.repository.tutors.TutorContratacionRepository;
import es.us.meerkat.backend.repository.tutors.TutorRepository;
import es.us.meerkat.backend.repository.users.UsuarioRepository;
import es.us.meerkat.backend.repository.zoom.GrabacionClaseRepository;
import es.us.meerkat.backend.repository.zoom.ZoomMeetingRepository;
import es.us.meerkat.backend.service.subscriptions.SuscripcionService;

@ExtendWith(MockitoExtension.class)
class CommunityServiceTest {

    @Mock private ComunidadRepository comunidadRepository;
    @Mock private MiembroComunidadRepository miembroComunidadRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private InstitutionRepository institutionRepository;
    @Mock private AuthorizationService authorizationService;
    @Mock private SuscripcionService suscripcionService;
    @Mock private MensajeComunidadRepository mensajeComunidadRepository;
    @Mock private MensajeComunidadLeidoRepository mensajeComunidadLeidoRepository;
    @Mock private ComunidadClassroomRepository comunidadClassroomRepository;
    @Mock private AnuncioRepository anuncioRepository;
    @Mock private ComentarioAnuncioRepository comentarioAnuncioRepository;
    @Mock private ApunteRepository apunteRepository;
    @Mock private InvitacionMiembroRepository invitacionMiembroRepository;
    @Mock private TareaClassroomRepository tareaClassroomRepository;
    @Mock private RecursoClassroomRepository recursoClassroomRepository;
    @Mock private CalificacionClassroomRepository calificacionClassroomRepository;
    @Mock private GrabacionClaseRepository grabacionClaseRepository;
    @Mock private TutorContratacionRepository tutorContratacionRepository;
    @Mock private RecomendacionComunidadRepository recomendacionComunidadRepository;
    @Mock private FeedbackRepository feedbackRepository;
    @Mock private EventoRepository eventoRepository;
    @Mock private CuestionarioRepository cuestionarioRepository;
    @Mock private ZoomMeetingRepository zoomMeetingRepository;
    @Mock private TutorRepository tutorRepository;

    @InjectMocks private CommunityService communityService;

    @Test
    void createCommunityShouldCreateFreeCommunityAndAssignAdmin() {
        Long userId = 1L;
        Usuario usuario = buildUsuario(userId);

        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));
        when(suscripcionService.obtenerMiSuscripcion(userId))
                .thenReturn(Optional.of(Suscripcion.builder().plan(TipoPlan.FREE).build()));
        when(comunidadRepository.countManagedNonInstitutionCommunities(userId)).thenReturn(0L);
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
        when(comunidadRepository.countManagedNonInstitutionCommunities(userId)).thenReturn(3L);

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
    void createCommunityShouldFailWhenNameAlreadyExists() {
        Long userId = 1L;
        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(buildUsuario(userId)));
        when(comunidadRepository.existsByNombreIgnoreCase("Comunidad Existente")).thenReturn(true);

        assertThatThrownBy(
                        () ->
                                communityService.createCommunity(
                                        userId,
                                        "Comunidad Existente",
                                        "Desc",
                                        TipoGrupo.COMUNIDAD_PUBLICA,
                                        null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya existe una comunidad con el nombre");
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
        when(comunidadRepository.searchActiveCommunities(
                        "java",
                        null,
                        null,
                        null,
                        null,
                        EstadoComunidad.ACTIVA,
                        PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(java.util.List.of()));

        communityService.listActiveCommunities(
                "java", null, null, null, null, PageRequest.of(0, 20));

        verify(comunidadRepository)
                .searchActiveCommunities(
                        "java",
                        null,
                        null,
                        null,
                        null,
                        EstadoComunidad.ACTIVA,
                        PageRequest.of(0, 20));
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
        when(comunidadRepository.countManagedNonInstitutionCommunities(userId)).thenReturn(0L);
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

    // ================================================================
    // updateCommunity
    // ================================================================

    @Test
    void updateCommunityShouldUpdateFields() {
        Comunidad comunidad =
                buildComunidad(10L, TipoGrupo.COMUNIDAD_PUBLICA, TipoPlanComunidad.FREE);
        when(authorizationService.isAdminOf(1L, 10L)).thenReturn(true);
        when(comunidadRepository.findById(10L)).thenReturn(Optional.of(comunidad));
        when(comunidadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Comunidad result =
                communityService.updateCommunity(
                        1L, 10L, "Nuevo nombre", "Nueva desc", "img.png", null);

        assertThat(result.getNombre()).isEqualTo("Nuevo nombre");
        assertThat(result.getDescripcion()).isEqualTo("Nueva desc");
        assertThat(result.getImagenUrl()).isEqualTo("img.png");
    }

    @Test
    void updateCommunityShouldThrowWhenNotAdmin() {
        when(authorizationService.isAdminOf(1L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> communityService.updateCommunity(1L, 10L, "N", "D", null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateCommunityShouldNotUpdateNameWhenBlank() {
        Comunidad comunidad =
                buildComunidad(10L, TipoGrupo.COMUNIDAD_PUBLICA, TipoPlanComunidad.FREE);
        when(authorizationService.isAdminOf(1L, 10L)).thenReturn(true);
        when(comunidadRepository.findById(10L)).thenReturn(Optional.of(comunidad));
        when(comunidadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Comunidad result = communityService.updateCommunity(1L, 10L, "  ", null, null, null);

        assertThat(result.getNombre()).isEqualTo("Comunidad"); // unchanged
    }

    @Test
    void updateCommunityShouldUpdateMaxMiembrosWhenAboveCurrent() {
        Comunidad comunidad =
                buildComunidad(10L, TipoGrupo.COMUNIDAD_PUBLICA, TipoPlanComunidad.FREE);
        when(authorizationService.isAdminOf(1L, 10L)).thenReturn(true);
        when(comunidadRepository.findById(10L)).thenReturn(Optional.of(comunidad));
        when(miembroComunidadRepository.countByComunidadId(10L)).thenReturn(3L);
        when(comunidadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Comunidad result = communityService.updateCommunity(1L, 10L, null, null, null, 20);

        assertThat(result.getMaxMiembros()).isEqualTo(20);
    }

    @Test
    void updateCommunityShouldRejectMaxMiembrosBelowCurrentCount() {
        Comunidad comunidad =
                buildComunidad(10L, TipoGrupo.COMUNIDAD_PUBLICA, TipoPlanComunidad.FREE);
        when(authorizationService.isAdminOf(1L, 10L)).thenReturn(true);
        when(comunidadRepository.findById(10L)).thenReturn(Optional.of(comunidad));
        when(miembroComunidadRepository.countByComunidadId(10L)).thenReturn(8L);

        assertThatThrownBy(() -> communityService.updateCommunity(1L, 10L, null, null, null, 5))
                .isInstanceOf(es.us.meerkat.backend.exception.ValidationException.class)
                .hasMessageContaining("miembros actuales");
    }

    // ================================================================
    // deleteCommunity
    // ================================================================

    @Test
    void deleteCommunityShouldDeleteWhenAdmin() {
        Comunidad comunidad =
                buildComunidad(10L, TipoGrupo.COMUNIDAD_PUBLICA, TipoPlanComunidad.FREE);
        when(authorizationService.isAdminOf(1L, 10L)).thenReturn(true);
        when(comunidadRepository.findById(10L)).thenReturn(Optional.of(comunidad));

        communityService.deleteCommunity(1L, 10L);

        verify(eventoRepository).disassociateFromComunidad(10L);
        verify(comunidadRepository).delete(comunidad);
    }

    @Test
    void deleteCommunityShouldThrowWhenNotAdmin() {
        when(authorizationService.isAdminOf(1L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> communityService.deleteCommunity(1L, 10L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ================================================================
    // getCommunityById
    // ================================================================

    @Test
    void getCommunityByIdShouldReturnPublicCommunity() {
        Comunidad comunidad =
                buildComunidad(10L, TipoGrupo.COMUNIDAD_PUBLICA, TipoPlanComunidad.FREE);
        when(comunidadRepository.findById(10L)).thenReturn(Optional.of(comunidad));

        Comunidad result = communityService.getCommunityById(10L, 99L);

        assertThat(result.getId()).isEqualTo(10L);
    }

    @Test
    void getCommunityByIdShouldAllowPrivateCommunityForMember() {
        Comunidad comunidad = buildComunidad(10L, TipoGrupo.GRUPO_PRIVADO, TipoPlanComunidad.FREE);
        when(comunidadRepository.findById(10L)).thenReturn(Optional.of(comunidad));
        when(authorizationService.isMemberOf(1L, 10L)).thenReturn(true);

        Comunidad result = communityService.getCommunityById(10L, 1L);

        assertThat(result).isNotNull();
    }

    @Test
    void getCommunityByIdShouldThrowWhenNotFound() {
        when(comunidadRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> communityService.getCommunityById(99L, 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ================================================================
    // countMembers / getMaxMembers / canAddMember
    // ================================================================

    @Test
    void countMembersShouldReturnCount() {
        when(miembroComunidadRepository.countByComunidadId(10L)).thenReturn(5L);

        assertThat(communityService.countMembers(10L)).isEqualTo(5L);
    }

    @Test
    void getMaxMembersShouldReturnFromCommunity() {
        Comunidad comunidad =
                buildComunidad(10L, TipoGrupo.COMUNIDAD_PUBLICA, TipoPlanComunidad.FREE);
        when(comunidadRepository.findById(10L)).thenReturn(Optional.of(comunidad));

        assertThat(communityService.getMaxMembers(10L)).isEqualTo(50);
    }

    @Test
    void canAddMemberShouldReturnTrueWhenBelowLimit() {
        Comunidad comunidad =
                buildComunidad(10L, TipoGrupo.COMUNIDAD_PUBLICA, TipoPlanComunidad.FREE);
        when(comunidadRepository.findById(10L)).thenReturn(Optional.of(comunidad));
        when(miembroComunidadRepository.countByComunidadId(10L)).thenReturn(10L);

        assertThat(communityService.canAddMember(10L)).isTrue();
    }

    @Test
    void canAddMemberShouldReturnFalseWhenAtLimit() {
        Comunidad comunidad =
                buildComunidad(10L, TipoGrupo.COMUNIDAD_PUBLICA, TipoPlanComunidad.FREE);
        when(comunidadRepository.findById(10L)).thenReturn(Optional.of(comunidad));
        when(miembroComunidadRepository.countByComunidadId(10L)).thenReturn(50L);

        assertThat(communityService.canAddMember(10L)).isFalse();
    }

    @Test
    void canAddMemberShouldReturnTrueWhenNullMaxMembers() {
        Comunidad comunidad = Comunidad.builder().id(10L).nombre("C").maxMiembros(null).build();
        when(comunidadRepository.findById(10L)).thenReturn(Optional.of(comunidad));

        assertThat(communityService.canAddMember(10L)).isTrue();
    }

    // ================================================================
    // isCommunityCorporate
    // ================================================================

    @Test
    void isCommunityCorporateShouldReturnTrueWhenHasInstitution() {
        Comunidad comunidad =
                buildComunidad(10L, TipoGrupo.COMUNIDAD_PUBLICA, TipoPlanComunidad.FREE);
        comunidad.setInstitution(new es.us.meerkat.backend.entity.communities.Institution());
        when(comunidadRepository.findById(10L)).thenReturn(Optional.of(comunidad));

        assertThat(communityService.isCommunityCorporate(10L)).isTrue();
    }

    @Test
    void isCommunityCorporateShouldReturnFalseWhenNotFound() {
        when(comunidadRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(communityService.isCommunityCorporate(99L)).isFalse();
    }

    // ================================================================
    // listActiveCommunities (no search)
    // ================================================================

    @Test
    void listActiveCommunitiesShouldUseDefaultFilterWhenNoSearch() {
        when(comunidadRepository.searchActiveCommunities(
                        null,
                        null,
                        null,
                        null,
                        null,
                        EstadoComunidad.ACTIVA,
                        PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of()));

        communityService.listActiveCommunities(null, null, null, null, null, PageRequest.of(0, 10));

        verify(comunidadRepository)
                .searchActiveCommunities(
                        null,
                        null,
                        null,
                        null,
                        null,
                        EstadoComunidad.ACTIVA,
                        PageRequest.of(0, 10));
    }

    @Test
    void listActiveCommunitiesShouldUseDefaultFilterWhenBlankSearch() {
        when(comunidadRepository.searchActiveCommunities(
                        null,
                        null,
                        null,
                        null,
                        null,
                        EstadoComunidad.ACTIVA,
                        PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of()));

        communityService.listActiveCommunities("  ", null, null, null, null, PageRequest.of(0, 10));

        verify(comunidadRepository)
                .searchActiveCommunities(
                        null,
                        null,
                        null,
                        null,
                        null,
                        EstadoComunidad.ACTIVA,
                        PageRequest.of(0, 10));
    }

    // ================================================================
    // upgradeToPremium - already premium
    // ================================================================

    @Test
    void upgradeToPremiumShouldThrowWhenAlreadyPremium() {
        Comunidad comunidad =
                buildComunidad(10L, TipoGrupo.COMUNIDAD_PUBLICA, TipoPlanComunidad.PREMIUM);
        when(authorizationService.isAdminOf(1L, 10L)).thenReturn(true);
        when(comunidadRepository.findById(10L)).thenReturn(Optional.of(comunidad));

        assertThatThrownBy(() -> communityService.upgradeToPremium(1L, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya es Premium");
    }

    // ================================================================
    // upgradeToPremium – happy path
    // ================================================================

    @Test
    void upgradeToPremiumShouldUpgradeFreeToPremium() {
        Comunidad comunidad =
                buildComunidad(10L, TipoGrupo.COMUNIDAD_PUBLICA, TipoPlanComunidad.FREE);
        when(authorizationService.isAdminOf(1L, 10L)).thenReturn(true);
        when(comunidadRepository.findById(10L)).thenReturn(Optional.of(comunidad));
        when(comunidadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Comunidad result = communityService.upgradeToPremium(1L, 10L);

        assertThat(result.getTipoPlan()).isEqualTo(TipoPlanComunidad.PREMIUM);
    }

    // ================================================================
    // updatePrivacy
    // ================================================================

    @Test
    void updatePrivacyShouldChangeTipoGrupo() {
        Comunidad comunidad =
                buildComunidad(10L, TipoGrupo.COMUNIDAD_PUBLICA, TipoPlanComunidad.FREE);
        when(authorizationService.isAdminOf(1L, 10L)).thenReturn(true);
        when(comunidadRepository.findById(10L)).thenReturn(Optional.of(comunidad));
        when(comunidadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Comunidad result = communityService.updatePrivacy(1L, 10L, TipoGrupo.GRUPO_PRIVADO);

        assertThat(result.getTipoGrupo()).isEqualTo(TipoGrupo.GRUPO_PRIVADO);
    }

    @Test
    void updatePrivacyShouldThrowWhenNotAdmin() {
        when(authorizationService.isAdminOf(1L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> communityService.updatePrivacy(1L, 10L, TipoGrupo.GRUPO_PRIVADO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ================================================================
    // createCommunity – Premium plan
    // ================================================================

    @Test
    void createCommunityShouldSetPremiumPlanForPremiumUser() {
        Long userId = 1L;
        Usuario usuario = buildUsuario(userId);

        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));
        when(suscripcionService.obtenerMiSuscripcion(userId))
                .thenReturn(Optional.of(Suscripcion.builder().plan(TipoPlan.PREMIUM).build()));
        when(comunidadRepository.countManagedNonInstitutionCommunities(userId)).thenReturn(0L);
        when(comunidadRepository.save(any(Comunidad.class))).thenAnswer(inv -> inv.getArgument(0));

        Comunidad result =
                communityService.createCommunity(
                        userId, "Test", "Desc", TipoGrupo.COMUNIDAD_PUBLICA, null);

        assertThat(result.getTipoPlan()).isEqualTo(TipoPlanComunidad.PREMIUM);
        assertThat(result.getMaxMiembros()).isEqualTo(75);
    }

    @Test
    void createCommunityShouldThrowWhenFreeUserExceedsLimit() {
        Long userId = 1L;
        Usuario usuario = buildUsuario(userId);

        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));
        when(suscripcionService.obtenerMiSuscripcion(userId))
                .thenReturn(Optional.of(Suscripcion.builder().plan(TipoPlan.FREE).build()));
        when(comunidadRepository.countManagedNonInstitutionCommunities(userId)).thenReturn(3L);

        assertThatThrownBy(
                        () ->
                                communityService.createCommunity(
                                        userId, "Test", "D", TipoGrupo.COMUNIDAD_PUBLICA, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("límite");
    }

    @Test
    void createCommunityShouldThrowWhenMaxMembersExceedsPlanLimit() {
        Long userId = 1L;
        Usuario usuario = buildUsuario(userId);

        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));
        when(suscripcionService.obtenerMiSuscripcion(userId))
                .thenReturn(Optional.of(Suscripcion.builder().plan(TipoPlan.FREE).build()));
        when(comunidadRepository.countManagedNonInstitutionCommunities(userId)).thenReturn(0L);

        assertThatThrownBy(
                        () ->
                                communityService.createCommunity(
                                        userId,
                                        "Test",
                                        "D",
                                        TipoGrupo.COMUNIDAD_PUBLICA,
                                        null,
                                        999))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("máximo de miembros");
    }

    @Test
    void createCommunityShouldAssignDefaultImageWhenBlank() {
        Long userId = 1L;
        Usuario usuario = buildUsuario(userId);

        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));
        when(suscripcionService.obtenerMiSuscripcion(userId))
                .thenReturn(Optional.of(Suscripcion.builder().plan(TipoPlan.FREE).build()));
        when(comunidadRepository.countManagedNonInstitutionCommunities(userId)).thenReturn(0L);
        when(comunidadRepository.save(any(Comunidad.class))).thenAnswer(inv -> inv.getArgument(0));

        Comunidad result =
                communityService.createCommunity(
                        userId, "Test", "D", TipoGrupo.COMUNIDAD_PUBLICA, "  ");

        assertThat(result.getImagenUrl()).contains("community-default");
    }

    // ================================================================
    // actualizarFotoComunidad
    // ================================================================

    @Test
    void actualizarFotoShouldThrowWhenNotAdmin() {
        when(authorizationService.isAdminOf(1L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> communityService.actualizarFotoComunidad(1L, 10L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("admin");
    }

    @Test
    void actualizarFotoShouldThrowWhenFileNull() {
        when(authorizationService.isAdminOf(1L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> communityService.actualizarFotoComunidad(1L, 10L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requerido");
    }

    @Test
    void actualizarFotoShouldThrowWhenFileTooLarge() {
        when(authorizationService.isAdminOf(1L, 10L)).thenReturn(true);
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(10L * 1024L * 1024L);

        assertThatThrownBy(() -> communityService.actualizarFotoComunidad(1L, 10L, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("5MB");
    }

    @Test
    void actualizarFotoShouldThrowWhenMimeTypeInvalid() {
        when(authorizationService.isAdminOf(1L, 10L)).thenReturn(true);
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn("application/pdf");

        assertThatThrownBy(() -> communityService.actualizarFotoComunidad(1L, 10L, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Formato");
    }

    @Test
    void actualizarFotoShouldSaveBase64Image() throws Exception {
        Comunidad comunidad =
                buildComunidad(10L, TipoGrupo.COMUNIDAD_PUBLICA, TipoPlanComunidad.FREE);
        when(authorizationService.isAdminOf(1L, 10L)).thenReturn(true);
        when(comunidadRepository.findById(10L)).thenReturn(Optional.of(comunidad));
        when(comunidadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getBytes()).thenReturn(new byte[] {1, 2, 3});

        Comunidad result = communityService.actualizarFotoComunidad(1L, 10L, file);

        assertThat(result.getImagenUrl()).startsWith("data:image/png;base64,");
    }

    // ================================================================
    // getCommunityRanking
    // ================================================================

    @Test
    void getCommunityRankingShouldThrowWhenNotMember() {
        when(authorizationService.isMemberOf(1L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> communityService.getCommunityRanking(10L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("miembro");
    }

    @Test
    void getCommunityRankingShouldReturnRankedMembers() {
        when(authorizationService.isMemberOf(1L, 10L)).thenReturn(true);

        Usuario u1 = buildUsuario(1L);
        u1.setNombre("Alice");
        Usuario u2 = buildUsuario(2L);
        u2.setNombre("Bob");

        MiembroComunidad m1 = MiembroComunidad.builder().usuario(u1).build();
        MiembroComunidad m2 = MiembroComunidad.builder().usuario(u2).build();

        when(miembroComunidadRepository.findByComunidadId(eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(m1, m2)));
        when(mensajeComunidadRepository.countMensajesByComunidad(10L))
                .thenReturn(List.of(new Object[] {1L, 5L}, new Object[] {2L, 2L}));

        Evento evento = new Evento();
        evento.setCreador(u1);
        evento.setAsistentesConfirmados(3);
        when(eventoRepository.findByComunidadId(10L)).thenReturn(List.of(evento));

        var ranking = communityService.getCommunityRanking(10L, 1L);

        assertThat(ranking).hasSize(2);
        // u1: 5 msgs + 3*5 asistentes = 20, u2: 2 msgs + 0 = 2
        assertThat(ranking.get(0).puntos()).isEqualTo(20L);
        assertThat(ranking.get(1).puntos()).isEqualTo(2L);
    }

    @Test
    void getCommunityRankingShouldHandleNullCreadorInEvents() {
        when(authorizationService.isMemberOf(1L, 10L)).thenReturn(true);

        Usuario u1 = buildUsuario(1L);
        u1.setNombre("Alice");
        MiembroComunidad m1 = MiembroComunidad.builder().usuario(u1).build();

        when(miembroComunidadRepository.findByComunidadId(eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(m1)));
        when(mensajeComunidadRepository.countMensajesByComunidad(10L)).thenReturn(List.of());

        Evento eventoSinCreador = new Evento();
        eventoSinCreador.setCreador(null); // null creador - should be skipped
        when(eventoRepository.findByComunidadId(10L)).thenReturn(List.of(eventoSinCreador));

        var ranking = communityService.getCommunityRanking(10L, 1L);

        assertThat(ranking).hasSize(1);
        assertThat(ranking.get(0).puntos()).isZero();
    }

    // ================================================================
    // getCommunityById – private non-member
    // ================================================================

    @Test
    void getCommunityByIdShouldThrowForPrivateNonMember() {
        Comunidad comunidad = buildComunidad(10L, TipoGrupo.GRUPO_PRIVADO, TipoPlanComunidad.FREE);
        when(comunidadRepository.findById(10L)).thenReturn(Optional.of(comunidad));
        when(authorizationService.isMemberOf(99L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> communityService.getCommunityById(10L, 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("privada");
    }

    // ================================================================
    // listActiveCommunities – with search
    // ================================================================

    @Test
    void listActiveCommunitiesShouldSearchByName() {
        when(comunidadRepository.searchActiveCommunities(
                        eq("java"),
                        org.mockito.ArgumentMatchers.isNull(),
                        org.mockito.ArgumentMatchers.isNull(),
                        org.mockito.ArgumentMatchers.isNull(),
                        org.mockito.ArgumentMatchers.isNull(),
                        eq(EstadoComunidad.ACTIVA),
                        any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        communityService.listActiveCommunities(
                "java", null, null, null, null, PageRequest.of(0, 10));

        verify(comunidadRepository)
                .searchActiveCommunities(
                        eq("java"),
                        org.mockito.ArgumentMatchers.isNull(),
                        org.mockito.ArgumentMatchers.isNull(),
                        org.mockito.ArgumentMatchers.isNull(),
                        org.mockito.ArgumentMatchers.isNull(),
                        eq(EstadoComunidad.ACTIVA),
                        any(Pageable.class));
    }
}
