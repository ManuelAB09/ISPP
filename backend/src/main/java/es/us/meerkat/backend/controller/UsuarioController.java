package es.us.meerkat.backend.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import es.us.meerkat.backend.dto.ChangePasswordRequest;
import es.us.meerkat.backend.dto.MessageResponse;
import es.us.meerkat.backend.dto.UpdateUserRequest;
import es.us.meerkat.backend.dto.UserDetailResponse;
import es.us.meerkat.backend.dto.UserPublicResponse;
import es.us.meerkat.backend.dto.VisibilityRequest;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.service.UsuarioService;
import lombok.RequiredArgsConstructor;

/**
 * Controlador de usuarios.
 *
 * <p>Implementa los endpoints del tag Usuarios del OpenAPI. Usa {@link AuthenticationPrincipal}
 * para obtener el usuario autenticado directamente del contexto de seguridad. Base URL:
 * /api/v1/users
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public final class UsuarioController {

    /** Servicio para operaciones de usuario. */
    private final UsuarioService usuarioService;

    /**
     * Devuelve el perfil completo del usuario autenticado.
     *
     * <p>GET /api/v1/users/me
     *
     * @param usuario Usuario autenticado (del token JWT).
     * @return Perfil completo del usuario.
     */
    @GetMapping("/me")
    public ResponseEntity<UserDetailResponse> getMe(
            @AuthenticationPrincipal final Usuario usuario) {
        if (usuario == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(usuarioService.obtenerPerfilPropio(usuario));
    }

    /**
     * Actualiza el perfil del usuario autenticado.
     *
     * <p>PUT /api/v1/users/me
     *
     * @param usuario Usuario autenticado.
     * @param request Datos a actualizar.
     * @return Perfil actualizado.
     */
    @PutMapping("/me")
    public ResponseEntity<UserDetailResponse> updateMe(
            @AuthenticationPrincipal final Usuario usuario,
            @RequestBody final UpdateUserRequest request) {
        return ResponseEntity.ok(usuarioService.actualizarPerfil(usuario, request));
    }

    /**
     * Sube una foto de perfil personalizada para el usuario autenticado.
     *
     * <p>POST /api/v1/users/me/photo (multipart/form-data)
     *
     * @param usuario Usuario autenticado.
     * @param file Archivo de imagen a usar como foto de perfil.
     * @return Perfil actualizado.
     */
    @PostMapping(value = "/me/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserDetailResponse> uploadProfilePhoto(
            @AuthenticationPrincipal final Usuario usuario,
            @RequestParam("file") final MultipartFile file) {
        if (usuario == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            return ResponseEntity.ok(usuarioService.actualizarFotoPerfil(usuario, file));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Elimina permanentemente la cuenta del usuario autenticado.
     *
     * <p>DELETE /api/v1/users/me Devuelve 204 sin contenido tras la eliminación.
     *
     * @param usuario Usuario autenticado a eliminar.
     * @return Respuesta vacía con código 204.
     */
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe(@AuthenticationPrincipal final Usuario usuario) {
        usuarioService.eliminarCuenta(usuario);
        return ResponseEntity.noContent().build();
    }

    /**
     * Cambia la contraseña del usuario autenticado.
     *
     * <p>PUT /api/v1/users/me/password
     *
     * @param usuario Usuario autenticado.
     * @param request Contraseña actual y nueva.
     * @return Mensaje de confirmación.
     */
    @PutMapping("/me/password")
    public ResponseEntity<MessageResponse> changePassword(
            @AuthenticationPrincipal final Usuario usuario,
            @RequestBody final ChangePasswordRequest request) {
        usuarioService.cambiarPassword(usuario, request);
        return ResponseEntity.ok(
                MessageResponse.builder().message("Contraseña actualizada correctamente").build());
    }

    /**
     * Actualiza la visibilidad del perfil en listados públicos.
     *
     * <p>PUT /api/v1/users/me/visibility
     *
     * @param usuario Usuario autenticado.
     * @param request Nueva configuración de visibilidad.
     * @return Perfil actualizado con nueva visibilidad.
     */
    @PutMapping("/me/visibility")
    public ResponseEntity<UserDetailResponse> updateVisibility(
            @AuthenticationPrincipal final Usuario usuario,
            @RequestBody final VisibilityRequest request) {
        return ResponseEntity.ok(usuarioService.actualizarVisibilidad(usuario, request));
    }

    /**
     * Devuelve el perfil público de un usuario por su ID.
     *
     * <p>GET /api/v1/users/{userId} Accesible sin autenticación si el perfil es visible.
     *
     * @param userId ID del usuario cuyo perfil se quiere ver.
     * @return Perfil público del usuario.
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserPublicResponse> getUserById(@PathVariable final Long userId) {
        return ResponseEntity.ok(usuarioService.obtenerPerfilPublico(userId));
    }

    /**
     * Devuelve los avatares predefinidos disponibles para foto de perfil.
     *
     * <p>GET /api/v1/users/profile-avatars
     *
     * @return Lista de rutas públicas de avatares.
     */
    @GetMapping("/profile-avatars")
    public ResponseEntity<List<String>> getProfileAvatars() {
        return ResponseEntity.ok(usuarioService.obtenerAvataresPerfilDisponibles());
    }
}
