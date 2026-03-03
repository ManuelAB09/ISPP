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

import es.us.meerkat.backend.dto.ChangePasswordRequest;
import es.us.meerkat.backend.dto.UpdateUserRequest;
import es.us.meerkat.backend.dto.UserDetailResponse;
import es.us.meerkat.backend.dto.UserPublicResponse;
import es.us.meerkat.backend.dto.VisibilityRequest;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.service.UsuarioService;

@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    @Mock private UsuarioService usuarioService;

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

        ResponseEntity<es.us.meerkat.backend.dto.MessageResponse> response =
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
}
