package es.us.meerkat.backend.controller.communities;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import es.us.meerkat.backend.dto.communities.CategoryListResponse;
import es.us.meerkat.backend.dto.communities.CategoryResponse;
import es.us.meerkat.backend.dto.communities.CommunityDetailResponse;
import es.us.meerkat.backend.dto.communities.CommunityListResponse;
import es.us.meerkat.backend.dto.communities.CommunityRankingEntryResponse;
import es.us.meerkat.backend.dto.communities.CreateCategoryRequest;
import es.us.meerkat.backend.dto.communities.CreateCommunityRequest;
import es.us.meerkat.backend.dto.communities.JoinCommunityRequest;
import es.us.meerkat.backend.dto.communities.MemberListResponse;
import es.us.meerkat.backend.dto.communities.MemberResponse;
import es.us.meerkat.backend.dto.communities.ReorderCategoriesRequest;
import es.us.meerkat.backend.dto.communities.TransferAdminRequest;
import es.us.meerkat.backend.dto.communities.UpdateCategoryRequest;
import es.us.meerkat.backend.dto.communities.UpdateCommunityRequest;
import es.us.meerkat.backend.dto.communities.UpgradeCommunityRequest;
import es.us.meerkat.backend.dto.events.CreateEventRequest;
import es.us.meerkat.backend.dto.events.EventSummaryResponse;
import es.us.meerkat.backend.dto.google.LinkClassroomRequest;
import es.us.meerkat.backend.dto.users.AccessRequestBody;
import es.us.meerkat.backend.dto.users.PrivacyRequest;
import es.us.meerkat.backend.dto.users.RequestResponse;
import es.us.meerkat.backend.dto.users.RespondRequestBody;
import es.us.meerkat.backend.entity.communities.Categoria;
import es.us.meerkat.backend.entity.communities.Comunidad;
import es.us.meerkat.backend.entity.communities.EstadoComunidad;
import es.us.meerkat.backend.entity.communities.MiembroComunidad;
import es.us.meerkat.backend.entity.communities.RolComunidad;
import es.us.meerkat.backend.entity.communities.SolicitudComunidad;
import es.us.meerkat.backend.entity.communities.TipoGrupo;
import es.us.meerkat.backend.entity.events.Evento;
import es.us.meerkat.backend.entity.google.ComunidadClassroom;
import es.us.meerkat.backend.entity.subscriptions.TipoPlanComunidad;
import es.us.meerkat.backend.entity.tutors.EstadoSolicitud;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.service.communities.AuthorizationService;
import es.us.meerkat.backend.service.communities.CategoryService;
import es.us.meerkat.backend.service.communities.CommunityService;
import es.us.meerkat.backend.service.communities.MemberService;
import es.us.meerkat.backend.service.communities.RequestService;
import es.us.meerkat.backend.service.events.EventoService;
import es.us.meerkat.backend.service.google.GoogleClassroomService;
import es.us.meerkat.backend.service.tutors.TutorContratacionService;

@ExtendWith(MockitoExtension.class)
class CommunityControllerTest {

    @Mock private CommunityService communityService;
    @Mock private MemberService memberService;
    @Mock private RequestService requestService;
    @Mock private CategoryService categoryService;
    @Mock private AuthorizationService authorizationService;
    @Mock private TutorContratacionService tutorContratacionService;
    @Mock private EventoService eventoService;
    @Mock private GoogleClassroomService googleClassroomService;

    @InjectMocks private CommunityController communityController;

    private Usuario usuario;
    private Comunidad comunidad;

    @BeforeEach
    void setUp() {
        usuario = buildUsuario(1L);
        comunidad =
                buildComunidad(100L, usuario, TipoGrupo.COMUNIDAD_PUBLICA, TipoPlanComunidad.FREE);
    }

    @SuppressWarnings("unchecked")
    @Test
    void createCommunityShouldReturnUnauthorizedWhenUserIsNull() {
        CreateCommunityRequest request =
                new CreateCommunityRequest(
                        "Comunidad Java",
                        "Descripción",
                        "COMUNIDAD_PUBLICA",
                        null,
                        null,
                        null,
                        null);

        ResponseEntity<CommunityDetailResponse> response =
                (ResponseEntity<CommunityDetailResponse>)
                        communityController.createCommunity(request, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @SuppressWarnings("unchecked")
    @Test
    void createCommunityShouldReturnCreatedWhenServiceSucceeds() {
        Usuario usuario = buildUsuario(1L);
        CreateCommunityRequest request =
                new CreateCommunityRequest(
                        "Comunidad Java",
                        "Descripción",
                        "COMUNIDAD_PUBLICA",
                        "img.png",
                        null,
                        null,
                        null);
        Comunidad comunidad =
                buildComunidad(10L, usuario, TipoGrupo.COMUNIDAD_PUBLICA, TipoPlanComunidad.FREE);

        when(communityService.createCommunity(
                        usuario.getId(),
                        request.nombre(),
                        request.descripcion(),
                        TipoGrupo.COMUNIDAD_PUBLICA,
                        request.imagenUrl(),
                        request.institutionId(),
                        request.maxMiembros(),
                        null))
                .thenReturn(comunidad);
        when(communityService.countMembers(comunidad.getId())).thenReturn(1L);
        MiembroComunidad membership = MiembroComunidad.builder().rol(RolComunidad.ADMIN).build();
        when(authorizationService.getMembership(usuario.getId(), comunidad.getId()))
                .thenReturn(membership);

        ResponseEntity<CommunityDetailResponse> response =
                (ResponseEntity<CommunityDetailResponse>)
                        communityController.createCommunity(request, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(comunidad.getId());
        assertThat(response.getBody().miRol()).isEqualTo("ADMIN");
    }

    @Test
    void createCommunityShouldReturnBadRequestWhenServiceFails() {
        Usuario usuario = buildUsuario(1L);
        CreateCommunityRequest request =
                new CreateCommunityRequest(
                        "Comunidad Java",
                        "Descripción",
                        "COMUNIDAD_PUBLICA",
                        null,
                        null,
                        null,
                        null);

        when(communityService.createCommunity(
                        usuario.getId(),
                        request.nombre(),
                        request.descripcion(),
                        TipoGrupo.COMUNIDAD_PUBLICA,
                        request.imagenUrl(),
                        request.institutionId(),
                        request.maxMiembros(),
                        null))
                .thenThrow(new IllegalArgumentException("límite alcanzado"));

        ResponseEntity<?> response = communityController.createCommunity(request, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void updatePrivacyShouldReturnForbiddenWhenUserIsNotAdmin() {
        Usuario usuario = buildUsuario(1L);
        when(authorizationService.isAdminOf(usuario.getId(), 100L)).thenReturn(false);

        ResponseEntity<CommunityDetailResponse> response =
                communityController.updateCommunityPrivacy(
                        100L, new PrivacyRequest("GRUPO_PRIVADO"), usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void joinPublicCommunityShouldReturnCreatedWhenServiceSucceeds() {
        Usuario usuario = buildUsuario(1L);
        MiembroComunidad miembro =
                MiembroComunidad.builder()
                        .id(50L)
                        .usuario(usuario)
                        .rol(RolComunidad.ALUMNO)
                        .build();

        when(memberService.joinPublicCommunity(usuario.getId(), 100L, null)).thenReturn(miembro);

        @SuppressWarnings("unchecked")
        ResponseEntity<MemberResponse> response =
                (ResponseEntity<MemberResponse>)
                        communityController.joinPublicCommunity(
                                100L, new JoinCommunityRequest(null), usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(50L);
        verify(memberService).joinPublicCommunity(usuario.getId(), 100L, null);
    }

    @Test
    void requestAccessShouldReturnCreatedWhenServiceSucceeds() {
        Usuario usuario = buildUsuario(2L);
        SolicitudComunidad solicitud =
                SolicitudComunidad.builder()
                        .id(77L)
                        .solicitante(usuario)
                        .estado(EstadoSolicitud.PENDIENTE)
                        .mensaje("Quiero entrar")
                        .build();

        when(requestService.requestAccess(
                        usuario.getId(), 100L, "Quiero entrar", RolComunidad.ALUMNO))
                .thenReturn(solicitud);

        ResponseEntity<RequestResponse> response =
                communityController.requestAccess(
                        100L, new AccessRequestBody("Quiero entrar", null), usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(77L);
    }

    @Test
    void respondToRequestShouldReturnForbiddenWhenUserIsNotAdmin() {
        Usuario usuario = buildUsuario(1L);
        when(authorizationService.isAdminOf(usuario.getId(), 100L)).thenReturn(false);

        ResponseEntity<RequestResponse> response =
                communityController.respondToRequest(
                        100L, 200L, new RespondRequestBody(true), usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void upgradeCommunityShouldReturnBadRequestWhenAlreadyPremium() {
        Usuario usuario = buildUsuario(1L);
        when(authorizationService.isAdminOf(usuario.getId(), 100L)).thenReturn(true);
        when(communityService.upgradeToPremium(usuario.getId(), 100L))
                .thenThrow(new IllegalArgumentException("ya premium"));

        ResponseEntity<CommunityDetailResponse> response =
                communityController.upgradeCommunity(
                        100L, new UpgradeCommunityRequest("premium"), usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void promoteMemberToAdminShouldReturnForbiddenWhenUserIsNotAdmin() {
        Usuario usuario = buildUsuario(1L);
        when(authorizationService.isAdminOf(usuario.getId(), 100L)).thenReturn(false);

        ResponseEntity<MemberResponse> response =
                communityController.promoteMemberToAdmin(100L, 2L, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void promoteMemberToAdminShouldReturnOkWhenServiceSucceeds() {
        Usuario usuario = buildUsuario(1L);
        Usuario targetUser = buildUsuario(2L);
        MiembroComunidad promoted =
                MiembroComunidad.builder()
                        .id(90L)
                        .usuario(targetUser)
                        .rol(RolComunidad.ADMIN)
                        .build();

        when(authorizationService.isAdminOf(usuario.getId(), 100L)).thenReturn(true);
        when(memberService.promoteToAdmin(usuario.getId(), 100L, 2L)).thenReturn(promoted);

        ResponseEntity<MemberResponse> response =
                communityController.promoteMemberToAdmin(100L, 2L, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(90L);
        assertThat(response.getBody().rol()).isEqualTo("ADMIN");
    }

    @Test
    void createEventShouldForwardPrivateFlagFromRequest() {
        Usuario usuario = buildUsuario(1L);
        CreateEventRequest request = new CreateEventRequest();
        request.setTitulo("Evento privado");
        request.setPrivado(true);

        Evento evento = mock(Evento.class);
        when(eventoService.crearEvento(
                        eq(usuario.getId()),
                        eq(100L),
                        eq(request.getTitulo()),
                        eq(request.getDescripcion()),
                        eq(request.getFechaHora()),
                        eq(request.getFechaFin()),
                        eq(request.getAforo()),
                        eq(request.getQueLlevar()),
                        eq(request.getEsVirtual()),
                        eq(true),
                        eq(request.getEnlaceVirtual()),
                        eq(request.getVisibleEnMapa()),
                        eq(request.getUbicacionId())))
                .thenReturn(evento);

        ResponseEntity<?> response = communityController.createEvent(100L, request, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(eventoService)
                .crearEvento(
                        eq(usuario.getId()),
                        eq(100L),
                        eq(request.getTitulo()),
                        eq(request.getDescripcion()),
                        eq(request.getFechaHora()),
                        eq(request.getFechaFin()),
                        eq(request.getAforo()),
                        eq(request.getQueLlevar()),
                        eq(request.getEsVirtual()),
                        eq(true),
                        eq(request.getEnlaceVirtual()),
                        eq(request.getVisibleEnMapa()),
                        eq(request.getUbicacionId()));
    }

    @Test
    void createCommunityShouldReturnUnauthorizedWhenUserIsNull2() {
        CreateCommunityRequest request =
                new CreateCommunityRequest(
                        "Test", "Desc", "COMUNIDAD_PRIVADA", null, null, null, null);

        ResponseEntity<?> response = communityController.createCommunity(request, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void joinPublicCommunityShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> response =
                communityController.joinPublicCommunity(100L, new JoinCommunityRequest(null), null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void requestAccessShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> response =
                communityController.requestAccess(100L, new AccessRequestBody("test", null), null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void respondToRequestShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> response =
                communityController.respondToRequest(
                        100L, 200L, new RespondRequestBody(true), null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void upgradeCommunityShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> response =
                communityController.upgradeCommunity(
                        100L, new UpgradeCommunityRequest("premium"), null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void upgradeCommunityShouldReturnForbiddenWhenUserIsNotAdmin() {
        Usuario usuario = buildUsuario(1L);
        when(authorizationService.isAdminOf(usuario.getId(), 100L)).thenReturn(false);

        ResponseEntity<?> response =
                communityController.upgradeCommunity(
                        100L, new UpgradeCommunityRequest("premium"), usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void promoteMemberToAdminShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> response = communityController.promoteMemberToAdmin(100L, 2L, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void createEventShouldReturnUnauthorizedWhenUserIsNull() {
        CreateEventRequest request = new CreateEventRequest();
        request.setTitulo("Test");

        ResponseEntity<?> response = communityController.createEvent(100L, request, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void listCommunitiesShouldReturnOkWithContent() {
        Usuario usuario = buildUsuario(1L);
        Comunidad comunidad =
                buildComunidad(10L, usuario, TipoGrupo.COMUNIDAD_PUBLICA, TipoPlanComunidad.FREE);
        Page<Comunidad> page = new PageImpl<>(List.of(comunidad), PageRequest.of(0, 20), 1);

        when(communityService.listActiveCommunities(any(), any(), any(), any(), any(), any()))
                .thenReturn(page);
        when(communityService.countMembers(comunidad.getId())).thenReturn(3L);
        when(authorizationService.getMembership(usuario.getId(), comunidad.getId()))
                .thenReturn(MiembroComunidad.builder().rol(RolComunidad.ALUMNO).build());
        when(googleClassroomService.getVinculacion(comunidad.getId())).thenReturn(Optional.empty());

        ResponseEntity<CommunityListResponse> response =
                communityController.listCommunities(null, null, null, null, null, 0, 20, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().content()).hasSize(1);
        assertThat(response.getBody().content().get(0).id()).isEqualTo(comunidad.getId());
    }

    @Test
    void listMyCommunitiesShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> response = communityController.listMyCommunities(0, 20, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void listMyCommunitiesShouldReturnOkWhenMembershipsExist() {
        Usuario usuario = buildUsuario(2L);
        Comunidad comunidad =
                buildComunidad(12L, usuario, TipoGrupo.COMUNIDAD_PUBLICA, TipoPlanComunidad.FREE);
        MiembroComunidad membership =
                MiembroComunidad.builder().usuario(usuario).comunidad(comunidad).build();
        Page<MiembroComunidad> page = new PageImpl<>(List.of(membership));

        when(memberService.listUserMemberships(eq(usuario.getId()), any())).thenReturn(page);
        when(communityService.countMembers(comunidad.getId())).thenReturn(1L);
        when(authorizationService.getMembership(usuario.getId(), comunidad.getId()))
                .thenReturn(membership);
        when(googleClassroomService.getVinculacion(comunidad.getId())).thenReturn(Optional.empty());

        ResponseEntity<CommunityListResponse> response =
                communityController.listMyCommunities(0, 20, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().content()).hasSize(1);
    }

    @Test
    void updateCommunityShouldReturnOkWhenAdmin() {
        Usuario usuario = buildUsuario(3L);
        Comunidad comunidad =
                buildComunidad(30L, usuario, TipoGrupo.COMUNIDAD_PUBLICA, TipoPlanComunidad.FREE);

        when(authorizationService.isAdminOf(usuario.getId(), comunidad.getId())).thenReturn(true);
        when(communityService.updateCommunity(
                        usuario.getId(), comunidad.getId(), "Nuevo", "Desc", "img.png"))
                .thenReturn(comunidad);
        when(communityService.countMembers(comunidad.getId())).thenReturn(1L);
        when(authorizationService.getMembership(usuario.getId(), comunidad.getId()))
                .thenReturn(MiembroComunidad.builder().rol(RolComunidad.ADMIN).build());
        when(googleClassroomService.getVinculacion(comunidad.getId())).thenReturn(Optional.empty());

        ResponseEntity<CommunityDetailResponse> response =
                communityController.updateCommunity(
                        comunidad.getId(),
                        new UpdateCommunityRequest("Nuevo", "Desc", "img.png"),
                        usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void deleteCommunityShouldReturnNoContentWhenAdmin() {
        Usuario usuario = buildUsuario(4L);
        when(authorizationService.isAdminOf(usuario.getId(), 40L)).thenReturn(true);

        ResponseEntity<Void> response = communityController.deleteCommunity(40L, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(communityService).deleteCommunity(usuario.getId(), 40L);
    }

    @Test
    void listCommunityMembersShouldReturnMemberList() {
        Usuario usuario = buildUsuario(5L);
        MiembroComunidad miembro =
                MiembroComunidad.builder().id(1L).usuario(usuario).rol(RolComunidad.ALUMNO).build();
        Page<MiembroComunidad> page = new PageImpl<>(List.of(miembro));

        when(memberService.listMembers(eq(50L), any())).thenReturn(page);

        ResponseEntity<MemberListResponse> response =
                communityController.listCommunityMembers(50L, 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().content()).hasSize(1);
    }

    @Test
    void getMyMembershipShouldReturnNotFoundWhenMissing() {
        Usuario usuario = buildUsuario(6L);
        when(memberService.getMyMembership(usuario.getId(), 60L))
                .thenThrow(new IllegalArgumentException("not member"));

        ResponseEntity<MemberResponse> response = communityController.getMyMembership(60L, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void leaveCommunityShouldReturnConflictWhenOnlyAdmin() {
        Usuario usuario = buildUsuario(7L);
        doThrow(new IllegalStateException("only admin"))
                .when(memberService)
                .leaveCommunity(usuario.getId(), 70L);

        ResponseEntity<Void> response = communityController.leaveCommunity(70L, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void expelMemberShouldReturnNotFoundWhenMemberMissing() {
        Usuario usuario = buildUsuario(8L);
        when(authorizationService.isAdminOf(usuario.getId(), 80L)).thenReturn(true);
        doThrow(new IllegalArgumentException("not found"))
                .when(memberService)
                .expelMember(usuario.getId(), 80L, 9L);

        ResponseEntity<Void> response = communityController.expelMember(80L, 9L, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void transferAdminShouldReturnBadRequestWhenRoleInvalid() {
        Usuario usuario = buildUsuario(9L);
        when(authorizationService.isAdminOf(usuario.getId(), 90L)).thenReturn(true);

        ResponseEntity<MemberResponse> response =
                communityController.transferAdmin(
                        90L, new TransferAdminRequest(2L, "INVALID"), usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getCommunityRankingShouldReturnForbiddenWhenNotMember() {
        Usuario usuario = buildUsuario(10L);
        when(authorizationService.isMemberOf(usuario.getId(), 100L)).thenReturn(false);

        ResponseEntity<List<CommunityRankingEntryResponse>> response =
                communityController.getCommunityRanking(100L, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void addAdminToCommunityShouldReturnBadRequestOnError() {
        Usuario usuario = buildUsuario(11L);
        when(authorizationService.isAdminOf(usuario.getId(), 110L)).thenReturn(true);
        when(memberService.addAdmin(usuario.getId(), 110L, 21L))
                .thenThrow(new IllegalArgumentException("invalid"));

        ResponseEntity<?> response = communityController.addAdminToCommunity(110L, 21L, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void listRequestsShouldReturnForbiddenWhenNotAdmin() {
        Usuario usuario = buildUsuario(12L);
        when(authorizationService.isAdminOf(usuario.getId(), 120L)).thenReturn(false);

        ResponseEntity<?> response = communityController.listRequests(120L, null, 0, 20, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void getMyRequestStatusShouldReturnPendingFlag() {
        Usuario usuario = buildUsuario(13L);
        when(requestService.hasPendingRequest(usuario.getId(), 130L)).thenReturn(true);

        ResponseEntity<java.util.Map<String, Boolean>> response =
                communityController.getMyRequestStatus(130L, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("pending", true);
    }

    @Test
    void createCategoryShouldReturnCreatedWhenAdmin() {
        Usuario usuario = buildUsuario(14L);
        when(authorizationService.isAdminOf(usuario.getId(), 140L)).thenReturn(true);
        Categoria categoria =
                Categoria.builder().id(5L).nombre("Cat").descripcion("Desc").orden(1).build();
        when(categoryService.createCategory(usuario.getId(), 140L, "Cat", "Desc"))
                .thenReturn(categoria);

        ResponseEntity<CategoryResponse> response =
                communityController.createCategory(
                        140L, new CreateCategoryRequest("Cat", "Desc"), usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void updateCategoryShouldReturnNotFoundWhenMissing() {
        Usuario usuario = buildUsuario(15L);
        when(authorizationService.isAdminOf(usuario.getId(), 150L)).thenReturn(true);
        when(categoryService.updateCategory(usuario.getId(), 150L, 7L, "New", "Desc"))
                .thenThrow(new IllegalArgumentException("missing"));

        ResponseEntity<CategoryResponse> response =
                communityController.updateCategory(
                        150L, 7L, new UpdateCategoryRequest("New", "Desc"), usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void reorderCategoriesShouldReturnOkWhenAdmin() {
        Usuario usuario = buildUsuario(16L);
        when(authorizationService.isAdminOf(usuario.getId(), 160L)).thenReturn(true);
        when(categoryService.listCategories(160L))
                .thenReturn(List.of(Categoria.builder().id(1L).nombre("A").build()));

        ResponseEntity<CategoryListResponse> response =
                communityController.reorderCategories(
                        160L, new ReorderCategoriesRequest(List.of(1L)), usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void listCommunityEventsShouldReturnNotFoundWhenCommunityMissing() {
        Usuario usuario = buildUsuario(17L);
        when(communityService.getCommunityById(170L, usuario.getId()))
                .thenThrow(new IllegalArgumentException("missing"));

        ResponseEntity<List<EventSummaryResponse>> response =
                communityController.listCommunityEvents(170L, false, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void linkClassroomShouldReturnOkWhenAdmin() {
        Usuario usuario = buildUsuario(18L);
        Comunidad comunidad =
                buildComunidad(180L, usuario, TipoGrupo.COMUNIDAD_PUBLICA, TipoPlanComunidad.FREE);
        ComunidadClassroom cc =
                ComunidadClassroom.builder()
                        .id(1L)
                        .comunidad(comunidad)
                        .classroomCourseId("c1")
                        .classroomCourseName("Curso")
                        .activa(true)
                        .build();

        when(authorizationService.isAdminOf(usuario.getId(), 180L)).thenReturn(true);
        when(googleClassroomService.vincularCurso(180L, "c1", "Curso")).thenReturn(cc);

        ResponseEntity<?> response =
                communityController.linkClassroom(
                        180L, new LinkClassroomRequest("c1", "Curso"), usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void unlinkClassroomShouldReturnNotFoundWhenMissing() {
        Usuario usuario = buildUsuario(19L);
        when(authorizationService.isAdminOf(usuario.getId(), 190L)).thenReturn(true);
        doThrow(new RuntimeException("missing"))
                .when(googleClassroomService)
                .desvincularCurso(190L);

        ResponseEntity<Void> response = communityController.unlinkClassroom(190L, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getLinkedClassroomShouldReturnNotFoundWhenAbsent() {
        when(googleClassroomService.getVinculacion(200L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = communityController.getLinkedClassroom(200L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private Usuario buildUsuario(final Long id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNombre("Usuario " + id);
        usuario.setEmail("user" + id + "@meerkat.es");
        return usuario;
    }

    private Comunidad buildComunidad(
            final Long id,
            final Usuario creador,
            final TipoGrupo tipoGrupo,
            final TipoPlanComunidad tipoPlan) {
        return Comunidad.builder()
                .id(id)
                .nombre("Comunidad")
                .descripcion("Desc")
                .tipoGrupo(tipoGrupo)
                .tipoPlan(tipoPlan)
                .estado(EstadoComunidad.ACTIVA)
                .maxMiembros(50)
                .creador(creador)
                .build();
    }
}
