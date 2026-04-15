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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import es.us.meerkat.backend.entity.communities.Comunidad;
import es.us.meerkat.backend.entity.communities.MiembroComunidad;
import es.us.meerkat.backend.entity.communities.RolComunidad;
import es.us.meerkat.backend.entity.communities.TipoGrupo;
import es.us.meerkat.backend.entity.google.ComunidadClassroom;
import es.us.meerkat.backend.entity.subscriptions.Suscripcion;
import es.us.meerkat.backend.entity.subscriptions.TipoPlan;
import es.us.meerkat.backend.entity.subscriptions.TipoPlanComunidad;
import es.us.meerkat.backend.entity.tutors.Tutor;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.communities.ComunidadRepository;
import es.us.meerkat.backend.repository.communities.MiembroComunidadRepository;
import es.us.meerkat.backend.repository.events.AsistenciaEventoRepository;
import es.us.meerkat.backend.repository.google.ComunidadClassroomRepository;
import es.us.meerkat.backend.repository.tutors.TutorRepository;
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
        @Mock private TutorRepository tutorRepository;
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

    @Test
    void joinPublicCommunityShouldFailWhenUserNotFound() {
        Long userId = 999L;
        Long communityId = 10L;

        when(usuarioRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.joinPublicCommunity(userId, communityId, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void joinPublicCommunityShouldFailWhenCommunityNotFound() {
        Long userId = 1L;
        Long communityId = 999L;

        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(buildUsuario(userId)));
        when(comunidadRepository.findById(communityId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.joinPublicCommunity(userId, communityId, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void joinPublicCommunityShouldFailWhenUserAlreadyMember() {
        Long userId = 1L;
        Long communityId = 10L;

        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(buildUsuario(userId)));
        when(comunidadRepository.findById(communityId))
                .thenReturn(Optional.of(buildComunidad(communityId, TipoGrupo.COMUNIDAD_PUBLICA)));
        when(authorizationService.isMemberOf(userId, communityId)).thenReturn(true);

        assertThatThrownBy(() -> memberService.joinPublicCommunity(userId, communityId, null))
                .isInstanceOf(IllegalArgumentException.class);

        verify(miembroComunidadRepository, never()).save(any());
    }

    @Test
    void expelMemberShouldFailWhenUserNotAdmin() {
        when(authorizationService.isAdminOf(1L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> memberService.expelMember(1L, 10L, 2L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void promoteToAdminShouldFailWhenNotAdmin() {
        Long adminId = 1L;
        Long targetUserId = 2L;
        Long communityId = 10L;

        when(authorizationService.isAdminOf(adminId, communityId)).thenReturn(false);

        assertThatThrownBy(() -> memberService.promoteToAdmin(adminId, communityId, targetUserId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void transferAdminShouldFailWhenTargetNotMember() {
        Long adminId = 1L;
        Long newAdminId = 2L;
        Long communityId = 10L;

        when(authorizationService.isAdminOf(adminId, communityId)).thenReturn(true);
        when(miembroComunidadRepository.findByUsuarioIdAndComunidadId(newAdminId, communityId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                        () -> memberService.transferAdmin(adminId, communityId, newAdminId, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void joinPublicCommunityShouldFailWhenRequestedRoleProfesorAndUserIsNotTutor() {
        Long userId = 1L;
        Long communityId = 10L;
        Usuario usuario = buildUsuario(userId);
        usuario.setEsTutor(false);

        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));
        when(comunidadRepository.findById(communityId))
                .thenReturn(Optional.of(buildComunidad(communityId, TipoGrupo.COMUNIDAD_PUBLICA)));
        when(authorizationService.isMemberOf(userId, communityId)).thenReturn(false);
        when(communityService.countMembers(communityId)).thenReturn(1L);
        when(communityService.getMaxMembers(communityId)).thenReturn(20);

        assertThatThrownBy(
                        () ->
                                memberService.joinPublicCommunity(
                                        userId, communityId, RolComunidad.PROFESOR))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Solo los usuarios tutores");
    }

    @Test
    void joinPublicCommunityShouldFailWhenTutorProfileIsIncomplete() {
        Long userId = 2L;
        Long communityId = 11L;
        Usuario usuario = buildUsuario(userId);
        usuario.setEsTutor(true);
        Tutor tutor = new Tutor();
        tutor.setUsuario(usuario);
        tutor.setEspecialidades(java.util.List.of("Java"));
        tutor.setTarifaHora(new java.math.BigDecimal("10.00"));
        tutor.setBio(" ");

        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));
        when(comunidadRepository.findById(communityId))
                .thenReturn(Optional.of(buildComunidad(communityId, TipoGrupo.COMUNIDAD_PUBLICA)));
        when(authorizationService.isMemberOf(userId, communityId)).thenReturn(false);
        when(communityService.countMembers(communityId)).thenReturn(3L);
        when(communityService.getMaxMembers(communityId)).thenReturn(20);
        when(tutorRepository.findByUsuario(usuario)).thenReturn(Optional.of(tutor));

        assertThatThrownBy(
                        () ->
                                memberService.joinPublicCommunity(
                                        userId, communityId, RolComunidad.PROFESOR))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("completar tu perfil de tutor");
    }

    @Test
    void joinPublicCommunityShouldCreateProfesorMembershipWhenTutorProfileIsComplete() {
        Long userId = 3L;
        Long communityId = 12L;
        Usuario usuario = buildUsuario(userId);
        usuario.setEsTutor(true);
        Tutor tutor = new Tutor();
        tutor.setUsuario(usuario);
        tutor.setEspecialidades(java.util.List.of("Spring", "JPA"));
        tutor.setTarifaHora(new java.math.BigDecimal("18.00"));
        tutor.setBio("Tutor con experiencia");
        Comunidad comunidad = buildComunidad(communityId, TipoGrupo.COMUNIDAD_PUBLICA);

        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));
        when(comunidadRepository.findById(communityId)).thenReturn(Optional.of(comunidad));
        when(authorizationService.isMemberOf(userId, communityId)).thenReturn(false);
        when(communityService.countMembers(communityId)).thenReturn(2L);
        when(communityService.getMaxMembers(communityId)).thenReturn(20);
        when(tutorRepository.findByUsuario(usuario)).thenReturn(Optional.of(tutor));
        when(miembroComunidadRepository.save(any(MiembroComunidad.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        MiembroComunidad miembro =
                memberService.joinPublicCommunity(userId, communityId, RolComunidad.PROFESOR);

        assertThat(miembro.getRol()).isEqualTo(RolComunidad.PROFESOR);
        verify(miembroComunidadRepository).save(any(MiembroComunidad.class));
    }

    @Test
    void leaveCommunityShouldDeleteCommunityWhenUserIsLastMember() {
        Long userId = 4L;
        Long communityId = 13L;
        Comunidad comunidad = buildComunidad(communityId, TipoGrupo.COMUNIDAD_PUBLICA);
        MiembroComunidad miembro =
                MiembroComunidad.builder().usuario(buildUsuario(userId)).comunidad(comunidad).build();

        when(miembroComunidadRepository.findByUsuarioIdAndComunidadId(userId, communityId))
                .thenReturn(Optional.of(miembro));
        when(miembroComunidadRepository.countByComunidadId(communityId)).thenReturn(1L);

        memberService.leaveCommunity(userId, communityId);

        verify(comunidadRepository).delete(comunidad);
        verify(miembroComunidadRepository, never()).delete(any(MiembroComunidad.class));
    }

    @Test
    void leaveCommunityShouldFailWhenUserHasActiveEventAttendances() {
        Long userId = 5L;
        Long communityId = 14L;
        MiembroComunidad miembro =
                MiembroComunidad.builder()
                        .usuario(buildUsuario(userId))
                        .comunidad(buildComunidad(communityId, TipoGrupo.COMUNIDAD_PUBLICA))
                        .rol(RolComunidad.ALUMNO)
                        .build();

        when(miembroComunidadRepository.findByUsuarioIdAndComunidadId(userId, communityId))
                .thenReturn(Optional.of(miembro));
        when(miembroComunidadRepository.countByComunidadId(communityId)).thenReturn(2L);
        when(asistenciaEventoRepository.countActiveEventAttendances(eq(userId), eq(communityId), any()))
                .thenReturn(2L);

        assertThatThrownBy(() -> memberService.leaveCommunity(userId, communityId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("asistencia confirmada");
    }

    @Test
    void getMyMembershipShouldReturnMembershipWhenExists() {
        MiembroComunidad miembro = MiembroComunidad.builder().rol(RolComunidad.ALUMNO).build();
        when(miembroComunidadRepository.findByUsuarioIdAndComunidadId(1L, 10L))
                .thenReturn(Optional.of(miembro));

        MiembroComunidad result = memberService.getMyMembership(1L, 10L);

        assertThat(result).isEqualTo(miembro);
    }

    @Test
    void getMyMembershipShouldThrowWhenMembershipNotFound() {
        when(miembroComunidadRepository.findByUsuarioIdAndComunidadId(1L, 99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.getMyMembership(1L, 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No eres miembro");
    }

    @Test
    void listMembersShouldReturnRepositoryPage() {
        Page<MiembroComunidad> page =
                new PageImpl<>(java.util.List.of(MiembroComunidad.builder().build()));
        when(miembroComunidadRepository.findByComunidadId(10L, PageRequest.of(0, 20)))
                .thenReturn(page);

        Page<MiembroComunidad> result = memberService.listMembers(10L, PageRequest.of(0, 20));

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void listUserMembershipsShouldReturnRepositoryPage() {
        Page<MiembroComunidad> page =
                new PageImpl<>(java.util.List.of(MiembroComunidad.builder().build()));
        when(miembroComunidadRepository.findByUsuarioId(1L, PageRequest.of(0, 10)))
                .thenReturn(page);

        Page<MiembroComunidad> result = memberService.listUserMemberships(1L, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void countAdminsShouldDelegateToRepository() {
        when(miembroComunidadRepository.countByComunidadIdAndRol(10L, RolComunidad.ADMIN))
                .thenReturn(3L);

        long count = memberService.countAdmins(10L);

        assertThat(count).isEqualTo(3L);
    }

    @Test
    void joinPublicCommunityShouldSyncStudentToClassroomWhenCommunityIsLinked() {
        Long userId = 21L;
        Long communityId = 31L;
        Usuario usuario = buildUsuario(userId);
        usuario.setEsTutor(false);
        Comunidad comunidad = buildComunidad(communityId, TipoGrupo.COMUNIDAD_PUBLICA);
        ComunidadClassroom vinculacion =
                ComunidadClassroom.builder()
                        .comunidad(comunidad)
                        .classroomCourseId("course-1")
                        .classroomCourseName("Course")
                        .activa(true)
                        .build();
        Usuario admin = buildUsuario(100L);
        MiembroComunidad adminMember =
                MiembroComunidad.builder().usuario(admin).comunidad(comunidad).rol(RolComunidad.ADMIN).build();

        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));
        when(comunidadRepository.findById(communityId)).thenReturn(Optional.of(comunidad));
        when(authorizationService.isMemberOf(userId, communityId)).thenReturn(false);
        when(communityService.countMembers(communityId)).thenReturn(1L);
        when(communityService.getMaxMembers(communityId)).thenReturn(20);
        when(miembroComunidadRepository.save(any(MiembroComunidad.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(comunidadClassroomRepository.findByComunidadId(communityId))
                .thenReturn(Optional.of(vinculacion));
        when(miembroComunidadRepository.findByComunidadIdAndRol(communityId, RolComunidad.ADMIN))
                .thenReturn(java.util.List.of(adminMember));

        memberService.joinPublicCommunity(userId, communityId, RolComunidad.ALUMNO);

        verify(googleClassroomService)
                .crearEstudiante(
                        eq(admin), eq("course-1"), eq("{\"userId\":\"" + usuario.getEmail() + "\"}"));
    }

    @Test
    void joinPublicCommunityShouldSyncTeacherToClassroomWhenTutorJoinsAsProfesor() {
        Long userId = 22L;
        Long communityId = 32L;
        Usuario usuario = buildUsuario(userId);
        usuario.setEsTutor(true);
        Tutor tutor = new Tutor();
        tutor.setUsuario(usuario);
        tutor.setEspecialidades(java.util.List.of("IA"));
        tutor.setTarifaHora(new java.math.BigDecimal("15.00"));
        tutor.setBio("Bio completa");
        Comunidad comunidad = buildComunidad(communityId, TipoGrupo.COMUNIDAD_PUBLICA);
        ComunidadClassroom vinculacion =
                ComunidadClassroom.builder()
                        .comunidad(comunidad)
                        .classroomCourseId("course-2")
                        .classroomCourseName("Course 2")
                        .activa(true)
                        .build();
        Usuario admin = buildUsuario(101L);
        MiembroComunidad adminMember =
                MiembroComunidad.builder().usuario(admin).comunidad(comunidad).rol(RolComunidad.ADMIN).build();

        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));
        when(comunidadRepository.findById(communityId)).thenReturn(Optional.of(comunidad));
        when(authorizationService.isMemberOf(userId, communityId)).thenReturn(false);
        when(communityService.countMembers(communityId)).thenReturn(1L);
        when(communityService.getMaxMembers(communityId)).thenReturn(20);
        when(tutorRepository.findByUsuario(usuario)).thenReturn(Optional.of(tutor));
        when(miembroComunidadRepository.save(any(MiembroComunidad.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(comunidadClassroomRepository.findByComunidadId(communityId))
                .thenReturn(Optional.of(vinculacion));
        when(miembroComunidadRepository.findByComunidadIdAndRol(communityId, RolComunidad.ADMIN))
                .thenReturn(java.util.List.of(adminMember));

        memberService.joinPublicCommunity(userId, communityId, RolComunidad.PROFESOR);

        verify(googleClassroomService)
                .crearProfesor(
                        eq(admin), eq("course-2"), eq("{\"userId\":\"" + usuario.getEmail() + "\"}"));
    }

    @Test
    void leaveCommunityShouldUnsyncStudentFromClassroomAndDeleteMembership() {
        Long userId = 23L;
        Long communityId = 33L;
        Usuario usuario = buildUsuario(userId);
        usuario.setEsTutor(false);
        Comunidad comunidad = buildComunidad(communityId, TipoGrupo.COMUNIDAD_PUBLICA);
        MiembroComunidad miembro =
                MiembroComunidad.builder().usuario(usuario).comunidad(comunidad).rol(RolComunidad.ALUMNO).build();
        ComunidadClassroom vinculacion =
                ComunidadClassroom.builder()
                        .comunidad(comunidad)
                        .classroomCourseId("course-3")
                        .classroomCourseName("Course 3")
                        .activa(true)
                        .build();
        Usuario admin = buildUsuario(102L);
        MiembroComunidad adminMember =
                MiembroComunidad.builder().usuario(admin).comunidad(comunidad).rol(RolComunidad.ADMIN).build();

        when(miembroComunidadRepository.findByUsuarioIdAndComunidadId(userId, communityId))
                .thenReturn(Optional.of(miembro));
        when(miembroComunidadRepository.countByComunidadId(communityId)).thenReturn(3L);
        when(asistenciaEventoRepository.countActiveEventAttendances(eq(userId), eq(communityId), any()))
                .thenReturn(0L);
        when(comunidadClassroomRepository.findByComunidadId(communityId))
                .thenReturn(Optional.of(vinculacion));
        when(miembroComunidadRepository.findByComunidadIdAndRol(communityId, RolComunidad.ADMIN))
                .thenReturn(java.util.List.of(adminMember));

        memberService.leaveCommunity(userId, communityId);

        verify(googleClassroomService).eliminarEstudiante(admin, "course-3", usuario.getEmail());
        verify(miembroComunidadRepository).delete(miembro);
    }

    @Test
    void expelMemberShouldUnsyncTeacherAndDeleteFutureAttendances() {
        Long adminUserId = 1L;
        Long targetUserId = 24L;
        Long communityId = 34L;
        Usuario target = buildUsuario(targetUserId);
        target.setEsTutor(true);
        Comunidad comunidad = buildComunidad(communityId, TipoGrupo.COMUNIDAD_PUBLICA);
        MiembroComunidad targetMember =
                MiembroComunidad.builder().usuario(target).comunidad(comunidad).rol(RolComunidad.ALUMNO).build();
        ComunidadClassroom vinculacion =
                ComunidadClassroom.builder()
                        .comunidad(comunidad)
                        .classroomCourseId("course-4")
                        .classroomCourseName("Course 4")
                        .activa(true)
                        .build();
        Usuario admin = buildUsuario(103L);
        MiembroComunidad adminMember =
                MiembroComunidad.builder().usuario(admin).comunidad(comunidad).rol(RolComunidad.ADMIN).build();

        when(authorizationService.isAdminOf(adminUserId, communityId)).thenReturn(true);
        when(miembroComunidadRepository.findByUsuarioIdAndComunidadId(targetUserId, communityId))
                .thenReturn(Optional.of(targetMember));
        when(comunidadClassroomRepository.findByComunidadId(communityId))
                .thenReturn(Optional.of(vinculacion));
        when(miembroComunidadRepository.findByComunidadIdAndRol(communityId, RolComunidad.ADMIN))
                .thenReturn(java.util.List.of(adminMember));

        memberService.expelMember(adminUserId, communityId, targetUserId);

        verify(googleClassroomService).eliminarProfesor(admin, "course-4", target.getEmail());
        verify(asistenciaEventoRepository)
                .deleteFutureAttendancesInCommunity(eq(targetUserId), eq(communityId), any());
        verify(miembroComunidadRepository).delete(targetMember);
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
