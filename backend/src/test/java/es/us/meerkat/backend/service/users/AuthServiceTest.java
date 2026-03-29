package es.us.meerkat.backend.service.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import es.us.meerkat.backend.dto.chats.MessageResponse;
import es.us.meerkat.backend.dto.users.AuthResponse;
import es.us.meerkat.backend.dto.users.ForgotPasswordRequest;
import es.us.meerkat.backend.dto.users.LoginRequest;
import es.us.meerkat.backend.dto.users.RegisterRequest;
import es.us.meerkat.backend.dto.users.TotpSetupResponse;
import es.us.meerkat.backend.dto.users.TwoFactorChallengeResponse;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.exception.ConflictException;
import es.us.meerkat.backend.exception.EmailNotVerifiedException;
import es.us.meerkat.backend.exception.ValidationException;
import es.us.meerkat.backend.repository.users.UsuarioRepository;
import es.us.meerkat.backend.security.JwtService;
import es.us.meerkat.backend.service.emails.EmailService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UsuarioRepository usuarioRepository;

    @Mock private BCryptPasswordEncoder passwordEncoder;

    @Mock private JwtService jwtService;

    @Mock private EmailService emailService;

    @InjectMocks private AuthService authService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setSecurityContext(Usuario usuario) {
        SecurityContext ctx = org.mockito.Mockito.mock(SecurityContext.class);
        Authentication auth = org.mockito.Mockito.mock(Authentication.class);
        org.mockito.Mockito.lenient().when(auth.isAuthenticated()).thenReturn(true);
        org.mockito.Mockito.lenient().when(auth.getName()).thenReturn(usuario.getEmail());
        org.mockito.Mockito.lenient().when(auth.getPrincipal()).thenReturn(usuario);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    @SuppressWarnings("unchecked")
    private void putTempLogin(Long userId, Instant expiresAt) {
        Map<String, Object> store =
                (Map<String, Object>) ReflectionTestUtils.getField(authService, "tempLoginStore");
        try {
            Class<?> tlClass =
                    Class.forName("es.us.meerkat.backend.service.users.AuthService$TempLogin");
            var ctor = tlClass.getDeclaredConstructors()[0];
            ctor.setAccessible(true);
            Object tl = ctor.newInstance(userId, expiresAt);
            store.put("temp-token-123", tl);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

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

    // ── registrar validation branches ─────────────────────────────────────

    @Test
    void registrarShouldThrowWhenEmailFormatInvalid() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("not-an-email");
        request.setPassword("Password123");
        request.setNombre("Test");

        assertThatThrownBy(() -> authService.registrar(request))
                .isInstanceOf(ValidationException.class)
                .hasMessage("El formato del email no es válido");
    }

    @Test
    void registrarShouldThrowWhenNombreIsBlank() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("valid@meerkat.es");
        request.setPassword("Password123");
        request.setNombre("   ");

        assertThatThrownBy(() -> authService.registrar(request))
                .isInstanceOf(ValidationException.class)
                .hasMessage("El nombre no puede estar vacío");
    }

    @Test
    void registrarShouldThrowWhenNombreIsNull() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("valid@meerkat.es");
        request.setPassword("Password123");
        request.setNombre(null);

        assertThatThrownBy(() -> authService.registrar(request))
                .isInstanceOf(ValidationException.class)
                .hasMessage("El nombre no puede estar vacío");
    }

    @Test
    void registrarShouldThrowWhenPasswordTooLong() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("valid@meerkat.es");
        request.setPassword("A1a" + "x".repeat(126)); // 129 chars
        request.setNombre("Test");

        assertThatThrownBy(() -> authService.registrar(request))
                .isInstanceOf(ValidationException.class)
                .hasMessage("La contraseña no puede tener más de 128 caracteres");
    }

    @Test
    void registrarShouldThrowWhenPasswordMissingComplexity() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("valid@meerkat.es");
        request.setPassword("alllowercase1"); // no uppercase
        request.setNombre("Test");

        assertThatThrownBy(() -> authService.registrar(request))
                .isInstanceOf(ValidationException.class)
                .hasMessage("La contraseña debe contener mayúsculas, minúsculas y números");
    }

    @Test
    void registrarShouldThrowWhenPasswordMissingDigit() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("valid@meerkat.es");
        request.setPassword("NoDigitHere");
        request.setNombre("Test");

        assertThatThrownBy(() -> authService.registrar(request))
                .isInstanceOf(ValidationException.class)
                .hasMessage("La contraseña debe contener mayúsculas, minúsculas y números");
    }

    // ── iniciarSesion 2FA branch ──────────────────────────────────────────

    @Test
    void iniciarSesionShouldReturnTwoFactorChallengeWhen2FAEnabled() {
        LoginRequest request = new LoginRequest();
        request.setEmail("2fa@meerkat.es");
        request.setPassword("password123");

        Usuario usuario = new Usuario();
        usuario.setId(50L);
        usuario.setEmail(request.getEmail());
        usuario.setPassword("encoded-password");
        usuario.setEmailVerificado(true);
        usuario.setAutenticacionDosFactores(true);
        usuario.setTotpSecret("JBSWY3DPEHPK3PXP");
        usuario.setVisibleEnListados(true);
        usuario.setNotificacionesPush(true);

        when(usuarioRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(request.getPassword(), usuario.getPassword()))
                .thenReturn(true);

        Object result = authService.iniciarSesion(request);

        assertThat(result).isInstanceOf(TwoFactorChallengeResponse.class);
        TwoFactorChallengeResponse tfResponse = (TwoFactorChallengeResponse) result;
        assertThat(tfResponse.getTwoFactorRequired()).isTrue();
        assertThat(tfResponse.getTempToken()).isNotNull().isNotBlank();
    }

    @Test
    void iniciarSesionShouldNotTrigger2FAWhenTotpSecretIsBlank() {
        LoginRequest request = new LoginRequest();
        request.setEmail("no2fa@meerkat.es");
        request.setPassword("password123");

        Usuario usuario = new Usuario();
        usuario.setId(51L);
        usuario.setEmail(request.getEmail());
        usuario.setPassword("encoded-password");
        usuario.setEmailVerificado(true);
        usuario.setAutenticacionDosFactores(true);
        usuario.setTotpSecret("   "); // blank secret → skip 2FA
        usuario.setVisibleEnListados(true);

        when(usuarioRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(request.getPassword(), usuario.getPassword()))
                .thenReturn(true);
        when(jwtService.generateToken(request.getEmail())).thenReturn("jwt-token");

        Object result = authService.iniciarSesion(request);

        assertThat(result).isInstanceOf(AuthResponse.class);
    }

    @Test
    void iniciarSesionShouldApplyPreferenceDefaultsWhenNull() {
        LoginRequest request = new LoginRequest();
        request.setEmail("defaults@meerkat.es");
        request.setPassword("password123");

        Usuario usuario = new Usuario();
        usuario.setId(52L);
        usuario.setEmail(request.getEmail());
        usuario.setPassword("encoded-password");
        usuario.setEmailVerificado(true);
        // leave visibleEnListados, autenticacionDosFactores, notificacionesPush null
        usuario.setVisibleEnListados(null);
        usuario.setAutenticacionDosFactores(null);
        usuario.setNotificacionesPush(null);

        when(usuarioRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(request.getPassword(), usuario.getPassword()))
                .thenReturn(true);
        when(jwtService.generateToken(request.getEmail())).thenReturn("jwt-token");

        Object result = authService.iniciarSesion(request);

        // Defaults should have been applied and user saved
        assertThat(usuario.getVisibleEnListados()).isTrue();
        assertThat(usuario.getAutenticacionDosFactores()).isFalse();
        assertThat(usuario.getNotificacionesPush()).isTrue();
        verify(usuarioRepository).save(usuario);
        assertThat(result).isInstanceOf(AuthResponse.class);
    }

    // ── verificarEmail ────────────────────────────────────────────────────

    @Test
    void verificarEmailShouldReturnAuthResponseWhenTokenValid() {
        String token = "valid-token";
        Usuario usuario = new Usuario();
        usuario.setId(60L);
        usuario.setEmail("verify@meerkat.es");
        usuario.setTokenExpiration(LocalDateTime.now().plusHours(1));
        usuario.setVisibleEnListados(true);
        usuario.setEsTutor(false);

        when(usuarioRepository.findByVerificationToken(token)).thenReturn(Optional.of(usuario));
        when(jwtService.generateToken(usuario.getEmail())).thenReturn("jwt-token");

        AuthResponse response = authService.verificarEmail(token);

        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        assertThat(usuario.getEmailVerificado()).isTrue();
        assertThat(usuario.getVerificationToken()).isNull();
        assertThat(usuario.getTokenExpiration()).isNull();
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void verificarEmailShouldThrowWhenTokenIsNull() {
        assertThatThrownBy(() -> authService.verificarEmail(null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Token de verificación inválido");
    }

    @Test
    void verificarEmailShouldThrowWhenTokenIsBlank() {
        assertThatThrownBy(() -> authService.verificarEmail("   "))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Token de verificación inválido");
    }

    @Test
    void verificarEmailShouldThrowWhenTokenNotFound() {
        when(usuarioRepository.findByVerificationToken("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verificarEmail("unknown"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Token de verificación inválido o ya utilizado");
    }

    @Test
    void verificarEmailShouldThrowWhenTokenExpired() {
        String token = "expired-token";
        Usuario usuario = new Usuario();
        usuario.setTokenExpiration(LocalDateTime.now().minusHours(1)); // expired

        when(usuarioRepository.findByVerificationToken(token)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> authService.verificarEmail(token))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("token de verificación ha expirado");
    }

    @Test
    void verificarEmailShouldThrowWhenTokenExpirationIsNull() {
        String token = "no-exp-token";
        Usuario usuario = new Usuario();
        usuario.setTokenExpiration(null);

        when(usuarioRepository.findByVerificationToken(token)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> authService.verificarEmail(token))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("token de verificación ha expirado");
    }

    // ── reenviarVerificacion ──────────────────────────────────────────────

    @Test
    void reenviarVerificacionShouldReturnMessageWhenSuccess() throws Exception {
        String email = "resend@meerkat.es";
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setNombre("User Resend");
        usuario.setEmailVerificado(false);

        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuario));

        MessageResponse response = authService.reenviarVerificacion(email);

        assertThat(response.getMessage()).contains(email);
        assertThat(usuario.getVerificationToken()).isNotNull();
        assertThat(usuario.getTokenExpiration()).isNotNull();
        verify(usuarioRepository).save(usuario);
        verify(emailService)
                .sendVerificationEmail(
                        any(String.class), any(String.class), any(String.class), any(String.class));
    }

    @Test
    void reenviarVerificacionShouldThrowWhenEmailIsBlank() {
        assertThatThrownBy(() -> authService.reenviarVerificacion("   "))
                .isInstanceOf(ValidationException.class)
                .hasMessage("El email no puede estar vacío");
    }

    @Test
    void reenviarVerificacionShouldThrowWhenEmailIsNull() {
        assertThatThrownBy(() -> authService.reenviarVerificacion(null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("El email no puede estar vacío");
    }

    @Test
    void reenviarVerificacionShouldThrowWhenUserNotFound() {
        when(usuarioRepository.findByEmail("unknown@meerkat.es")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.reenviarVerificacion("unknown@meerkat.es"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("No existe una cuenta con este email");
    }

    @Test
    void reenviarVerificacionShouldThrowWhenAlreadyVerified() {
        String email = "verified@meerkat.es";
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setEmailVerificado(true);

        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> authService.reenviarVerificacion(email))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Este email ya ha sido verificado");
    }

    // ── completeLoginWith2fa ─────────────────────────────────────────

    @Test
    void completeLoginWith2faShouldThrowWhenTokenInvalid() {
        assertThatThrownBy(() -> authService.completeLoginWith2fa("bad-token", "123456"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Token temporal inválido o expirado");
    }

    @Test
    void completeLoginWith2faShouldThrowWhenTokenExpired() {
        putTempLogin(1L, Instant.now().minusSeconds(60));

        assertThatThrownBy(() -> authService.completeLoginWith2fa("temp-token-123", "123456"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Token temporal inválido o expirado");
    }

    @Test
    void completeLoginWith2faShouldThrowWhenUserNotFound() {
        putTempLogin(99L, Instant.now().plusSeconds(300));
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.completeLoginWith2fa("temp-token-123", "123456"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Usuario no encontrado");
    }

    @Test
    void completeLoginWith2faShouldThrowWhenNo2faConfigured() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("test@test.es");
        usuario.setTotpSecret(null);

        putTempLogin(1L, Instant.now().plusSeconds(300));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> authService.completeLoginWith2fa("temp-token-123", "000000"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("2FA no configurado para este usuario");
    }

    @Test
    void completeLoginWith2faShouldSucceedWithBackupCode() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("test@test.es");
        usuario.setNombre("Test");
        usuario.setTotpSecret("JBSWY3DPEHPK3PXP");
        usuario.setEmailVerificado(true);
        List<String> hashes = new ArrayList<>(List.of("hash1", "hash2"));
        usuario.setBackupCodeHashes(hashes);

        putTempLogin(1L, Instant.now().plusSeconds(300));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(anyString(), any())).thenReturn(false).thenReturn(true);
        when(jwtService.generateToken("test@test.es")).thenReturn("jwt-token");

        AuthResponse result = authService.completeLoginWith2fa("temp-token-123", "backup-code");

        assertThat(result.getAccessToken()).isEqualTo("jwt-token");
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void completeLoginWith2faShouldThrowWhenCodeInvalid() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("test@test.es");
        usuario.setTotpSecret("JBSWY3DPEHPK3PXP");
        usuario.setBackupCodeHashes(new ArrayList<>());

        putTempLogin(1L, Instant.now().plusSeconds(300));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> authService.completeLoginWith2fa("temp-token-123", "000000"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Código 2FA o de respaldo inválido");
    }

    // ── generateTotpSetupForCurrentUser ──────────────────────────────

    @Test
    void generateTotpSetupShouldReturnSecretAndOtpauthUrl() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("test@test.es");
        setSecurityContext(usuario);

        TotpSetupResponse result = authService.generateTotpSetupForCurrentUser();

        assertThat(result.getSecret()).isNotBlank();
        assertThat(result.getOtpauthUrl()).contains("otpauth://totp/");
        assertThat(result.getOtpauthUrl()).contains("test%40test.es");
        verify(usuarioRepository).save(usuario);
        assertThat(usuario.getTotpTempSecret()).isEqualTo(result.getSecret());
    }

    @Test
    void generateTotpSetupShouldThrowWhenUnauthorized() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> authService.generateTotpSetupForCurrentUser())
                .isInstanceOf(ValidationException.class)
                .hasMessage("Unauthorized");
    }

    // ── unlinkGoogleFromCurrentUser ──────────────────────────────────

    @Test
    void unlinkGoogleShouldClearGoogleIdAndSave() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("test@test.es");
        usuario.setGoogleId("google-123");
        setSecurityContext(usuario);

        MessageResponse result = authService.unlinkGoogleFromCurrentUser();

        assertThat(result.getMessage()).isEqualTo("Cuenta Google desvinculada");
        assertThat(usuario.getGoogleId()).isNull();
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void unlinkGoogleShouldThrowWhenUnauthorized() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> authService.unlinkGoogleFromCurrentUser())
                .isInstanceOf(ValidationException.class)
                .hasMessage("Unauthorized");
    }

    // ── getGoogleAuthorizeUrl ────────────────────────────────────────

    @Test
    void getGoogleAuthorizeUrlShouldReturnUrlForLogin() {
        ReflectionTestUtils.setField(authService, "googleClientId", "test-client-id");
        ReflectionTestUtils.setField(
                authService, "googleRedirectUriLogin", "http://localhost/callback");

        String result = authService.getGoogleAuthorizeUrl("login");

        assertThat(result).contains("accounts.google.com");
        assertThat(result).contains("client_id=test-client-id");
        assertThat(result).contains("prompt=select_account");
    }

    @Test
    void getGoogleAuthorizeUrlShouldReturnUrlForLink() {
        ReflectionTestUtils.setField(authService, "googleClientId", "test-client-id");
        ReflectionTestUtils.setField(
                authService, "googleRedirectUriLogin", "http://localhost/callback");
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("test@test.es");
        setSecurityContext(usuario);

        String result = authService.getGoogleAuthorizeUrl("link");

        assertThat(result).contains("accounts.google.com");
    }

    @Test
    void getGoogleAuthorizeUrlShouldThrowWhenLinkWithoutAuth() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> authService.getGoogleAuthorizeUrl("link"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Debes estar autenticado para vincular");
    }

    // ── enableTotpForCurrentUser ─────────────────────────────────────

    @Test
    void enableTotpShouldThrowWhenNoTempSecret() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("test@test.es");
        usuario.setTotpTempSecret(null);
        setSecurityContext(usuario);

        assertThatThrownBy(() -> authService.enableTotpForCurrentUser("123456"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("No hay clave TOTP temporal registrada");
    }

    @Test
    void enableTotpShouldThrowWhenUnauthorized() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> authService.enableTotpForCurrentUser("123456"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Unauthorized");
    }

    // ── disableTotpForCurrentUser ────────────────────────────────────

    @Test
    void disableTotpShouldThrowWhenNo2faActive() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("test@test.es");
        usuario.setTotpSecret(null);
        setSecurityContext(usuario);

        assertThatThrownBy(() -> authService.disableTotpForCurrentUser("123456"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("2FA no está activado");
    }

    @Test
    void disableTotpShouldThrowWhenInvalidCode() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("test@test.es");
        usuario.setTotpSecret("JBSWY3DPEHPK3PXP");
        usuario.setBackupCodeHashes(new ArrayList<>());
        setSecurityContext(usuario);

        assertThatThrownBy(() -> authService.disableTotpForCurrentUser("000000"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Código 2FA o de respaldo inválido");
    }

    @Test
    void disableTotpShouldSucceedWithBackupCode() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("test@test.es");
        usuario.setTotpSecret("JBSWY3DPEHPK3PXP");
        usuario.setAutenticacionDosFactores(true);
        List<String> hashes = new ArrayList<>(List.of("hash1"));
        usuario.setBackupCodeHashes(hashes);
        setSecurityContext(usuario);

        when(passwordEncoder.matches(anyString(), any())).thenReturn(true);

        MessageResponse result = authService.disableTotpForCurrentUser("my-backup");

        assertThat(result.getMessage()).isEqualTo("2FA desactivado");
        assertThat(usuario.getTotpSecret()).isNull();
        assertThat(usuario.getAutenticacionDosFactores()).isFalse();
        verify(usuarioRepository).save(usuario);
    }
}
