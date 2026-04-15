package es.us.meerkat.backend.controller.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import es.us.meerkat.backend.dto.chats.MessageResponse;
import es.us.meerkat.backend.dto.users.AuthResponse;
import es.us.meerkat.backend.dto.users.ForgotPasswordRequest;
import es.us.meerkat.backend.dto.users.LoginRequest;
import es.us.meerkat.backend.dto.users.RegisterRequest;
import es.us.meerkat.backend.dto.users.ResendVerificationRequest;
import es.us.meerkat.backend.dto.users.ResetPasswordRequest;
import es.us.meerkat.backend.dto.users.TotpEnableResponse;
import es.us.meerkat.backend.dto.users.TotpSetupResponse;
import es.us.meerkat.backend.dto.users.TotpVerifyRequest;
import es.us.meerkat.backend.dto.users.UserDetailResponse;
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
    void registerShouldReturnConflictWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@meerkat.es");
        request.setPassword("password123");

        when(authService.registrar(request))
                .thenThrow(new IllegalArgumentException("Email already exists"));

        try {
            authController.register(request);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo("Email already exists");
        }
    }

    @Test
    void verifyEmailShouldReturnOkWithAuthResponseWhenTokenIsValid() {
        String token = "valid-token-123";
        AuthResponse serviceResponse =
                AuthResponse.builder()
                        .accessToken("jwt-token")
                        .user(UserDetailResponse.builder().id(1L).email("user@meerkat.es").build())
                        .build();
        when(authService.verificarEmail(token)).thenReturn(serviceResponse);

        ResponseEntity<AuthResponse> response = authController.verifyEmail(token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(serviceResponse);
        verify(authService).verificarEmail(token);
    }

    @Test
    void verifyEmailShouldThrowWhenTokenIsInvalid() {
        String token = "invalid-token";
        when(authService.verificarEmail(token))
                .thenThrow(new IllegalArgumentException("Invalid token"));

        try {
            authController.verifyEmail(token);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo("Invalid token");
        }
    }

    @Test
    void resendVerificationShouldReturnOkWithMessage() {
        ResendVerificationRequest request = new ResendVerificationRequest();
        request.setEmail("user@meerkat.es");

        MessageResponse serviceResponse =
                MessageResponse.builder().message("Email de verificación reenviado").build();
        when(authService.reenviarVerificacion("user@meerkat.es")).thenReturn(serviceResponse);

        ResponseEntity<MessageResponse> response = authController.resendVerification(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(serviceResponse);
        verify(authService).reenviarVerificacion("user@meerkat.es");
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
    void loginShouldThrowWhenCredentialsAreInvalid() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@meerkat.es");
        request.setPassword("wrong");

        when(authService.iniciarSesion(request))
                .thenThrow(new IllegalArgumentException("Invalid credentials"));

        try {
            authController.login(request);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo("Invalid credentials");
        }
    }

    @Test
    void login2faShouldReturnOkWhenCodeIsValid() {
        Map<String, String> body = Map.of("tempToken", "temp-123", "code", "123456");
        AuthResponse serviceResponse =
                AuthResponse.builder()
                        .accessToken("jwt-token")
                        .user(UserDetailResponse.builder().id(3L).email("2fa@meerkat.es").build())
                        .build();
        when(authService.completeLoginWith2fa("temp-123", "123456")).thenReturn(serviceResponse);

        ResponseEntity<AuthResponse> response = authController.login2fa(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(serviceResponse);
        verify(authService).completeLoginWith2fa("temp-123", "123456");
    }

    @Test
    void setup2faShouldReturnOkWithQrCode() {
        TotpSetupResponse serviceResponse =
                new TotpSetupResponse("JBSWY3DPEBLW64TMMQ======", "otpauth://totp/example");
        when(authService.generateTotpSetupForCurrentUser()).thenReturn(serviceResponse);

        ResponseEntity<TotpSetupResponse> response = authController.setup2fa();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(serviceResponse);
        verify(authService).generateTotpSetupForCurrentUser();
    }

    @Test
    void enable2faShouldReturnOkWhenCodeIsValid() {
        TotpVerifyRequest request = new TotpVerifyRequest();
        request.setCode("123456");

        TotpEnableResponse serviceResponse =
                TotpEnableResponse.builder()
                        .message("2FA habilitado correctamente")
                        .backupCodes(java.util.List.of("CODE1", "CODE2"))
                        .build();
        when(authService.enableTotpForCurrentUser("123456")).thenReturn(serviceResponse);

        ResponseEntity<TotpEnableResponse> response = authController.enable2fa(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(serviceResponse);
        verify(authService).enableTotpForCurrentUser("123456");
    }

    @Test
    void disable2faShouldReturnOkWithMessage() {
        TotpVerifyRequest request = new TotpVerifyRequest();
        request.setCode("123456");

        MessageResponse serviceResponse =
                MessageResponse.builder().message("2FA deshabilitado correctamente").build();
        when(authService.disableTotpForCurrentUser("123456")).thenReturn(serviceResponse);

        ResponseEntity<MessageResponse> response = authController.disable2fa(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(serviceResponse);
        verify(authService).disableTotpForCurrentUser("123456");
    }

    @Test
    void authorizeGoogleLoginShouldReturnOkWithUrl() {
        String googleUrl = "https://accounts.google.com/o/oauth2/v2/auth?...";
        when(authService.getGoogleAuthorizeUrl("login")).thenReturn(googleUrl);

        ResponseEntity<java.util.Map<String, String>> response =
                authController.authorizeGoogleLogin();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("url", googleUrl);
        verify(authService).getGoogleAuthorizeUrl("login");
    }

    @Test
    void authorizeGoogleLinkShouldReturnOkWithUrl() {
        String googleUrl = "https://accounts.google.com/o/oauth2/v2/auth?...";
        when(authService.getGoogleAuthorizeUrl("link")).thenReturn(googleUrl);

        ResponseEntity<java.util.Map<String, String>> response =
                authController.authorizeGoogleLink();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("url", googleUrl);
        verify(authService).getGoogleAuthorizeUrl("link");
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

    @Test
    void resetPasswordShouldReturnOkWithServiceMessage() {
        ResetPasswordRequest request =
                ResetPasswordRequest.builder().token("reset-token").newPassword("NewPass1").build();
        MessageResponse serviceResponse =
                MessageResponse.builder()
                        .message("Contraseña restablecida correctamente. Ya puedes iniciar sesión.")
                        .build();
        when(authService.restablecerContrasena(request)).thenReturn(serviceResponse);

        ResponseEntity<MessageResponse> response = authController.resetPassword(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(serviceResponse);
        verify(authService).restablecerContrasena(request);
    }

    @Test
    void resetPasswordShouldThrowWhenTokenIsInvalid() {
        ResetPasswordRequest request =
                ResetPasswordRequest.builder()
                        .token("invalid-token")
                        .newPassword("NewPass1")
                        .build();

        when(authService.restablecerContrasena(request))
                .thenThrow(new IllegalArgumentException("Invalid token"));

        try {
            authController.resetPassword(request);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo("Invalid token");
        }
    }

    @Test
    void unlinkGoogleShouldReturnOkWithMessage() {
        MessageResponse serviceResponse =
                MessageResponse.builder().message("Cuenta Google desvinculada").build();
        when(authService.unlinkGoogleFromCurrentUser()).thenReturn(serviceResponse);

        ResponseEntity<MessageResponse> response = authController.unlinkGoogle();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(serviceResponse);
        verify(authService).unlinkGoogleFromCurrentUser();
    }
}
