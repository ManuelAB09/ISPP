package es.us.meerkat.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import es.us.meerkat.backend.dto.AccessRequestBody;
import es.us.meerkat.backend.dto.CommunityDetailResponse;
import es.us.meerkat.backend.dto.CreateCommunityRequest;
import es.us.meerkat.backend.dto.MemberResponse;
import es.us.meerkat.backend.dto.PrivacyRequest;
import es.us.meerkat.backend.dto.RequestResponse;
import es.us.meerkat.backend.dto.RespondRequestBody;
import es.us.meerkat.backend.dto.UpgradeCommunityRequest;
import es.us.meerkat.backend.entity.Comunidad;
import es.us.meerkat.backend.entity.EstadoComunidad;
import es.us.meerkat.backend.entity.EstadoSolicitud;
import es.us.meerkat.backend.entity.MiembroComunidad;
import es.us.meerkat.backend.entity.RolComunidad;
import es.us.meerkat.backend.entity.SolicitudComunidad;
import es.us.meerkat.backend.entity.TipoGrupo;
import es.us.meerkat.backend.entity.TipoPlanComunidad;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.service.AuthorizationService;
import es.us.meerkat.backend.service.CategoryService;
import es.us.meerkat.backend.service.CommunityService;
import es.us.meerkat.backend.service.EventoService;
import es.us.meerkat.backend.service.GoogleClassroomService;
import es.us.meerkat.backend.service.MemberService;
import es.us.meerkat.backend.service.RequestService;
import es.us.meerkat.backend.service.TutorContratacionService;

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

    @Test
    void createCommunityShouldReturnUnauthorizedWhenUserIsNull() {
        CreateCommunityRequest request =
                new CreateCommunityRequest(
                        "Comunidad Java", "Descripción", "COMUNIDAD_PUBLICA", null);

        ResponseEntity<CommunityDetailResponse> response =
                (ResponseEntity<CommunityDetailResponse>)
                        communityController.createCommunity(request, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void createCommunityShouldReturnCreatedWhenServiceSucceeds() {
        Usuario usuario = buildUsuario(1L);
        CreateCommunityRequest request =
                new CreateCommunityRequest(
                        "Comunidad Java", "Descripción", "COMUNIDAD_PUBLICA", "img.png");
        Comunidad comunidad =
                buildComunidad(10L, usuario, TipoGrupo.COMUNIDAD_PUBLICA, TipoPlanComunidad.FREE);

        when(communityService.createCommunity(
                        usuario.getId(),
                        request.nombre(),
                        request.descripcion(),
                        TipoGrupo.COMUNIDAD_PUBLICA,
                        request.imagenUrl()))
                .thenReturn(comunidad);
        when(communityService.countMembers(comunidad.getId())).thenReturn(1L);
        when(authorizationService.getUserRoleInCommunityAsString(
                        usuario.getId(), comunidad.getId()))
                .thenReturn("ADMIN");

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
                        "Comunidad Java", "Descripción", "COMUNIDAD_PUBLICA", null);

        when(communityService.createCommunity(
                        usuario.getId(),
                        request.nombre(),
                        request.descripcion(),
                        TipoGrupo.COMUNIDAD_PUBLICA,
                        request.imagenUrl()))
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

        when(memberService.joinPublicCommunity(usuario.getId(), 100L)).thenReturn(miembro);

        @SuppressWarnings("unchecked")
        ResponseEntity<MemberResponse> response =
                (ResponseEntity<MemberResponse>)
                        communityController.joinPublicCommunity(100L, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(50L);
        verify(memberService).joinPublicCommunity(usuario.getId(), 100L);
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

        when(requestService.requestAccess(usuario.getId(), 100L, "Quiero entrar"))
                .thenReturn(solicitud);

        ResponseEntity<RequestResponse> response =
                communityController.requestAccess(
                        100L, new AccessRequestBody("Quiero entrar"), usuario);

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
