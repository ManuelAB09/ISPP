package es.us.meerkat.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import es.us.meerkat.backend.dto.AuthResponse;
import es.us.meerkat.backend.dto.ForgotPasswordRequest;
import es.us.meerkat.backend.dto.LoginRequest;
import es.us.meerkat.backend.dto.MessageResponse;
import es.us.meerkat.backend.dto.RegisterRequest;
import es.us.meerkat.backend.dto.ResendVerificationRequest;
import es.us.meerkat.backend.service.AuthService;
import lombok.RequiredArgsConstructor;

/**
 * Controlador de autenticación.
 *
 * <p>Implementa los endpoints del tag Auth del OpenAPI: registro, login y logout. Base URL:
 * /api/v1/auth
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public final class AuthController {

    /** Servicio de autenticación. */
    private final AuthService authService;

    /**
     * Registra un nuevo usuario con email y contraseña.
     *
     * <p>POST /api/v1/auth/register Devuelve 201 con mensaje de confirmación si el registro es
     * exitoso. Se envía un email de verificación al usuario. Devuelve 409 si el email ya está
     * registrado.
     *
     * @param request Datos del nuevo usuario.
     * @return MessageResponse con instrucciones para verificar el email.
     */
    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@RequestBody final RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(request));
    }

    /**
     * Verifica el email de un usuario usando el token enviado por email.
     *
     * <p>GET /api/v1/auth/verify?token=xxx Devuelve 200 con token JWT si la verificación es
     * exitosa. Devuelve 400 si el token es inválido o ha expirado.
     *
     * @param token Token de verificación.
     * @return AuthResponse con token JWT y datos del usuario.
     */
    @GetMapping("/verify")
    public ResponseEntity<AuthResponse> verifyEmail(@RequestParam final String token) {
        return ResponseEntity.ok(authService.verificarEmail(token));
    }

    /**
     * Reenvía el email de verificación a un usuario.
     *
     * <p>POST /api/v1/auth/resend-verification Devuelve 200 con mensaje de confirmación. Devuelve
     * 400 si el email no existe o ya está verificado.
     *
     * @param request DTO con el email del usuario.
     * @return MessageResponse con confirmación.
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<MessageResponse> resendVerification(
            @RequestBody final ResendVerificationRequest request) {
        return ResponseEntity.ok(authService.reenviarVerificacion(request.getEmail()));
    }

    /**
     * Autentica a un usuario con sus credenciales.
     *
     * <p>POST /api/v1/auth/login Devuelve 200 con token JWT si las credenciales son correctas.
     * Devuelve 401 si las credenciales son incorrectas.
     *
     * @param request Credenciales del usuario.
     * @return AuthResponse con token JWT y datos del usuario.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody final LoginRequest request) {
        return ResponseEntity.ok(authService.iniciarSesion(request));
    }

    /**
     * Cierra la sesión del usuario de forma segura.
     *
     * <p>POST /api/v1/auth/logout En arquitectura JWT sin estado, el token se invalida en el
     * cliente. Este endpoint confirma el evento.
     *
     * @return Mensaje de confirmación.
     */
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout() {
        return ResponseEntity.ok(
                MessageResponse.builder().message("Sesión cerrada correctamente").build());
    }

    /**
     * Solicita la recuperación de contraseña.
     *
     * <p>POST /api/v1/auth/password/forgot Envía un email con instrucciones para recuperar la
     * contraseña. Por seguridad, siempre devuelve 200 OK aunque el email no exista.
     *
     * @param request DTO con el email del usuario.
     * @return MessageResponse con confirmación.
     */
    @PostMapping("/password/forgot")
    public ResponseEntity<MessageResponse> forgotPassword(
            @RequestBody final ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.recuperarContrasena(request));
    }
}
