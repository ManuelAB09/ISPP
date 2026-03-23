package es.us.meerkat.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import es.us.meerkat.backend.entity.Comunidad;
import es.us.meerkat.backend.entity.EstadoSolicitud;
import es.us.meerkat.backend.entity.MiembroComunidad;
import es.us.meerkat.backend.entity.SolicitudComunidad;
import es.us.meerkat.backend.entity.TipoGrupo;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.ComunidadRepository;
import es.us.meerkat.backend.repository.MiembroComunidadRepository;
import es.us.meerkat.backend.repository.SolicitudComunidadRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class RequestServiceTest {

    @Mock private SolicitudComunidadRepository solicitudComunidadRepository;
    @Mock private ComunidadRepository comunidadRepository;
    @Mock private MiembroComunidadRepository miembroComunidadRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AuthorizationService authorizationService;
    @Mock private CommunityService communityService;

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
    void requestAccessShouldFailForPublicCommunity() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(buildUsuario(1L)));
        when(comunidadRepository.findById(10L))
                .thenReturn(Optional.of(buildComunidad(10L, TipoGrupo.COMUNIDAD_PUBLICA)));

        assertThatThrownBy(() -> requestService.requestAccess(1L, 10L, "hola", null))
                .isInstanceOf(IllegalArgumentException.class)
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
