package es.us.meerkat.backend.service.communities;

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

import es.us.meerkat.backend.entity.communities.Comunidad;
import es.us.meerkat.backend.entity.communities.MiembroComunidad;
import es.us.meerkat.backend.entity.communities.RolComunidad;
import es.us.meerkat.backend.entity.communities.SolicitudComunidad;
import es.us.meerkat.backend.entity.communities.TipoGrupo;
import es.us.meerkat.backend.entity.notifications.PreferenciasNotificacion;
import es.us.meerkat.backend.entity.tutors.EstadoSolicitud;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.exception.ValidationException;
import es.us.meerkat.backend.repository.communities.ComunidadRepository;
import es.us.meerkat.backend.repository.communities.MiembroComunidadRepository;
import es.us.meerkat.backend.repository.communities.SolicitudComunidadRepository;
import es.us.meerkat.backend.repository.users.UsuarioRepository;
import es.us.meerkat.backend.service.emails.EmailService;
import es.us.meerkat.backend.service.notifications.PreferenciasNotificacionService;

@ExtendWith(MockitoExtension.class)
class RequestServiceTest {

    @Mock private SolicitudComunidadRepository solicitudComunidadRepository;
    @Mock private ComunidadRepository comunidadRepository;
    @Mock private MiembroComunidadRepository miembroComunidadRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AuthorizationService authorizationService;
    @Mock private CommunityService communityService;
    @Mock private PreferenciasNotificacionService preferenciasNotificacionService;
    @Mock private EmailService emailService;

    @InjectMocks private RequestService requestService;

    @Test
    void requestAccessShouldCreatePendingRequestForPrivateCommunity() {
        Long userId = 1L;
        Long communityId = 10L;

        Usuario usuario = buildUsuario(userId);
        Comunidad comunidad = buildComunidad(communityId, TipoGrupo.GRUPO_PRIVADO);

        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));
        when(comunidadRepository.findById(communityId)).thenReturn(Optional.of(comunidad));
        when(authorizationService.isMemberOf(userId, communityId)).thenReturn(false);
        when(solicitudComunidadRepository.findBySolicitanteIdAndComunidadIdAndEstado(
                        userId, communityId, EstadoSolicitud.PENDIENTE))
                .thenReturn(Optional.empty());
        when(solicitudComunidadRepository.save(any(SolicitudComunidad.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SolicitudComunidad solicitud =
                requestService.requestAccess(userId, communityId, "Quiero entrar", null);

        verify(solicitudComunidadRepository).save(any(SolicitudComunidad.class));
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(solicitud.getMensaje()).isEqualTo("Quiero entrar");
    }

    @Test
    void requestAccessShouldNotifyOwnerWhenSolicitudAccessAndEmailsAreEnabled() {
        Long userId = 1L;
        Long ownerId = 7L;
        Long communityId = 10L;

        Usuario solicitante = buildUsuario(userId);
        Usuario dueno = buildUsuario(ownerId);
        Comunidad comunidad = buildComunidad(communityId, TipoGrupo.GRUPO_PRIVADO);
        comunidad.setCreador(dueno);

        PreferenciasNotificacion preferenciasDueno = new PreferenciasNotificacion();
        preferenciasDueno.setEmailsActivados(true);
        preferenciasDueno.setNotificarSolicitudAcceso(true);

        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(solicitante));
        when(comunidadRepository.findById(communityId)).thenReturn(Optional.of(comunidad));
        when(authorizationService.isMemberOf(userId, communityId)).thenReturn(false);
        when(solicitudComunidadRepository.findBySolicitanteIdAndComunidadIdAndEstado(
                        userId, communityId, EstadoSolicitud.PENDIENTE))
                .thenReturn(Optional.empty());
        when(solicitudComunidadRepository.save(any(SolicitudComunidad.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(preferenciasNotificacionService.getOrCreate(ownerId)).thenReturn(preferenciasDueno);

        requestService.requestAccess(userId, communityId, "Quiero entrar", RolComunidad.ALUMNO);

        verify(emailService)
                .sendCommunityAccessRequestEmail(dueno, comunidad, solicitante, "Quiero entrar");
    }

    @Test
    void requestAccessShouldNotNotifyOwnerWhenSolicitudAccessPreferenceIsDisabled() {
        Long userId = 1L;
        Long ownerId = 7L;
        Long communityId = 10L;

        Usuario solicitante = buildUsuario(userId);
        Usuario dueno = buildUsuario(ownerId);
        Comunidad comunidad = buildComunidad(communityId, TipoGrupo.GRUPO_PRIVADO);
        comunidad.setCreador(dueno);

        PreferenciasNotificacion preferenciasDueno = new PreferenciasNotificacion();
        preferenciasDueno.setEmailsActivados(true);
        preferenciasDueno.setNotificarSolicitudAcceso(false);

        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(solicitante));
        when(comunidadRepository.findById(communityId)).thenReturn(Optional.of(comunidad));
        when(authorizationService.isMemberOf(userId, communityId)).thenReturn(false);
        when(solicitudComunidadRepository.findBySolicitanteIdAndComunidadIdAndEstado(
                        userId, communityId, EstadoSolicitud.PENDIENTE))
                .thenReturn(Optional.empty());
        when(solicitudComunidadRepository.save(any(SolicitudComunidad.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(preferenciasNotificacionService.getOrCreate(ownerId)).thenReturn(preferenciasDueno);

        requestService.requestAccess(userId, communityId, "Quiero entrar", RolComunidad.ALUMNO);

        verify(emailService, never()).sendCommunityAccessRequestEmail(any(), any(), any(), any());
    }

    @Test
    void requestAccessShouldFailForPublicCommunity() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(buildUsuario(1L)));
        when(comunidadRepository.findById(10L))
                .thenReturn(Optional.of(buildComunidad(10L, TipoGrupo.COMUNIDAD_PUBLICA)));

        assertThatThrownBy(() -> requestService.requestAccess(1L, 10L, "hola", RolComunidad.ALUMNO))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("comunidad pública");
    }

    @Test
    void respondToRequestShouldAcceptAndCreateMembershipWhenSpaceAvailable() {
        Long adminId = 99L;
        Long communityId = 10L;
        Long requestId = 500L;

        Usuario admin = buildUsuario(adminId);
        Usuario solicitante = buildUsuario(2L);
        Comunidad comunidad = buildComunidad(communityId, TipoGrupo.GRUPO_PRIVADO);
        SolicitudComunidad solicitud =
                SolicitudComunidad.builder()
                        .id(requestId)
                        .solicitante(solicitante)
                        .comunidad(comunidad)
                        .estado(EstadoSolicitud.PENDIENTE)
                        .build();

        when(authorizationService.isAdminOf(adminId, communityId)).thenReturn(true);
        when(solicitudComunidadRepository.findById(requestId)).thenReturn(Optional.of(solicitud));
        when(usuarioRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(communityService.countMembers(communityId)).thenReturn(2L);
        when(communityService.getMaxMembers(communityId)).thenReturn(50);
        when(solicitudComunidadRepository.save(any(SolicitudComunidad.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SolicitudComunidad responded =
                requestService.respondToRequest(adminId, communityId, requestId, true);

        assertThat(responded.getEstado()).isEqualTo(EstadoSolicitud.ACEPTADA);
        assertThat(responded.getRespondidaPor()).isEqualTo(admin);
        verify(miembroComunidadRepository)
                .save(org.mockito.ArgumentMatchers.any(MiembroComunidad.class));
        verify(solicitudComunidadRepository).save(solicitud);
    }

    @Test
    void respondToRequestShouldRejectRequestWithoutMembershipCreation() {
        Long adminId = 99L;
        Long communityId = 10L;
        Long requestId = 501L;

        SolicitudComunidad solicitud =
                SolicitudComunidad.builder()
                        .id(requestId)
                        .solicitante(buildUsuario(2L))
                        .comunidad(buildComunidad(communityId, TipoGrupo.GRUPO_PRIVADO))
                        .estado(EstadoSolicitud.PENDIENTE)
                        .build();

        when(authorizationService.isAdminOf(adminId, communityId)).thenReturn(true);
        when(solicitudComunidadRepository.findById(requestId)).thenReturn(Optional.of(solicitud));
        when(usuarioRepository.findById(adminId)).thenReturn(Optional.of(buildUsuario(adminId)));
        when(solicitudComunidadRepository.save(any(SolicitudComunidad.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SolicitudComunidad responded =
                requestService.respondToRequest(adminId, communityId, requestId, false);

        assertThat(responded.getEstado()).isEqualTo(EstadoSolicitud.RECHAZADA);
        verify(solicitudComunidadRepository).save(solicitud);
    }

    @Test
    void respondToRequestShouldFailWhenCommunityIsFull() {
        Long adminId = 99L;
        Long communityId = 10L;
        Long requestId = 502L;

        SolicitudComunidad solicitud =
                SolicitudComunidad.builder()
                        .id(requestId)
                        .solicitante(buildUsuario(2L))
                        .comunidad(buildComunidad(communityId, TipoGrupo.GRUPO_PRIVADO))
                        .estado(EstadoSolicitud.PENDIENTE)
                        .build();

        when(authorizationService.isAdminOf(adminId, communityId)).thenReturn(true);
        when(solicitudComunidadRepository.findById(requestId)).thenReturn(Optional.of(solicitud));
        when(usuarioRepository.findById(adminId)).thenReturn(Optional.of(buildUsuario(adminId)));
        when(communityService.countMembers(communityId)).thenReturn(50L);
        when(communityService.getMaxMembers(communityId)).thenReturn(50);

        assertThatThrownBy(
                        () ->
                                requestService.respondToRequest(
                                        adminId, communityId, requestId, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("comunidad está llena");
    }

    @Test
    void respondToRequestShouldFailWhenUserNotAdmin() {
        Long adminId = 1L;
        Long communityId = 10L;
        Long requestId = 1L;

        when(authorizationService.isAdminOf(adminId, communityId)).thenReturn(false);

        assertThatThrownBy(
                        () ->
                                requestService.respondToRequest(
                                        adminId, communityId, requestId, true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void respondToRequestShouldFailWhenRequestNotFound() {
        Long adminId = 99L;
        Long communityId = 10L;
        Long requestId = 999L;

        when(authorizationService.isAdminOf(adminId, communityId)).thenReturn(true);
        when(solicitudComunidadRepository.findById(requestId)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                requestService.respondToRequest(
                                        adminId, communityId, requestId, true))
                .isInstanceOf(IllegalArgumentException.class);
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
