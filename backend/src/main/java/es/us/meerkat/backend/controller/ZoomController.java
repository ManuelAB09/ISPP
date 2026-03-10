package es.us.meerkat.backend.controller;

import java.util.List;
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
import es.us.meerkat.backend.dto.ZoomJoinResponse;
import es.us.meerkat.backend.dto.ZoomMeetingResponse;
import es.us.meerkat.backend.dto.ZoomParticipantResponse;
import es.us.meerkat.backend.dto.ZoomRecordingResponse;
import es.us.meerkat.backend.dto.ZoomUserCallResponse;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.entity.ZoomMeeting;
import es.us.meerkat.backend.service.AuthorizationService;
import es.us.meerkat.backend.service.ZoomIntegrationService;
import io.swagger.v3.oas.annotations.Operation;
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
    public ResponseEntity<ZoomMeetingResponse> createOrGetMeeting(
            @PathVariable Long communityId,
            @RequestBody(required = false) CreateZoomMeetingRequest request,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!authorizationService.isMemberOf(usuario.getId(), communityId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        ZoomMeeting meeting =
                zoomIntegrationService.createOrGetActiveMeeting(
                        communityId,
                        usuario.getId(),
                        request != null ? request.topic() : null,
                        request != null ? request.durationMinutes() : null);

        return ResponseEntity.ok(toResponse(meeting));
    }

    @GetMapping("/communities/{communityId}/meeting")
    @Operation(summary = "Obtener llamada activa de la comunidad")
    public ResponseEntity<ZoomMeetingResponse> getActiveMeeting(
            @PathVariable Long communityId, @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!authorizationService.isMemberOf(usuario.getId(), communityId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        ZoomMeeting meeting = zoomIntegrationService.getActiveMeeting(communityId, usuario.getId());
        return ResponseEntity.ok(toResponse(meeting));
    }

    @PostMapping("/communities/{communityId}/meeting/join")
    @Operation(
            summary = "Entrar en la llamada",
            description =
                    "Devuelve link y clave de acceso de forma automatica y registra que el usuario"
                            + " entro")
    public ResponseEntity<ZoomJoinResponse> joinMeeting(
            @PathVariable Long communityId, @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!authorizationService.isMemberOf(usuario.getId(), communityId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(
                zoomIntegrationService.joinActiveMeeting(communityId, usuario.getId()));
    }

    @GetMapping("/communities/{communityId}/meeting/participants")
    @Operation(summary = "Listar quien esta en la llamada activa")
    public ResponseEntity<List<ZoomParticipantResponse>> listParticipants(
            @PathVariable Long communityId, @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!authorizationService.isMemberOf(usuario.getId(), communityId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(
                zoomIntegrationService.getActiveParticipants(communityId, usuario.getId()));
    }

    @GetMapping("/communities/{communityId}/recordings")
    @Operation(summary = "Listar grabaciones de la comunidad")
    public ResponseEntity<List<ZoomRecordingResponse>> listRecordings(
            @PathVariable Long communityId, @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!authorizationService.isMemberOf(usuario.getId(), communityId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(
                zoomIntegrationService.getRecordingsForCommunity(communityId, usuario.getId()));
    }

    @GetMapping("/me/calls")
    @Operation(summary = "Saber en que llamada esta el usuario")
    public ResponseEntity<List<ZoomUserCallResponse>> getMyActiveCalls(
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(zoomIntegrationService.getActiveCallsForUser(usuario.getId()));
    }

    @PostMapping("/webhook")
    @Operation(
            summary = "Webhook de Zoom",
            description =
                    "Recibe eventos de Zoom para sincronizar participantes y grabaciones de"
                            + " llamadas")
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
