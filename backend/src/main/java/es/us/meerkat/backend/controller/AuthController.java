package es.us.meerkat.backend.controller;

import es.us.meerkat.backend.dto.AuthResponse;
import es.us.meerkat.backend.dto.LoginRequest;
import es.us.meerkat.backend.dto.MessageResponse;
import es.us.meerkat.backend.dto.RegisterRequest;
import es.us.meerkat.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador de autenticación.
 *
 * Implementa los endpoints del tag Auth del OpenAPI:
 * registro, login y logout.
 * Base URL: /api/v1/auth
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public final class AuthController {

    /**
     * Servicio de autenticación.
     */
    private final AuthService authService;

    /**
     * Registra un nuevo usuario con email y contraseña.
     *
     * POST /api/v1/auth/register
     * Devuelve 201 con token JWT si el registro es exitoso.
     * Devuelve 409 si el email ya está registrado.
     *
     * @param request Datos del nuevo usuario.
     * @return AuthResponse con token JWT y datos del usuario.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @RequestBody final RegisterRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(authService.registrar(request));
    }

    /**
     * Autentica a un usuario con sus credenciales.
     *
     * POST /api/v1/auth/login
     * Devuelve 200 con token JWT si las credenciales son correctas.
     * Devuelve 401 si las credenciales son incorrectas.
     *
     * @param request Credenciales del usuario.
     * @return AuthResponse con token JWT y datos del usuario.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody final LoginRequest request) {
        return ResponseEntity.ok(authService.iniciarSesion(request));
    }

    /**
     * Cierra la sesión del usuario de forma segura.
     *
     * POST /api/v1/auth/logout
     * En arquitectura JWT sin estado, el token se invalida
     * en el cliente. Este endpoint confirma el evento.
     *
     * @return Mensaje de confirmación.
     */
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout() {
        return ResponseEntity.ok(
            MessageResponse.builder()
                .message("Sesión cerrada correctamente")
                .build());
    }
}
