package es.us.meerkat.backend.service.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import es.us.meerkat.backend.dto.chats.MessageResponse;
import es.us.meerkat.backend.dto.users.AuthResponse;
import es.us.meerkat.backend.dto.users.ForgotPasswordRequest;
import es.us.meerkat.backend.dto.users.LoginRequest;
import es.us.meerkat.backend.dto.users.RegisterRequest;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.exception.ConflictException;
import es.us.meerkat.backend.exception.EmailNotVerifiedException;
import es.us.meerkat.backend.exception.ValidationException;
import es.us.meerkat.backend.repository.users.UsuarioRepository;
import es.us.meerkat.backend.security.JwtService;
import es.us.meerkat.backend.service.emails.EmailService;
import es.us.meerkat.backend.service.users.AuthService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UsuarioRepository usuarioRepository;

    @Mock private BCryptPasswordEncoder passwordEncoder;

    @Mock private JwtService jwtService;

    @Mock private EmailService emailService;

    @InjectMocks private AuthService authService;

    @Test
    void registrarShouldCreateUserAndReturnMessageResponse() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new.user@meerkat.es");
        request.setPassword("Password123");
        request.setNombre("Nuevo Usuario");

        when(usuarioRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded-password");

        MessageResponse response = authService.registrar(request);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario savedUser = captor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo(request.getEmail());
        assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
        assertThat(savedUser.getNombre()).isEqualTo(request.getNombre());
        assertThat(savedUser.getVisibleEnListados()).isTrue();
        assertThat(savedUser.getEsTutor()).isFalse();
        assertThat(savedUser.getIntereses()).isInstanceOf(ArrayList.class);
        assertThat(savedUser.getEmailVerificado()).isFalse();
        assertThat(savedUser.getVerificationToken()).isNotNull();

        assertThat(response.getMessage()).contains("verificar");
    }

    @Test
    void registrarShouldThrowConflictWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("used@meerkat.es");
        request.setPassword("Password123");
        request.setNombre("Usuario Test");

        when(usuarioRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> authService.registrar(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("El email ya está registrado");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void registrarShouldThrowValidationWhenEmailIsBlank() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("   ");
        request.setPassword("password123");

        assertThatThrownBy(() -> authService.registrar(request))
                .isInstanceOf(ValidationException.class)
                .hasMessage("El email no puede estar vacío");
    }

    @Test
    void registrarShouldThrowValidationWhenPasswordTooShort() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("user@meerkat.es");
        request.setPassword("short");
        request.setNombre("Usuario Test");

        assertThatThrownBy(() -> authService.registrar(request))
                .isInstanceOf(ValidationException.class)
                .hasMessage("La contraseña debe tener al menos 8 caracteres");
    }

    @Test
    void iniciarSesionShouldReturnAuthResponseWhenCredentialsAreValid() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@meerkat.es");
        request.setPassword("password123");

        Usuario usuario = new Usuario();
        usuario.setId(20L);
        usuario.setEmail(request.getEmail());
        usuario.setPassword("encoded-password");
        usuario.setVisibleEnListados(true);
        usuario.setEsTutor(false);
        usuario.setEmailVerificado(true);

        when(usuarioRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(request.getPassword(), usuario.getPassword()))
                .thenReturn(true);
        when(jwtService.generateToken(request.getEmail())).thenReturn("jwt-token");

        AuthResponse response = (AuthResponse) authService.iniciarSesion(request);

        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getId()).isEqualTo(20L);
        assertThat(response.getUser().getEmail()).isEqualTo(request.getEmail());
    }

    @Test
    void iniciarSesionShouldThrowValidationWhenEmailDoesNotExist() {
        LoginRequest request = new LoginRequest();
        request.setEmail("missing@meerkat.es");
        request.setPassword("password123");

        when(usuarioRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.iniciarSesion(request))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Este email no está registrado");
    }

    @Test
    void iniciarSesionShouldThrowValidationWhenPasswordIsIncorrect() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@meerkat.es");
        request.setPassword("wrong-password");

        Usuario usuario = new Usuario();
        usuario.setEmail(request.getEmail());
        usuario.setPassword("encoded-password");

        when(usuarioRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(request.getPassword(), usuario.getPassword()))
                .thenReturn(false);

        assertThatThrownBy(() -> authService.iniciarSesion(request))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Credenciales incorrectas");
    }

    @Test
    void iniciarSesionShouldThrowEmailNotVerifiedWhenEmailNotVerified() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@meerkat.es");
        request.setPassword("password123");

        Usuario usuario = new Usuario();
        usuario.setId(20L);
        usuario.setEmail(request.getEmail());
        usuario.setPassword("encoded-password");
        usuario.setEmailVerificado(false);

        when(usuarioRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(request.getPassword(), usuario.getPassword()))
                .thenReturn(true);

        assertThatThrownBy(() -> authService.iniciarSesion(request))
                .isInstanceOf(EmailNotVerifiedException.class)
                .hasMessageContaining("verificar tu email");
    }

    @Test
    void recuperarContrasenaShouldPersistTemporaryPasswordAndSendEmail() throws Exception {
        ForgotPasswordRequest request =
                ForgotPasswordRequest.builder().email("user@meerkat.es").build();

        Usuario usuario = new Usuario();
        usuario.setEmail(request.getEmail());
        usuario.setNombre("Usuario Reset");

        when(usuarioRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode(any(String.class))).thenReturn("encoded-temp-password");

        MessageResponse response = authService.recuperarContrasena(request);

        verify(usuarioRepository).save(usuario);
        verify(emailService)
                .sendPasswordResetEmail(any(String.class), any(String.class), any(String.class));
        assertThat(usuario.getPassword()).isEqualTo("encoded-temp-password");
        assertThat(response.getMessage())
                .contains("Si el email existe en el sistema, recibirás instrucciones");
    }

    @Test
    void recuperarContrasenaShouldThrowValidationExceptionWhenProcessingFails() {
        ForgotPasswordRequest request =
                ForgotPasswordRequest.builder().email("missing@meerkat.es").build();
        when(usuarioRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.recuperarContrasena(request))
                .isInstanceOf(ValidationException.class)
                .hasMessage("No se pudo enviar el email de recuperación");
    }
}
