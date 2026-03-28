package es.us.meerkat.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import es.us.meerkat.backend.dto.communities.CreateInvitacionRequest;
import es.us.meerkat.backend.dto.communities.InvitacionResponse;
import es.us.meerkat.backend.entity.communities.Comunidad;
import es.us.meerkat.backend.entity.communities.EstadoInvitacion;
import es.us.meerkat.backend.entity.communities.InvitacionMiembro;
import es.us.meerkat.backend.entity.communities.MiembroComunidad;
import es.us.meerkat.backend.entity.communities.RolComunidad;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.communities.ComunidadRepository;
import es.us.meerkat.backend.repository.communities.InvitacionMiembroRepository;
import es.us.meerkat.backend.repository.communities.MiembroComunidadRepository;
import es.us.meerkat.backend.repository.users.UsuarioRepository;
import es.us.meerkat.backend.service.communities.AuthorizationService;
import es.us.meerkat.backend.service.communities.InvitacionMiembroService;
import es.us.meerkat.backend.service.emails.EmailService;

@ExtendWith(MockitoExtension.class)
class InvitacionMiembroServiceTest {

    @Mock private InvitacionMiembroRepository invitacionRepository;
    @Mock private ComunidadRepository comunidadRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private MiembroComunidadRepository miembroCommunityRepository;
    @Mock private AuthorizationService authorizationService;
    @Mock private EmailService emailService;

    @InjectMocks private InvitacionMiembroService invitacionService;

    @Test
    void createInvitacionShouldCreateSuccessfullyWhenAdminInvites() {
        Long userId = 1L;
        Long communityId = 10L;
        String invitedEmail = "newtuser@meerkat.es";

        Usuario admin = buildUsuario(userId);
        Comunidad comunidad = buildComunidad(communityId);
        CreateInvitacionRequest request =
                new CreateInvitacionRequest(invitedEmail, RolComunidad.ALUMNO);

        when(authorizationService.isAdminOf(userId, communityId)).thenReturn(true);
        when(comunidadRepository.findById(communityId)).thenReturn(Optional.of(comunidad));
        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(admin));
        when(invitacionRepository.existsByEmailAndComunidadAndEstado(
                        invitedEmail, comunidad, EstadoInvitacion.PENDIENTE))
                .thenReturn(false);
        when(usuarioRepository.findByEmail(invitedEmail)).thenReturn(Optional.empty());
        when(invitacionRepository.save(any(InvitacionMiembro.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InvitacionMiembro result = invitacionService.createInvitacion(userId, communityId, request);

        assertThat(result.getEmail()).isEqualTo(invitedEmail);
        assertThat(result.getComunidad()).isEqualTo(comunidad);
        assertThat(result.getEstado()).isEqualTo(EstadoInvitacion.PENDIENTE);
        assertThat(result.getRol()).isEqualTo(RolComunidad.ALUMNO);
        assertThat(result.getCodigo()).isNotBlank();
        verify(invitacionRepository).save(result);
        verify(emailService).sendSimpleEmail(any(), any(), any());
    }

    @Test
    void createInvitacionShouldFailWhenUserIsNotAdmin() {
        Long userId = 1L;
        Long communityId = 10L;
        CreateInvitacionRequest request =
                new CreateInvitacionRequest("new@meerkat.es", RolComunidad.ALUMNO);

        when(authorizationService.isAdminOf(userId, communityId)).thenReturn(false);

        assertThatThrownBy(() -> invitacionService.createInvitacion(userId, communityId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("administrador");
    }

    @Test
    void createInvitacionShouldFailWhenEmailHasPendingInvitation() {
        Long userId = 1L;
        Long communityId = 10L;
        String email = "pending@meerkat.es";
        Usuario admin = buildUsuario(userId);
        Comunidad comunidad = buildComunidad(communityId);
        CreateInvitacionRequest request = new CreateInvitacionRequest(email, RolComunidad.ALUMNO);

        when(authorizationService.isAdminOf(userId, communityId)).thenReturn(true);
        when(comunidadRepository.findById(communityId)).thenReturn(Optional.of(comunidad));
        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(admin));
        when(invitacionRepository.existsByEmailAndComunidadAndEstado(
                        email, comunidad, EstadoInvitacion.PENDIENTE))
                .thenReturn(true);

        assertThatThrownBy(() -> invitacionService.createInvitacion(userId, communityId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invitación");
    }

    @Test
    void getInvitacionesByCommunityShouldReturnPagedInvitations() {
        Long userId = 1L;
        Long communityId = 10L;
        Comunidad comunidad = buildComunidad(communityId);
        Usuario admin = buildUsuario(userId);

        InvitacionMiembro invitacion1 = buildInvitacion(1L, "user1@meerkat.es", comunidad, admin);
        InvitacionMiembro invitacion2 = buildInvitacion(2L, "user2@meerkat.es", comunidad, admin);

        Page<InvitacionMiembro> page =
                new PageImpl<>(
                        java.util.List.of(invitacion1, invitacion2), PageRequest.of(0, 10), 2);

        when(authorizationService.isAdminOf(userId, communityId)).thenReturn(true);
        when(comunidadRepository.findById(communityId)).thenReturn(Optional.of(comunidad));
        when(invitacionRepository.findByComunidad(comunidad, page.getPageable())).thenReturn(page);

        Page<InvitacionMiembro> result =
                invitacionService.getInvitacionesByCommunity(
                        userId, communityId, page.getPageable());

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("user1@meerkat.es");
        assertThat(result.getContent().get(1).getEmail()).isEqualTo("user2@meerkat.es");
    }

    @Test
    void getInvitacionByCodigoShouldReturnInvitacion() {
        String codigo = "unique-code-123";
        InvitacionMiembro invitacion =
                buildInvitacion(1L, "test@meerkat.es", buildComunidad(10L), buildUsuario(1L));
        invitacion.setCodigo(codigo);

        when(invitacionRepository.findByCodigo(codigo)).thenReturn(Optional.of(invitacion));

        InvitacionMiembro result = invitacionService.getInvitacionByCodigo(codigo);

        assertThat(result.getCodigo()).isEqualTo(codigo);
        assertThat(result.getEmail()).isEqualTo("test@meerkat.es");
    }

    @Test
    void getInvitacionByCodigoShouldThrowWhenNotFound() {
        String codigo = "invalid-code";
        when(invitacionRepository.findByCodigo(codigo)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invitacionService.getInvitacionByCodigo(codigo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invitación no encontrada");
    }

    @Test
    void aceptarInvitacionShouldAcceptSuccessfullyAndAddToMembership() {
        String codigo = "valid-code";
        Long userId = 5L;
        String invitedEmail = "newmember@meerkat.es";
        Usuario usuario = buildUsuario(userId);
        usuario.setEmail(invitedEmail); // Email must match invitation
        Comunidad comunidad = buildComunidad(10L);
        InvitacionMiembro invitacion =
                buildInvitacion(1L, invitedEmail, comunidad, buildUsuario(1L));
        invitacion.setCodigo(codigo);

        when(invitacionRepository.findByCodigo(codigo)).thenReturn(Optional.of(invitacion));
        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));
        when(invitacionRepository.save(any(InvitacionMiembro.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(miembroCommunityRepository.save(any(MiembroComunidad.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InvitacionMiembro result = invitacionService.aceptarInvitacion(codigo, userId);

        assertThat(result.getEstado()).isEqualTo(EstadoInvitacion.ACEPTADA);
        assertThat(result.getUsuarioAceptador()).isEqualTo(usuario);
        verify(invitacionRepository).save(result);
        verify(miembroCommunityRepository).save(any(MiembroComunidad.class));
    }

    @Test
    void rechazarInvitacionShouldRejectSuccessfully() {
        String codigo = "valid-code";
        InvitacionMiembro invitacion =
                buildInvitacion(1L, "user@meerkat.es", buildComunidad(10L), buildUsuario(1L));
        invitacion.setCodigo(codigo);
        invitacion.setEstado(EstadoInvitacion.PENDIENTE);

        when(invitacionRepository.findByCodigo(codigo)).thenReturn(Optional.of(invitacion));
        when(invitacionRepository.save(any(InvitacionMiembro.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InvitacionMiembro result = invitacionService.rechazarInvitacion(codigo);

        assertThat(result.getEstado()).isEqualTo(EstadoInvitacion.RECHAZADA);
        verify(invitacionRepository).save(result);
    }

    @Test
    void toResponseShouldConvertInvitacionToDTOCorrectly() {
        Usuario invitador = buildUsuario(1L);
        Comunidad comunidad = buildComunidad(10L);
        InvitacionMiembro invitacion = buildInvitacion(1L, "test@meerkat.es", comunidad, invitador);

        InvitacionResponse response = invitacionService.toResponse(invitacion);

        assertThat(response.email()).isEqualTo("test@meerkat.es");
        assertThat(response.rol()).isEqualTo(RolComunidad.ALUMNO);
        assertThat(response.estado()).isEqualTo(EstadoInvitacion.PENDIENTE);
        assertThat(response.usuarioInvitador().nombre()).isEqualTo("Usuario 1");
    }

    private Usuario buildUsuario(final Long id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNombre("Usuario " + id);
        usuario.setEmail("user" + id + "@meerkat.es");
        usuario.setFoto("https://avatar.com/user" + id + ".jpg");
        return usuario;
    }

    private Comunidad buildComunidad(final Long id) {
        return Comunidad.builder()
                .id(id)
                .nombre("Comunidad " + id)
                .descripcion("Descripción")
                .creador(buildUsuario(1L))
                .build();
    }

    private InvitacionMiembro buildInvitacion(
            final Long id, final String email, final Comunidad comunidad, final Usuario invitador) {
        return InvitacionMiembro.builder()
                .id(id)
                .email(email)
                .codigo("code-" + id)
                .comunidad(comunidad)
                .usuarioInvitador(invitador)
                .rol(RolComunidad.ALUMNO)
                .estado(EstadoInvitacion.PENDIENTE)
                .createdAt(LocalDateTime.now())
                .fechaExpiracion(LocalDateTime.now().plusDays(30))
                .build();
    }
}
