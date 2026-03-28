package es.us.meerkat.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import es.us.meerkat.backend.dto.AlarmaPersonalizadaResponse;
import es.us.meerkat.backend.dto.CrearAlarmaRequest;
import es.us.meerkat.backend.dto.CrearAlarmasLoteRequest;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.service.events.AlarmaPersonalizadaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controlador REST para la gestión de alarmas personalizadas por evento.
 *
 * <p>Permite al usuario crear, consultar y eliminar alarmas específicas para eventos concretos,
 * eligiendo la antelación y el canal de notificación.
 *
 * <p>Base URL: /api/v1/events/{eventoId}/alarms
 */
@RestController
@RequestMapping("/api/v1/events/{eventoId}/alarms")
@RequiredArgsConstructor
@Tag(name = "Alarmas", description = "Alarmas personalizadas por evento")
public class AlarmaPersonalizadaController {

    private final AlarmaPersonalizadaService alarmaService;

    // ===============================
    // CREAR ALARMA INDIVIDUAL
    // ===============================

    /**
     * Crea una alarma personalizada para un evento concreto.
     *
     * <p>Se puede llamar desde la página del evento en cualquier momento, o al confirmar asistencia
     * si el usuario elige una sola antelación. Si ya existe una alarma con los mismos minutos,
     * devuelve la existente (sin duplicar).
     *
     * @param eventoId ID del evento.
     * @param request minutosAntes (obligatorio) y canal (opcional).
     * @param usuario Usuario autenticado.
     * @return La alarma creada.
     */
    @PostMapping
    @Operation(
            summary = "Crear alarma para un evento",
            description =
                    "Crea una alarma personalizada. Si ya existe con los mismos minutos, "
                            + "devuelve la existente sin duplicar. "
                            + "El campo 'canal' es opcional: si no se envía se usa el "
                            + "canal por defecto de las preferencias del usuario.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Alarma creada correctamente"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos o evento ya finalizado"),
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
        @ApiResponse(responseCode = "404", description = "Evento no encontrado")
    })
    public ResponseEntity<AlarmaPersonalizadaResponse> crearAlarma(
            @PathVariable @Parameter(description = "ID del evento") final Long eventoId,
            @Valid @RequestBody final CrearAlarmaRequest request,
            @AuthenticationPrincipal Usuario usuario) {

        requireAuth(usuario);
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(alarmaService.crearAlarma(usuario.getId(), eventoId, request));
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    // ===============================
    // CREAR ALARMAS EN LOTE
    // ===============================

    /**
     * Crea múltiples alarmas para un evento en una sola llamada.
     *
     * <p>Pensado para usarse al confirmar asistencia, cuando el usuario selecciona varios
     * checkboxes de antelación a la vez. Los duplicados se ignoran silenciosamente.
     *
     * <p>Ejemplo de body:
     *
     * <pre>
     * {
     *   "minutosAntesList": [2880, 1440, 30],
     *   "canal": "AMBOS"
     * }
     * </pre>
     *
     * @param eventoId ID del evento.
     * @param request Lista de minutos y canal opcional.
     * @param usuario Usuario autenticado.
     * @return Lista de alarmas creadas (no incluye las que ya existían).
     */
    @PostMapping("/batch")
    @Operation(
            summary = "Crear varias alarmas a la vez",
            description =
                    "Crea múltiples alarmas en un solo request. "
                            + "Ideal para usar al confirmar asistencia con checkboxes. "
                            + "Los duplicados se ignoran.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Alarmas creadas"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos o evento ya finalizado"),
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
        @ApiResponse(responseCode = "404", description = "Evento no encontrado")
    })
    public ResponseEntity<List<AlarmaPersonalizadaResponse>> crearAlarmasLote(
            @PathVariable final Long eventoId,
            @Valid @RequestBody final CrearAlarmasLoteRequest request,
            @AuthenticationPrincipal Usuario usuario) {

        requireAuth(usuario);
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(alarmaService.crearAlarmasLote(usuario.getId(), eventoId, request));
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    // ===============================
    // LISTAR ALARMAS DE UN EVENTO
    // ===============================

    /**
     * Lista todas las alarmas del usuario para un evento concreto.
     *
     * <p>El frontend usa esto para mostrar qué alarmas están activas en la página del evento, con
     * la opción de eliminarlas.
     *
     * @param eventoId ID del evento.
     * @param usuario Usuario autenticado.
     * @return Lista de alarmas configuradas para ese evento.
     */
    @GetMapping
    @Operation(
            summary = "Listar alarmas del usuario para un evento",
            description = "Devuelve las alarmas configuradas por el usuario para este evento.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de alarmas obtenida"),
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado")
    })
    public ResponseEntity<List<AlarmaPersonalizadaResponse>> listarAlarmasDeEvento(
            @PathVariable final Long eventoId, @AuthenticationPrincipal Usuario usuario) {

        requireAuth(usuario);
        return ResponseEntity.ok(alarmaService.listarAlarmasDeEvento(usuario.getId(), eventoId));
    }

    // ===============================
    // ELIMINAR ALARMA INDIVIDUAL
    // ===============================

    /**
     * Elimina una alarma concreta.
     *
     * @param eventoId ID del evento (para la URL coherente).
     * @param alarmaId ID de la alarma a eliminar.
     * @param usuario Usuario autenticado.
     * @return 204 No Content.
     */
    @DeleteMapping("/{alarmaId}")
    @Operation(
            summary = "Eliminar una alarma",
            description = "Elimina una alarma personalizada del usuario.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Alarma eliminada"),
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
        @ApiResponse(responseCode = "403", description = "No tienes permiso sobre esta alarma"),
        @ApiResponse(responseCode = "404", description = "Alarma no encontrada")
    })
    public ResponseEntity<Void> eliminarAlarma(
            @PathVariable final Long eventoId,
            @PathVariable @Parameter(description = "ID de la alarma") final Long alarmaId,
            @AuthenticationPrincipal Usuario usuario) {

        requireAuth(usuario);
        try {
            alarmaService.eliminarAlarma(alarmaId, usuario.getId());
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("permiso")) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
            }
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    // ===============================
    // ELIMINAR TODAS LAS ALARMAS DE UN EVENTO
    // ===============================

    /**
     * Elimina todas las alarmas del usuario para un evento.
     *
     * <p>Se llama automáticamente cuando el usuario cancela su asistencia, pero también puede
     * llamarlo manualmente desde la UI.
     *
     * @param eventoId ID del evento.
     * @param usuario Usuario autenticado.
     * @return 204 No Content.
     */
    @DeleteMapping
    @Operation(
            summary = "Eliminar todas las alarmas de un evento",
            description =
                    "Elimina todas las alarmas del usuario para este evento. "
                            + "Se llama al cancelar asistencia.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Alarmas eliminadas"),
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado")
    })
    public ResponseEntity<Void> eliminarTodasLasAlarmas(
            @PathVariable final Long eventoId, @AuthenticationPrincipal Usuario usuario) {

        requireAuth(usuario);
        alarmaService.eliminarAlarmasDeEvento(usuario.getId(), eventoId);
        return ResponseEntity.noContent().build();
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
