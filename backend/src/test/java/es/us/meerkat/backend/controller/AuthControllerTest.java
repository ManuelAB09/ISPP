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

import es.us.meerkat.backend.dto.AuthResponse;
import es.us.meerkat.backend.dto.ForgotPasswordRequest;
import es.us.meerkat.backend.dto.LoginRequest;
import es.us.meerkat.backend.dto.MessageResponse;
import es.us.meerkat.backend.dto.RegisterRequest;
import es.us.meerkat.backend.dto.UserDetailResponse;
import es.us.meerkat.backend.service.users.AuthService;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private AuthService authService;

    @InjectMocks private AuthController authController;

    @Test
    void registerShouldReturnCreatedWithMessageResponse() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new.user@meerkat.es");
        request.setPassword("password123");
        request.setNombre("Nuevo Usuario");

        MessageResponse serviceResponse =
                MessageResponse.builder()
                        .message("Se ha enviado un email de verificación a new.user@meerkat.es")
                        .build();
        when(authService.registrar(request)).thenReturn(serviceResponse);

        ResponseEntity<MessageResponse> response = authController.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(serviceResponse);
        verify(authService).registrar(request);
    }

    @Test
    void loginShouldReturnOkWithAuthResponse() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@meerkat.es");
        request.setPassword("password123");

        AuthResponse serviceResponse =
                AuthResponse.builder()
                        .accessToken("jwt-token")
                        .user(UserDetailResponse.builder().id(2L).email(request.getEmail()).build())
                        .build();
        when(authService.iniciarSesion(request)).thenReturn(serviceResponse);

        ResponseEntity<?> response = authController.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(serviceResponse);
        verify(authService).iniciarSesion(request);
    }

    @Test
    void logoutShouldReturnOkAndConfirmationMessage() {
        ResponseEntity<MessageResponse> response = authController.logout();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Sesión cerrada correctamente");
    }

    @Test
    void forgotPasswordShouldReturnOkWithServiceMessage() {
        ForgotPasswordRequest request =
                ForgotPasswordRequest.builder().email("user@meerkat.es").build();
        MessageResponse serviceResponse =
                MessageResponse.builder()
                        .message("Si el email existe en el sistema, recibirás instrucciones")
                        .build();
        when(authService.recuperarContrasena(request)).thenReturn(serviceResponse);

        ResponseEntity<MessageResponse> response = authController.forgotPassword(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(serviceResponse);
        verify(authService).recuperarContrasena(request);
    }
}
