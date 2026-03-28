package es.us.meerkat.backend.controller;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import es.us.meerkat.backend.dto.CreateZoomMeetingRequest;
import es.us.meerkat.backend.dto.MessageResponse;
import es.us.meerkat.backend.dto.ZoomMeetingResponse;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.entity.ZoomMeeting;
import es.us.meerkat.backend.service.communities.AuthorizationService;
import es.us.meerkat.backend.service.zoom.ZoomIntegrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
        @ApiResponse(
                responseCode = "401",
                description = "No autenticado",
                content = @Content(schema = @Schema(implementation = MessageResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "No pertenece a la comunidad",
                content = @Content(schema = @Schema(implementation = MessageResponse.class))),
        @ApiResponse(
                responseCode = "503",
                description = "Zoom no configurado en backend",
                content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<?> createOrGetMeeting(
            @PathVariable Long communityId,
            @RequestBody(required = false) CreateZoomMeetingRequest request,
            @AuthenticationPrincipal Usuario usuario) {

        ResponseEntity<?> authError = validateMembership(usuario, communityId);
        if (authError != null) {
            return authError;
        }

        try {
            ZoomMeeting meeting =
                    zoomIntegrationService.createOrGetActiveMeeting(
                            communityId,
                            usuario.getId(),
                            request != null ? request.topic() : null,
                            request != null ? request.durationMinutes() : null);
            return ResponseEntity.ok(toResponse(meeting, usuario));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(MessageResponse.builder().message(e.getMessage()).build());
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

        ResponseEntity<?> authError = validateMembership(usuario, communityId);
        if (authError != null) {
            return authError;
        }

        try {
            return ResponseEntity.ok(
                    toResponse(
                            zoomIntegrationService.getActiveMeeting(communityId, usuario.getId()),
                            usuario));
        } catch (RuntimeException e) {
            if ("No hay llamada activa en esta comunidad".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(MessageResponse.builder().message(e.getMessage()).build());
            }
            throw e;
        }
    }

    @PostMapping("/communities/{communityId}/meeting/join")
    @Operation(
            summary = "Entrar en la llamada",
            description = "Devuelve link y clave de acceso y registra que el usuario entro")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Acceso a llamada devuelto"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "No pertenece a la comunidad"),
        @ApiResponse(responseCode = "404", description = "No hay llamada activa")
    })
    public ResponseEntity<?> joinMeeting(
            @PathVariable Long communityId, @AuthenticationPrincipal Usuario usuario) {

        ResponseEntity<?> authError = validateMembership(usuario, communityId);
        if (authError != null) {
            return authError;
        }

        try {
            return ResponseEntity.ok(
                    zoomIntegrationService.joinActiveMeeting(communityId, usuario.getId()));
        } catch (RuntimeException e) {
            if ("No hay llamada activa en esta comunidad".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(MessageResponse.builder().message(e.getMessage()).build());
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

        ResponseEntity<?> authError = validateMembership(usuario, communityId);
        if (authError != null) {
            return authError;
        }

        try {
            return ResponseEntity.ok(
                    zoomIntegrationService.getActiveParticipants(communityId, usuario.getId()));
        } catch (RuntimeException e) {
            if ("No hay llamada activa en esta comunidad".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(MessageResponse.builder().message(e.getMessage()).build());
            }
            throw e;
        }
    }

    @GetMapping("/communities/{communityId}/meetings")
    @Operation(summary = "Listar historico de reuniones de la comunidad")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reuniones listadas"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "No pertenece a la comunidad")
    })
    public ResponseEntity<?> listMeetings(
            @PathVariable Long communityId, @AuthenticationPrincipal Usuario usuario) {

        ResponseEntity<?> authError = validateMembership(usuario, communityId);
        if (authError != null) {
            return authError;
        }

        return ResponseEntity.ok(
                zoomIntegrationService
                        .getMeetingsForCommunity(communityId, usuario.getId())
                        .stream()
                        .map(meeting -> toResponse(meeting, usuario))
                        .toList());
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

        ResponseEntity<?> authError = validateMembership(usuario, communityId);
        if (authError != null) {
            return authError;
        }

        return ResponseEntity.ok(
                zoomIntegrationService.getRecordingsForCommunity(communityId, usuario.getId()));
    }

    @GetMapping("/communities/{communityId}/recordings/{recordingId}")
    @Operation(summary = "Obtener detalle de una grabacion de la comunidad")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Grabacion encontrada"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "No pertenece a la comunidad"),
        @ApiResponse(responseCode = "404", description = "Grabacion no encontrada")
    })
    public ResponseEntity<?> getRecording(
            @PathVariable Long communityId,
            @PathVariable String recordingId,
            @AuthenticationPrincipal Usuario usuario) {

        ResponseEntity<?> authError = validateMembership(usuario, communityId);
        if (authError != null) {
            return authError;
        }

        try {
            return ResponseEntity.ok(
                    zoomIntegrationService.getRecordingForCommunity(
                            communityId, recordingId, usuario.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(MessageResponse.builder().message(e.getMessage()).build());
        }
    }

    @PostMapping(
            value = "/communities/{communityId}/meetings/{meetingId}/recordings/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir manualmente una grabacion para una reunion")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Grabacion subida"),
        @ApiResponse(responseCode = "400", description = "Archivo invalido"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "No pertenece a la comunidad"),
        @ApiResponse(responseCode = "404", description = "Reunion no encontrada")
    })
    public ResponseEntity<?> uploadRecording(
            @PathVariable Long communityId,
            @PathVariable Long meetingId,
            @AuthenticationPrincipal Usuario usuario,
            @RequestParam("file") MultipartFile file) {

        ResponseEntity<?> authError = validateMembership(usuario, communityId);
        if (authError != null) {
            return authError;
        }

        try {
            return ResponseEntity.ok(
                    zoomIntegrationService.uploadRecordingForMeeting(
                            communityId, meetingId, usuario.getId(), file));
        } catch (RuntimeException e) {
            if ("Reunion no encontrada".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(MessageResponse.builder().message(e.getMessage()).build());
            }
            return ResponseEntity.badRequest()
                    .body(MessageResponse.builder().message(e.getMessage()).build());
        }
    }

    @GetMapping("/communities/{communityId}/recordings/{recordingId}/download")
    @Operation(summary = "Descargar una grabacion de la comunidad")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Grabacion descargada"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "No pertenece a la comunidad"),
        @ApiResponse(responseCode = "404", description = "Grabacion no encontrada")
    })
    public ResponseEntity<?> downloadRecording(
            @PathVariable Long communityId,
            @PathVariable String recordingId,
            @AuthenticationPrincipal Usuario usuario) {

        ResponseEntity<?> authError = validateMembership(usuario, communityId);
        if (authError != null) {
            return authError;
        }

        try {
            ZoomIntegrationService.RecordingDownload recording =
                    zoomIntegrationService.downloadRecordingForCommunity(
                            communityId, recordingId, usuario.getId());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(recording.mimeType()))
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + recording.fileName() + "\"")
                    .body(recording.content());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(MessageResponse.builder().message(e.getMessage()).build());
        }
    }

    @DeleteMapping("/communities/{communityId}/meeting")
    @Operation(
            summary = "Finalizar llamada activa",
            description = "Solo el creador de la llamada puede finalizarla")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Llamada finalizada"),
        @ApiResponse(
                responseCode = "401",
                description = "No autenticado",
                content = @Content(schema = @Schema(implementation = MessageResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "No eres el creador de la llamada",
                content = @Content(schema = @Schema(implementation = MessageResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "No hay llamada activa",
                content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<?> endMeeting(
            @PathVariable Long communityId, @AuthenticationPrincipal Usuario usuario) {

        ResponseEntity<?> authError = validateMembership(usuario, communityId);
        if (authError != null) {
            return authError;
        }

        try {
            zoomIntegrationService.endActiveMeeting(communityId, usuario.getId());
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            if ("No hay llamada activa en esta comunidad".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(MessageResponse.builder().message(e.getMessage()).build());
            }
            if ("Solo el creador puede finalizar la llamada".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(MessageResponse.builder().message(e.getMessage()).build());
            }
            throw e;
        }
    }

    // ============================
    // ENDPOINTS DE EVENTOS
    // ============================

    @PostMapping("/events/{eventId}/meeting")
    @Operation(
            summary = "Crear o reutilizar llamada Zoom de evento",
            description =
                    "Si no existe una llamada activa para el evento, la crea. Si existe, todos"
                            + " reciben la misma sala")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Llamada creada u obtenida"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "No pertenece a la comunidad del evento"),
        @ApiResponse(responseCode = "503", description = "Zoom no configurado en backend")
    })
    public ResponseEntity<?> createOrGetEventMeeting(
            @PathVariable Long eventId,
            @RequestBody(required = false) CreateZoomMeetingRequest request,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(MessageResponse.builder().message("Usuario no autenticado").build());
        }

        try {
            ZoomMeeting meeting =
                    zoomIntegrationService.createOrGetActiveMeetingForEvent(
                            eventId,
                            usuario.getId(),
                            request != null ? request.topic() : null,
                            request != null ? request.durationMinutes() : null);
            return ResponseEntity.ok(toResponse(meeting, usuario));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(MessageResponse.builder().message(e.getMessage()).build());
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("Faltan credenciales Zoom")) {
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

    @GetMapping("/events/{eventId}/meeting")
    @Operation(summary = "Obtener llamada activa del evento")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Llamada activa encontrada"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "404", description = "No hay llamada activa")
    })
    public ResponseEntity<?> getActiveEventMeeting(
            @PathVariable Long eventId, @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(MessageResponse.builder().message("Usuario no autenticado").build());
        }

        try {
            return ResponseEntity.ok(
                    toResponse(
                            zoomIntegrationService.getActiveMeetingForEvent(
                                    eventId, usuario.getId()),
                            usuario));
        } catch (RuntimeException e) {
            if ("No hay llamada activa en este evento".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(MessageResponse.builder().message(e.getMessage()).build());
            }
            throw e;
        }
    }

    @PostMapping("/events/{eventId}/meeting/join")
    @Operation(
            summary = "Entrar en la llamada del evento",
            description = "Devuelve link y clave de acceso y registra que el usuario entro")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Acceso a llamada devuelto"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "404", description = "No hay llamada activa")
    })
    public ResponseEntity<?> joinEventMeeting(
            @PathVariable Long eventId, @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(MessageResponse.builder().message("Usuario no autenticado").build());
        }

        try {
            return ResponseEntity.ok(
                    zoomIntegrationService.joinActiveMeetingForEvent(eventId, usuario.getId()));
        } catch (RuntimeException e) {
            if ("No hay llamada activa en este evento".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(MessageResponse.builder().message(e.getMessage()).build());
            }
            throw e;
        }
    }

    @GetMapping("/events/{eventId}/meeting/participants")
    @Operation(summary = "Listar quien esta en la llamada activa del evento")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Participantes listados"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "404", description = "No hay llamada activa")
    })
    public ResponseEntity<?> listEventParticipants(
            @PathVariable Long eventId, @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(MessageResponse.builder().message("Usuario no autenticado").build());
        }

        try {
            return ResponseEntity.ok(
                    zoomIntegrationService.getActiveParticipantsForEvent(eventId, usuario.getId()));
        } catch (RuntimeException e) {
            if ("No hay llamada activa en este evento".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(MessageResponse.builder().message(e.getMessage()).build());
            }
            throw e;
        }
    }

    @DeleteMapping("/events/{eventId}/meeting")
    @Operation(
            summary = "Finalizar llamada activa del evento",
            description = "Solo el creador de la llamada puede finalizarla")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Llamada finalizada"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "No eres el creador de la llamada"),
        @ApiResponse(responseCode = "404", description = "No hay llamada activa")
    })
    public ResponseEntity<?> endEventMeeting(
            @PathVariable Long eventId, @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(MessageResponse.builder().message("Usuario no autenticado").build());
        }

        try {
            zoomIntegrationService.endActiveMeetingForEvent(eventId, usuario.getId());
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            if ("No hay llamada activa en este evento".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(MessageResponse.builder().message(e.getMessage()).build());
            }
            if ("Solo el creador puede finalizar la llamada".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(MessageResponse.builder().message(e.getMessage()).build());
            }
            throw e;
        }
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
    @Operation(summary = "Webhook de Zoom", description = "Recibe eventos de Zoom")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Webhook procesado"),
        @ApiResponse(responseCode = "401", description = "Webhook no autorizado")
    })
    public ResponseEntity<Map<String, Object>> zoomWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "x-zm-signature", required = false) String zmSignature,
            @RequestHeader(value = "x-zm-request-timestamp", required = false) String zmTimestamp) {

        try {
            return ResponseEntity.ok(
                    zoomIntegrationService.processWebhook(payload, zmSignature, zmTimestamp));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    private ResponseEntity<?> validateMembership(final Usuario usuario, final Long communityId) {
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
        return null;
    }

    private ZoomMeetingResponse toResponse(final ZoomMeeting meeting, final Usuario usuario) {
        String startUrl =
                usuario != null && meeting.getCreador().getId().equals(usuario.getId())
                        ? meeting.getStartUrl()
                        : null;
        return new ZoomMeetingResponse(
                meeting.getId(),
                meeting.getZoomMeetingId(),
                meeting.getTopic(),
                meeting.getJoinUrl(),
                startUrl,
                meeting.getPassword(),
                meeting.getStatus().name(),
                meeting.getComunidad().getId(),
                meeting.getComunidad().getNombre(),
                meeting.getDurationMinutes(),
                meeting.getCreatedAt(),
                meeting.getStartedAt(),
                meeting.getEndedAt());
    }
}
