package es.us.meerkat.backend.controller.google;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import es.us.meerkat.backend.dto.google.GoogleCalendarStatusResponse;
import es.us.meerkat.backend.dto.google.UpdateCalendarPreferenciasRequest;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.service.google.GoogleCalendarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controlador REST para la integración con Google Calendar.
 *
 * <p>Base URL: /api/v1/google-calendar
 *
 * <p>Flujo OAuth completo:
 *
 * <ol>
 *   <li>Frontend llama a {@code GET /api/v1/google-calendar/auth-url}.
 *   <li>Backend devuelve la URL de Google.
 *   <li>Frontend redirige al usuario a esa URL.
 *   <li>Usuario autoriza en Google.
 *   <li>Google redirige al backend: {@code GET /api/v1/google-calendar/oauth/callback}.
 *   <li>Backend guarda los tokens y redirige al frontend.
 * </ol>
 */
@RestController
@RequestMapping("/api/v1/google-calendar")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Google Calendar", description = "Integración con Google Calendar via OAuth 2.0")
public class GoogleCalendarController {

    private final GoogleCalendarService googleCalendarService;

    // ===============================
    // OBTENER URL DE AUTORIZACIÓN
    // ===============================

    /**
     * Devuelve la URL de Google a la que el frontend debe redirigir al usuario para iniciar el
     * flujo OAuth de Google Calendar.
     *
     * @param usuario Usuario autenticado.
     * @return URL de autorización de Google.
     */
    @GetMapping("/auth-url")
    @Operation(
            summary = "Obtener URL de autorización de Google",
            description =
                    "Devuelve la URL de Google OAuth a la que redirigir al usuario. "
                            + "El frontend debe abrir esta URL (redirect o popup).",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "URL generada correctamente"),
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado")
    })
    public ResponseEntity<String> getAuthUrl(@AuthenticationPrincipal Usuario usuario) {
        requireAuth(usuario);
        try {
            final String url = googleCalendarService.generarUrlAutorizacion(usuario.getId());
            return ResponseEntity.ok(url);
        } catch (Exception e) {
            log.error("Error generando URL de autorización: {}", e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Error al generar URL de autorización");
        }
    }

    // ===============================
    // CALLBACK OAUTH (Google redirige aquí)
    // ===============================

    /**
     * Endpoint de callback al que Google redirige tras la autorización del usuario.
     *
     * <p>Este endpoint NO requiere autenticación Bearer porque Google lo llama directamente. El ID
     * del usuario viaja en el parámetro {@code state} que se estableció al generar la URL.
     *
     * <p>Tras procesar los tokens, redirige al frontend con el resultado.
     *
     * @param code Código de autorización de Google.
     * @param state ID del usuario (establecido al generar la URL).
     * @param error Error de Google si el usuario denegó el acceso.
     */
    @GetMapping("/oauth/callback")
    @Operation(
            summary = "Callback OAuth de Google",
            description =
                    "Endpoint al que Google redirige tras la autorización. "
                            + "No llamar directamente, es para uso interno del flujo OAuth.")
    public ResponseEntity<Void> oauthCallback(
            @RequestParam(required = false) final String code,
            @RequestParam(required = false) final String state,
            @RequestParam(required = false) final String error) {

        // Usuario denegó el acceso
        if (error != null || code == null) {
            log.warn("Google Calendar OAuth denegado o error: {}", error);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", buildFrontendRedirect(false, "acceso_denegado"))
                    .build();
        }

        try {
            final Long usuarioId = Long.parseLong(state);
            googleCalendarService.procesarCallback(code, usuarioId);
            log.info("Google Calendar conectado exitosamente para usuario {}", usuarioId);

            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", buildFrontendRedirect(true, null))
                    .build();

        } catch (Exception e) {
            log.error("Error procesando callback de Google: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", buildFrontendRedirect(false, "error_interno"))
                    .build();
        }
    }

    // ===============================
    // ESTADO DE LA CONEXIÓN
    // ===============================

    /**
     * Devuelve el estado actual de la conexión de Google Calendar del usuario. El frontend usa esto
     * para saber si mostrar "Conectar" o "Desconectar".
     *
     * @param usuario Usuario autenticado.
     * @return Estado de la conexión y preferencias.
     */
    @GetMapping("/status")
    @Operation(
            summary = "Estado de la conexión con Google Calendar",
            description =
                    "Devuelve si el usuario tiene Google Calendar conectado, "
                            + "si la sincronización está activa y qué tipos sincroniza.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estado obtenido"),
        @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public ResponseEntity<GoogleCalendarStatusResponse> getStatus(
            @AuthenticationPrincipal Usuario usuario) {
        requireAuth(usuario);
        return ResponseEntity.ok(googleCalendarService.obtenerEstado(usuario.getId()));
    }

    // ===============================
    // ACTUALIZAR PREFERENCIAS
    // ===============================

    /**
     * Actualiza las preferencias de sincronización del usuario. Permite activar/desactivar la
     * sincronización y filtrar por tipos de evento.
     *
     * @param request Nuevas preferencias.
     * @param usuario Usuario autenticado.
     * @return Estado actualizado.
     */
    @PutMapping("/preferences")
    @Operation(
            summary = "Actualizar preferencias de sincronización",
            description =
                    "Permite activar/desactivar la sincronización y elegir qué tipos "
                            + "de evento sincronizar. Lista vacía en tiposSincronizados = todos.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Preferencias actualizadas"),
        @ApiResponse(responseCode = "400", description = "Google Calendar no conectado"),
        @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public ResponseEntity<GoogleCalendarStatusResponse> updatePreferences(
            @RequestBody final UpdateCalendarPreferenciasRequest request,
            @AuthenticationPrincipal Usuario usuario) {

        requireAuth(usuario);
        try {
            return ResponseEntity.ok(
                    googleCalendarService.actualizarPreferencias(usuario.getId(), request));
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    // ===============================
    // DESCONECTAR
    // ===============================

    /**
     * Desconecta Google Calendar del usuario. Elimina los tokens y todos los mapeos de eventos. Los
     * eventos ya creados en Google Calendar NO se eliminan (quedan en el calendario del usuario).
     *
     * @param usuario Usuario autenticado.
     * @return 204 No Content.
     */
    @DeleteMapping("/disconnect")
    @Operation(
            summary = "Desconectar Google Calendar",
            description =
                    "Desconecta Google Calendar. Los tokens se eliminan y la sincronización "
                            + "se detiene. Los eventos ya creados en Google Calendar permanecen.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Desconectado correctamente"),
        @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public ResponseEntity<Void> disconnect(@AuthenticationPrincipal Usuario usuario) {
        requireAuth(usuario);
        googleCalendarService.desconectar(usuario.getId());
        return ResponseEntity.noContent().build();
    }

    // ===============================
    // HELPERS
    // ===============================

    private void requireAuth(final Usuario usuario) {
        if (usuario == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }
    }

    private String buildFrontendRedirect(final boolean exito, final String error) {
        if (exito) {
            return "http://localhost:3000/settings/calendar?connected=true";
        }
        return "http://localhost:3000/settings/calendar?error=" + error;
    }
}
