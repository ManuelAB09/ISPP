package es.us.meerkat.backend.controller.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import es.us.meerkat.backend.dto.users.ChangePasswordRequest;
import es.us.meerkat.backend.dto.users.UpdateUserRequest;
import es.us.meerkat.backend.dto.users.UserActivityResponse;
import es.us.meerkat.backend.dto.users.UserDetailResponse;
import es.us.meerkat.backend.dto.users.UserPublicResponse;
import es.us.meerkat.backend.dto.users.VisibilityRequest;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.events.AsistenciaEventoRepository;
import es.us.meerkat.backend.repository.forms.CuestionarioIntentoRepository;
import es.us.meerkat.backend.repository.recommendations.FeedbackRepository;
import es.us.meerkat.backend.service.users.UsuarioService;

@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    @Mock private UsuarioService usuarioService;
    @Mock private CuestionarioIntentoRepository intentoRepository;
    @Mock private AsistenciaEventoRepository asistenciaEventoRepository;
    @Mock private FeedbackRepository feedbackRepository;
    @Mock private MultipartFile multipartFile;

    @InjectMocks private UsuarioController usuarioController;

    @Test
    void getMeShouldReturnUnauthorizedWhenUsuarioIsNull() {
        ResponseEntity<UserDetailResponse> response = usuarioController.getMe(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void getMeShouldReturnOkWithProfileWhenAuthenticated() {
        Usuario usuario = new Usuario();
        usuario.setEmail("user@meerkat.es");

        UserDetailResponse detail =
                UserDetailResponse.builder().id(10L).email(usuario.getEmail()).build();
        when(usuarioService.obtenerPerfilPropio(usuario)).thenReturn(detail);

        ResponseEntity<UserDetailResponse> response = usuarioController.getMe(usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(detail);
        verify(usuarioService).obtenerPerfilPropio(usuario);
    }

    @Test
    void updateMeShouldReturnUpdatedProfile() {
        Usuario usuario = new Usuario();
        UpdateUserRequest request = new UpdateUserRequest();
        request.setNombre("Nombre actualizado");

        UserDetailResponse updated =
                UserDetailResponse.builder().id(11L).nombre("Nombre actualizado").build();
        when(usuarioService.actualizarPerfil(usuario, request)).thenReturn(updated);

        ResponseEntity<UserDetailResponse> response = usuarioController.updateMe(usuario, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(updated);
        verify(usuarioService).actualizarPerfil(usuario, request);
    }

    @Test
    void uploadProfilePhotoShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<UserDetailResponse> response =
                usuarioController.uploadProfilePhoto(null, multipartFile);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void uploadProfilePhotoShouldReturnOkWithUpdatedProfile() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        UserDetailResponse updated =
                UserDetailResponse.builder().id(1L).foto("path/to/photo.jpg").build();
        when(usuarioService.actualizarFotoPerfil(usuario, multipartFile)).thenReturn(updated);

        ResponseEntity<UserDetailResponse> response =
                usuarioController.uploadProfilePhoto(usuario, multipartFile);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(updated);
        verify(usuarioService).actualizarFotoPerfil(usuario, multipartFile);
    }

    @Test
    void uploadProfilePhotoShouldReturnBadRequestWhenFileIsInvalid() {
        Usuario usuario = new Usuario();
        doThrow(new IllegalArgumentException("Invalid file"))
                .when(usuarioService)
                .actualizarFotoPerfil(usuario, multipartFile);

        ResponseEntity<UserDetailResponse> response =
                usuarioController.uploadProfilePhoto(usuario, multipartFile);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void uploadProfilePhotoShouldReturnInternalServerErrorWhenServiceThrows() {
        Usuario usuario = new Usuario();
        doThrow(new RuntimeException("Service error"))
                .when(usuarioService)
                .actualizarFotoPerfil(usuario, multipartFile);

        ResponseEntity<UserDetailResponse> response =
                usuarioController.uploadProfilePhoto(usuario, multipartFile);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void deleteMeShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<Void> response = usuarioController.deleteMe(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void deleteMeShouldReturnNoContentAndCallService() {
        Usuario usuario = new Usuario();

        ResponseEntity<Void> response = usuarioController.deleteMe(usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(usuarioService).eliminarCuenta(usuario);
    }

    @Test
    void changePasswordShouldReturnSuccessMessage() {
        Usuario usuario = new Usuario();
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword");
        request.setNewPassword("newPassword123");

        ResponseEntity<es.us.meerkat.backend.dto.chats.MessageResponse> response =
                usuarioController.changePassword(usuario, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage())
                .isEqualTo("Contraseña actualizada correctamente");
        verify(usuarioService).cambiarPassword(usuario, request);
    }

    @Test
    void updateVisibilityShouldReturnUpdatedProfile() {
        Usuario usuario = new Usuario();
        VisibilityRequest request = new VisibilityRequest();
        request.setVisibleEnListados(Boolean.FALSE);

        UserDetailResponse updated =
                UserDetailResponse.builder().id(12L).visibleEnListados(false).build();
        when(usuarioService.actualizarVisibilidad(usuario, request)).thenReturn(updated);

        ResponseEntity<UserDetailResponse> response =
                usuarioController.updateVisibility(usuario, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(updated);
        verify(usuarioService).actualizarVisibilidad(usuario, request);
    }

    @Test
    void getUserByIdShouldReturnPublicProfile() {
        Long userId = 15L;
        UserPublicResponse publicResponse =
                UserPublicResponse.builder().id(userId).nombre("Perfil Público").build();
        when(usuarioService.obtenerPerfilPublico(userId)).thenReturn(publicResponse);

        ResponseEntity<UserPublicResponse> response = usuarioController.getUserById(userId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(publicResponse);
        verify(usuarioService).obtenerPerfilPublico(userId);
    }

    @Test
    void getUserByIdShouldThrowWhenUserNotFound() {
        Long userId = 999L;
        when(usuarioService.obtenerPerfilPublico(userId))
                .thenThrow(new IllegalArgumentException("User not found"));

        try {
            usuarioController.getUserById(userId);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo("User not found");
        }
    }

    @Test
    void getMyActivityShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<UserActivityResponse> response = usuarioController.getMyActivity(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getMyActivityShouldReturnOkWithEmptyActivity() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        when(intentoRepository.findWithCuestionarioByUsuarioId(1L)).thenReturn(new ArrayList<>());
        when(asistenciaEventoRepository.findConfirmadasByUsuarioId(1L))
                .thenReturn(new ArrayList<>());
        when(feedbackRepository.findByAlumnoId(any(), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());

        ResponseEntity<UserActivityResponse> response = usuarioController.getMyActivity(usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void getProfileAvatarsShouldReturnOkWithList() {
        List<String> avatars =
                List.of("/avatars/avatar1.png", "/avatars/avatar2.png", "/avatars/avatar3.png");
        when(usuarioService.obtenerAvataresPerfilDisponibles()).thenReturn(avatars);

        ResponseEntity<List<String>> response = usuarioController.getProfileAvatars();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(avatars);
        assertThat(response.getBody()).hasSize(3);
        verify(usuarioService).obtenerAvataresPerfilDisponibles();
    }
}
