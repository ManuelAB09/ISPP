package es.us.meerkat.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import es.us.meerkat.backend.dto.PreferenciasNotificacionResponse;
import es.us.meerkat.backend.dto.UpdatePreferenciasRequest;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.service.PreferenciasNotificacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Controlador REST para la gestión de preferencias de notificación del usuario.
 *
 * <p>Permite al usuario consultar y modificar su configuración de recordatorios por email desde la
 * pantalla de ajustes.
 *
 * <p>Base URL: /api/v1/notifications/preferences
 */
@RestController
@RequestMapping("/api/v1/notifications/preferences")
@RequiredArgsConstructor
@Tag(name = "Notificaciones", description = "Preferencias de recordatorios por email")
public class NotificacionesController {

    private final PreferenciasNotificacionService preferenciasService;

    // ===============================
    // GET PREFERENCIAS
    // ===============================

    /**
     * Devuelve las preferencias de notificación del usuario autenticado.
     *
     * <p>Si el usuario nunca las ha configurado, devuelve los valores por defecto: emails
     * activados, recordatorio 24h y 1h activados, 30min desactivado.
     *
     * @param usuario Usuario autenticado.
     * @return Preferencias actuales.
     */
    @GetMapping
    @Operation(
            summary = "Obtener preferencias de notificación",
            description = "Devuelve la configuración de recordatorios por email del usuario.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Preferencias obtenidas"),
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado")
    })
    public ResponseEntity<PreferenciasNotificacionResponse> obtenerPreferencias(
            @AuthenticationPrincipal Usuario usuario) {

        requireAuth(usuario);
        return ResponseEntity.ok(preferenciasService.obtenerPreferencias(usuario.getId()));
    }

    // ===============================
    // PUT PREFERENCIAS
    // ===============================

    /**
     * Actualiza las preferencias de notificación del usuario autenticado.
     *
     * <p>Actualización parcial: solo se modifican los campos que vengan en el body. Los campos que
     * no se envíen conservan su valor actual.
     *
     * @param request Campos a actualizar.
     * @param usuario Usuario autenticado.
     * @return Preferencias actualizadas.
     */
    @PutMapping
    @Operation(
            summary = "Actualizar preferencias de notificación",
            description =
                    "Actualiza la configuración de recordatorios por email. "
                            + "Solo se modifican los campos enviados en el body.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Preferencias actualizadas"),
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado")
    })
    public ResponseEntity<PreferenciasNotificacionResponse> actualizarPreferencias(
            @RequestBody final UpdatePreferenciasRequest request,
            @AuthenticationPrincipal Usuario usuario) {

        requireAuth(usuario);
        return ResponseEntity.ok(
                preferenciasService.actualizarPreferencias(usuario.getId(), request));
    }

    // ===============================
    // HELPER
    // ===============================

    private void requireAuth(final Usuario usuario) {
        if (usuario == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }
    }
}
