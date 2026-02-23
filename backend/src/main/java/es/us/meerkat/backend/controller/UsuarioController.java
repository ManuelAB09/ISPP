package es.us.meerkat.backend.controller;

import es.us.meerkat.backend.dto.AuthResponse;
import es.us.meerkat.backend.dto.CambiarPasswordRequest;
import es.us.meerkat.backend.dto.LoginRequest;
import es.us.meerkat.backend.dto.PrivacidadRequest;
import es.us.meerkat.backend.dto.RegisterRequest;
import es.us.meerkat.backend.dto.UpdatePerfilRequest;
import es.us.meerkat.backend.dto.UsuarioPerfilResponse;
import es.us.meerkat.backend.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador para manejar las operaciones relacionadas con los usuarios.
 *
 * Cubre registro, inicio/cierre de sesión, edición de perfil,
 * cambio de contraseña, eliminación de cuenta y configuración de privacidad.
 */
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public final class UsuarioController {

    /**
     * Servicio para operaciones de usuario.
     */
    private final UsuarioService usuarioService;

    // ===============================
    // REGISTRO
    // ===============================

    /**
     * Registra un nuevo usuario con email y contraseña.
     *
     * Devuelve un mensaje de confirmación si el registro es exitoso,
     * o un error claro si el email ya está en uso o los datos no son válidos.
     *
     * @param request Datos del nuevo usuario (email, contraseña, nombre).
     * @return Mensaje de confirmación de registro.
     */
    @PostMapping("/registrar")
    public ResponseEntity<String> registrar(
        @RequestBody final RegisterRequest request) {
        return ResponseEntity.ok(usuarioService.registrar(request));
    }

    // ===============================
    // INICIO DE SESIÓN
    // ===============================

    /**
     * Autentica a un usuario con sus credenciales.
     *
     * Devuelve un token JWT y los datos básicos del usuario si las
     * credenciales son correctas. Retorna error si son incorrectas.
     *
     * @param request Credenciales del usuario (email y contraseña).
     * @return DTO con token JWT y datos del usuario autenticado.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> iniciarSesion(
        @RequestBody final LoginRequest request) {
        return ResponseEntity.ok(usuarioService.iniciarSesion(request));
    }

    /**
     * Cierra la sesión del usuario de forma segura.
     *
     * En una arquitectura JWT sin estado, el cierre de sesión se gestiona
     * en el cliente eliminando el token. Este endpoint sirve como confirmación
     * explícita y permite registrar el evento si fuera necesario.
     *
     * @return Mensaje de confirmación de cierre de sesión.
     */
    @PostMapping("/logout")
    public ResponseEntity<String> cerrarSesion() {
        return ResponseEntity.ok("Sesión cerrada correctamente");
    }

    // ===============================
    // EDICIÓN DE PERFIL
    // ===============================

    /**
     * Actualiza la información personal del usuario autenticado.
     *
     * Permite modificar nombre, foto de perfil, bio e intereses.
     * Los cambios se reflejan inmediatamente en el perfil público.
     *
     * @param usuarioId Identificador del usuario.
     * @param request   Datos actualizados del perfil.
     * @return Perfil público actualizado.
     */
    @PutMapping("/{usuarioId}/perfil")
    public ResponseEntity<UsuarioPerfilResponse> actualizarPerfil(
            @PathVariable final Long usuarioId,
            @RequestBody final UpdatePerfilRequest request) {

        return ResponseEntity
            .ok(usuarioService.actualizarPerfil(usuarioId, request));
    }

    // ===============================
    // VER PERFIL PÚBLICO
    // ===============================

    /**
     * Devuelve el perfil público de un usuario por su identificador.
     *
     * Accesible por cualquier usuario (autenticado o visitante).
     * Muestra únicamente la información que el usuario ha hecho pública.
     *
     * @param usuarioId Identificador del usuario cuyo perfil se quiere ver.
     * @return Perfil público del usuario.
     */
    @GetMapping("/{usuarioId}/perfil")
    public ResponseEntity<UsuarioPerfilResponse> verPerfil(
            @PathVariable final Long usuarioId) {

        return ResponseEntity.ok(usuarioService.verPerfil(usuarioId));
    }

    /**
     * Lista todos los perfiles públicos de usuarios visibles en listados.
     *
     * Solo incluye usuarios que hayan activado la opción de visibilidad.
     * Accesible por cualquier usuario (autenticado o visitante).
     *
     * @return Lista de perfiles públicos visibles.
     */
    @GetMapping("/publicos")
    public ResponseEntity<List<UsuarioPerfilResponse>>
        listarPerfilesPublicos() {
        return ResponseEntity.ok(usuarioService.listarPerfilesPublicos());
    }

    // ===============================
    // CAMBIAR CONTRASEÑA
    // ===============================

    /**
     * Permite al usuario autenticado modificar su contraseña.
     *
     * Solicita la contraseña actual y la nueva (con confirmación).
     * Devuelve mensaje de éxito o error según el resultado.
     *
     * @param usuarioId Identificador del usuario.
     * @param request   Contraseña actual y nueva con confirmación.
     * @return Mensaje de éxito o error.
     */
    @PutMapping("/{usuarioId}/cambiar-password")
    public ResponseEntity<String> cambiarPassword(
            @PathVariable final Long usuarioId,
            @RequestBody final CambiarPasswordRequest request) {

        return ResponseEntity
            .ok(usuarioService.cambiarPassword(usuarioId, request));
    }

    // ===============================
    // ELIMINAR CUENTA
    // ===============================

    /**
     * Elimina permanentemente la cuenta del usuario autenticado.
     *
     * El frontend debe mostrar un flujo de confirmación explícito
     * ("¿Estás seguro? Esta acción no se puede deshacer")
     * antes de llamar a este endpoint.
     *
     * @param usuarioId Identificador del usuario a eliminar.
     * @return Mensaje de confirmación de eliminación.
     */
    @DeleteMapping("/{usuarioId}/cuenta")
    public ResponseEntity<String> eliminarCuenta(
        @PathVariable final Long usuarioId) {
        return ResponseEntity.ok(usuarioService.eliminarCuenta(usuarioId));
    }

    // ===============================
    // CONFIGURACIÓN DE PRIVACIDAD
    // ===============================

    /**
     * Actualiza la configuración de privacidad del usuario.
     *
     * Permite al usuario decidir si su perfil aparece en listados
     * públicos y resultados de búsqueda dentro de la plataforma.
     *
     * @param usuarioId Identificador del usuario.
     * @param request   Configuración de privacidad deseada.
     * @return Mensaje de confirmación.
     */
    @PutMapping("/{usuarioId}/privacidad")
    public ResponseEntity<String> actualizarPrivacidad(
            @PathVariable final Long usuarioId,
            @RequestBody final PrivacidadRequest request) {

        return ResponseEntity
            .ok(usuarioService.actualizarPrivacidad(usuarioId, request));
    }
}
