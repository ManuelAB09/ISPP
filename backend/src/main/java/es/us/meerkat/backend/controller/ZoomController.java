package es.us.meerkat.backend.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.us.meerkat.backend.dto.CreateZoomMeetingRequest;
import es.us.meerkat.backend.dto.MessageResponse;
import es.us.meerkat.backend.dto.ZoomMeetingResponse;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.entity.ZoomMeeting;
import es.us.meerkat.backend.service.AuthorizationService;
import es.us.meerkat.backend.service.ZoomIntegrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** Endpoints para la integracion con Zoom. */
@RestController
@RequestMapping("/api/v1/zoom")
@RequiredArgsConstructor
@Tag(name = "Zoom", description = "Gestion de reuniones Zoom dentro de comunidades")
public class ZoomController {

    private final ZoomIntegrationService zoomIntegrationService;
    private final AuthorizationService authorizationService;

    @PostMapping("/communities/{communityId}/meeting")
    @Operation(
            summary = "Crear o reutilizar llamada de comunidad",
            description =
                    "Si no existe una llamada activa, la crea. Si existe, todos reciben la misma"
                            + " sala")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Llamada creada u obtenida"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "No pertenece a la comunidad"),
        @ApiResponse(responseCode = "503", description = "Zoom no configurado en backend")
    })
    public ResponseEntity<?> createOrGetMeeting(
            @PathVariable Long communityId,
            @RequestBody(required = false) CreateZoomMeetingRequest request,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(MessageResponse.builder().message("Usuario no autenticado").build());
        }
        if (!authorizationService.isMemberOf(usuario.getId(), communityId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(
                            MessageResponse.builder()
                                    .message("No perteneces a esta comunidad")
                                    .build());
        }

        try {
            ZoomMeeting meeting =
                    zoomIntegrationService.createOrGetActiveMeeting(
                            communityId,
                            usuario.getId(),
                            request != null ? request.topic() : null,
                            request != null ? request.durationMinutes() : null);

            return ResponseEntity.ok(toResponse(meeting));
        } catch (RuntimeException e) {
            if ("Faltan credenciales Zoom. Revisa zoom.client-id, zoom.client-secret y zoom.account-id"
                    .equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(
                                MessageResponse.builder()
                                        .message(
                                                "Zoom no esta configurado en backend. Define"
                                                        + " ZOOM_CLIENT_ID, ZOOM_CLIENT_SECRET y"
                                                        + " ZOOM_ACCOUNT_ID")
                                        .build());
            }
            throw e;
        }
    }

    @GetMapping("/communities/{communityId}/meeting")
    @Operation(summary = "Obtener llamada activa de la comunidad")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Llamada activa encontrada"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "No pertenece a la comunidad"),
        @ApiResponse(responseCode = "404", description = "No hay llamada activa")
    })
    public ResponseEntity<?> getActiveMeeting(
            @PathVariable Long communityId, @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(MessageResponse.builder().message("Usuario no autenticado").build());
        }
        if (!authorizationService.isMemberOf(usuario.getId(), communityId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(
                            MessageResponse.builder()
                                    .message("No perteneces a esta comunidad")
                                    .build());
        }

        try {
            ZoomMeeting meeting =
                    zoomIntegrationService.getActiveMeeting(communityId, usuario.getId());
            return ResponseEntity.ok(toResponse(meeting));
        } catch (RuntimeException e) {
            if ("No hay llamada activa en esta comunidad".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(
                                MessageResponse.builder()
                                        .message("No hay llamada activa en esta comunidad")
                                        .build());
            }
            throw e;
        }
    }

    @PostMapping("/communities/{communityId}/meeting/join")
    @Operation(
            summary = "Entrar en la llamada",
            description =
                    "Devuelve link y clave de acceso de forma automatica y registra que el usuario"
                            + " entro")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Acceso a llamada devuelto"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "No pertenece a la comunidad"),
        @ApiResponse(responseCode = "404", description = "No hay llamada activa")
    })
    public ResponseEntity<?> joinMeeting(
            @PathVariable Long communityId, @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(MessageResponse.builder().message("Usuario no autenticado").build());
        }
        if (!authorizationService.isMemberOf(usuario.getId(), communityId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(
                            MessageResponse.builder()
                                    .message("No perteneces a esta comunidad")
                                    .build());
        }

        try {
            return ResponseEntity.ok(
                    zoomIntegrationService.joinActiveMeeting(communityId, usuario.getId()));
        } catch (RuntimeException e) {
            if ("No hay llamada activa en esta comunidad".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(
                                MessageResponse.builder()
                                        .message("No hay llamada activa en esta comunidad")
                                        .build());
            }
            throw e;
        }
    }

    @GetMapping("/communities/{communityId}/meeting/participants")
    @Operation(summary = "Listar quien esta en la llamada activa")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Participantes listados"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "No pertenece a la comunidad"),
        @ApiResponse(responseCode = "404", description = "No hay llamada activa")
    })
    public ResponseEntity<?> listParticipants(
            @PathVariable Long communityId, @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(MessageResponse.builder().message("Usuario no autenticado").build());
        }
        if (!authorizationService.isMemberOf(usuario.getId(), communityId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(
                            MessageResponse.builder()
                                    .message("No perteneces a esta comunidad")
                                    .build());
        }

        try {
            return ResponseEntity.ok(
                    zoomIntegrationService.getActiveParticipants(communityId, usuario.getId()));
        } catch (RuntimeException e) {
            if ("No hay llamada activa en esta comunidad".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(
                                MessageResponse.builder()
                                        .message("No hay llamada activa en esta comunidad")
                                        .build());
            }
            throw e;
        }
    }

    @GetMapping("/communities/{communityId}/recordings")
    @Operation(summary = "Listar grabaciones de la comunidad")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Grabaciones listadas"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "No pertenece a la comunidad")
    })
    public ResponseEntity<?> listRecordings(
            @PathVariable Long communityId, @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(MessageResponse.builder().message("Usuario no autenticado").build());
        }
        if (!authorizationService.isMemberOf(usuario.getId(), communityId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(
                            MessageResponse.builder()
                                    .message("No perteneces a esta comunidad")
                                    .build());
        }

        return ResponseEntity.ok(
                zoomIntegrationService.getRecordingsForCommunity(communityId, usuario.getId()));
    }

    @GetMapping("/me/calls")
    @Operation(summary = "Saber en que llamada esta el usuario")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Llamadas activas listadas"),
        @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public ResponseEntity<?> getMyActiveCalls(@AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(MessageResponse.builder().message("Usuario no autenticado").build());
        }

        return ResponseEntity.ok(zoomIntegrationService.getActiveCallsForUser(usuario.getId()));
    }

    @PostMapping("/webhook")
    @Operation(
            summary = "Webhook de Zoom",
            description =
                    "Recibe eventos de Zoom para sincronizar participantes y grabaciones de"
                            + " llamadas")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Webhook procesado"),
        @ApiResponse(responseCode = "401", description = "Webhook no autorizado")
    })
    public ResponseEntity<Map<String, Object>> zoomWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "authorization", required = false) String authorization) {

        try {
            return ResponseEntity.ok(zoomIntegrationService.processWebhook(payload, authorization));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    private ZoomMeetingResponse toResponse(final ZoomMeeting meeting) {
        return new ZoomMeetingResponse(
                meeting.getId(),
                meeting.getZoomMeetingId(),
                meeting.getTopic(),
                meeting.getJoinUrl(),
                meeting.getPassword(),
                meeting.getStatus().name(),
                meeting.getComunidad().getId(),
                meeting.getComunidad().getNombre());
    }
}
