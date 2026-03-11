package es.us.meerkat.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import es.us.meerkat.backend.dto.AlertaEventoResponse;
import es.us.meerkat.backend.dto.MisEventosItemResponse;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.service.MisEventosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Controlador REST para el panel "Mis Eventos" y las alertas automáticas.
 *
 * <p>Expone endpoints para:
 *
 * <ul>
 *   <li>Listado unificado de próximos eventos del usuario.
 *   <li>Historial completo de eventos.
 *   <li>Gestión de alertas: consulta, marcado como leída, conteo.
 * </ul>
 *
 * <p>Base URL: /api/v1/my-events
 */
@RestController
@RequestMapping("/api/v1/my-events")
@RequiredArgsConstructor
@Tag(name = "Mis Eventos", description = "Panel de eventos del usuario con alertas automáticas")
public class MisEventosController {

    private final MisEventosService misEventosService;

    // ===============================
    // MIS EVENTOS
    // ===============================

    /**
     * Obtiene el listado unificado de próximos eventos del usuario autenticado.
     *
     * <p>Incluye todos los eventos de las comunidades a las que pertenece el usuario, ordenados por
     * fecha ascendente. Los eventos próximos (menos de 24h) y los inminentes (menos de 15 min) se
     * marcan con flags para su destacado visual en el frontend.
     *
     * @param usuario Usuario autenticado.
     * @return Lista de próximos eventos con información de proximidad.
     */
    @GetMapping
    @Operation(
            summary = "Mis próximos eventos",
            description =
                    "Devuelve el listado unificado de próximos eventos del usuario: reuniones, "
                            + "exámenes, cuestionarios, tutorías y clases. Los eventos próximos "
                            + "(<24h) y los inminentes (<15min) se marcan con flags "
                            + "para destacarlos visualmente.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente"),
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado")
    })
    public ResponseEntity<List<MisEventosItemResponse>> obtenerMisEventos(
            @AuthenticationPrincipal Usuario usuario) {

        requireAuth(usuario);
        return ResponseEntity.ok(misEventosService.obtenerMisEventos(usuario.getId()));
    }

    /**
     * Obtiene el historial completo de eventos del usuario (pasados y futuros).
     *
     * @param incluirCancelados Si se incluyen eventos cancelados (por defecto false).
     * @param usuario Usuario autenticado.
     * @return Lista completa de eventos del usuario.
     */
    @GetMapping("/history")
    @Operation(
            summary = "Historial de eventos",
            description =
                    "Devuelve el historial completo de eventos del usuario (pasados y futuros)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Historial obtenido correctamente"),
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado")
    })
    public ResponseEntity<List<MisEventosItemResponse>> obtenerHistorial(
            @RequestParam(defaultValue = "false") boolean incluirCancelados,
            @AuthenticationPrincipal Usuario usuario) {

        requireAuth(usuario);
        return ResponseEntity.ok(
                misEventosService.obtenerHistorialEventos(usuario.getId(), incluirCancelados));
    }

    // ===============================
    // ALERTAS
    // ===============================

    /**
     * Obtiene las alertas no leídas del usuario autenticado.
     *
     * <p>Las alertas se generan automáticamente:
     *
     * <ul>
     *   <li>24 horas antes del inicio del evento → tipo PROXIMA_24H.
     *   <li>15 minutos antes del inicio → tipo INMINENTE_15MIN.
     * </ul>
     *
     * @param usuario Usuario autenticado.
     * @return Lista de alertas no leídas.
     */
    @GetMapping("/alerts")
    @Operation(
            summary = "Mis alertas no leídas",
            description =
                    "Devuelve las alertas automáticas no leídas del usuario: eventos que comienzan "
                            + "en menos de 24h (PROXIMA_24H) o en menos de 15 minutos "
                            + "(INMINENTE_15MIN).",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Alertas obtenidas"),
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado")
    })
    public ResponseEntity<List<AlertaEventoResponse>> obtenerAlertasNoLeidas(
            @AuthenticationPrincipal Usuario usuario) {

        requireAuth(usuario);
        return ResponseEntity.ok(misEventosService.obtenerAlertasNoLeidas(usuario.getId()));
    }

    /**
     * Obtiene todas las alertas del usuario (leídas y no leídas).
     *
     * @param usuario Usuario autenticado.
     * @return Lista completa de alertas.
     */
    @GetMapping("/alerts/all")
    @Operation(
            summary = "Todas mis alertas",
            description = "Devuelve todas las alertas del usuario, incluyendo las ya leídas.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Alertas obtenidas"),
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado")
    })
    public ResponseEntity<List<AlertaEventoResponse>> obtenerTodasLasAlertas(
            @AuthenticationPrincipal Usuario usuario) {

        requireAuth(usuario);
        return ResponseEntity.ok(misEventosService.obtenerTodasLasAlertas(usuario.getId()));
    }

    /**
     * Cuenta las alertas no leídas del usuario. Útil para el badge del icono de notificaciones.
     *
     * @param usuario Usuario autenticado.
     * @return Número de alertas no leídas.
     */
    @GetMapping("/alerts/count")
    @Operation(
            summary = "Contador de alertas no leídas",
            description =
                    "Devuelve el número de alertas no leídas. Útil para el badge de"
                            + " notificaciones.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Contador obtenido"),
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado")
    })
    public ResponseEntity<Long> contarAlertasNoLeidas(@AuthenticationPrincipal Usuario usuario) {
        requireAuth(usuario);
        return ResponseEntity.ok(misEventosService.contarAlertasNoLeidas(usuario.getId()));
    }

    /**
     * Marca una alerta específica como leída.
     *
     * @param alertaId Identificador de la alerta.
     * @param usuario Usuario autenticado.
     * @return La alerta actualizada.
     */
    @PatchMapping("/alerts/{alertaId}/read")
    @Operation(
            summary = "Marcar alerta como leída",
            description = "Marca una alerta específica como leída.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Alerta marcada como leída"),
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
        @ApiResponse(responseCode = "403", description = "No tienes permiso sobre esta alerta"),
        @ApiResponse(responseCode = "404", description = "Alerta no encontrada")
    })
    public ResponseEntity<AlertaEventoResponse> marcarAlertaComoLeida(
            @PathVariable @Parameter(description = "ID de la alerta") final Long alertaId,
            @AuthenticationPrincipal Usuario usuario) {

        requireAuth(usuario);
        try {
            return ResponseEntity.ok(
                    misEventosService.marcarAlertaComoLeida(alertaId, usuario.getId()));
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("permiso")) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
            }
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    /**
     * Marca todas las alertas no leídas del usuario como leídas. Útil para "Marcar todo como
     * leído".
     *
     * @param usuario Usuario autenticado.
     * @return 204 No Content si se ha realizado correctamente.
     */
    @PatchMapping("/alerts/read-all")
    @Operation(
            summary = "Marcar todas las alertas como leídas",
            description = "Marca todas las alertas no leídas del usuario como leídas.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Todas las alertas marcadas como leídas"),
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado")
    })
    public ResponseEntity<Void> marcarTodasComoLeidas(@AuthenticationPrincipal Usuario usuario) {
        requireAuth(usuario);
        misEventosService.marcarTodasComoLeidas(usuario.getId());
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
