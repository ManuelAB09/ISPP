package es.us.meerkat.backend.controller.zoom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import es.us.meerkat.backend.dto.zoom.CreateZoomMeetingRequest;
import es.us.meerkat.backend.dto.zoom.ZoomJoinResponse;
import es.us.meerkat.backend.dto.zoom.ZoomRecordingResponse;
import es.us.meerkat.backend.entity.communities.Comunidad;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.entity.zoom.ZoomMeeting;
import es.us.meerkat.backend.service.communities.AuthorizationService;
import es.us.meerkat.backend.service.zoom.ZoomIntegrationService;

@ExtendWith(MockitoExtension.class)
class ZoomControllerTest {

    @Mock private ZoomIntegrationService zoomIntegrationService;
    @Mock private AuthorizationService authorizationService;

    @InjectMocks private ZoomController controller;

    private Usuario usuario;
    private ZoomMeeting meeting;
    private Comunidad community;
    private Long communityId;
    private Long meetingId;

    @BeforeEach
    void setUp() {
        usuario = buildUsuario(1L);
        communityId = 100L;
        meetingId = 50L;
        community = new Comunidad();
        community.setId(communityId);
        meeting = new ZoomMeeting();
        meeting.setId(meetingId);
        meeting.setZoomMeetingId("123456789");
        meeting.setCreador(usuario);
        meeting.setComunidad(community);
    }

    private Usuario buildUsuario(Long id) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setEmail("user@test.es");
        return u;
    }

    // ============ GET MY ACTIVE CALLS TESTS ============
    @Test
    void getMyActiveCallsShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> response = controller.getMyActiveCalls(null);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getMyActiveCallsShouldReturnOkWithActiveCalls() {
        when(zoomIntegrationService.getActiveCallsForUser(1L)).thenReturn(List.of());

        ResponseEntity<?> response = controller.getMyActiveCalls(usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(zoomIntegrationService).getActiveCallsForUser(1L);
    }

    @Test
    void getMyActiveCallsShouldReturnEmptyList() {
        when(zoomIntegrationService.getActiveCallsForUser(1L)).thenReturn(List.of());

        ResponseEntity<?> response = controller.getMyActiveCalls(usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ============ CREATE OR GET MEETING TESTS ============
    @Test
    void createOrGetMeetingShouldReturnMeetingWhenAuthorized() throws Exception {
        when(authorizationService.isMemberOf(usuario.getId(), communityId)).thenReturn(true);
        when(zoomIntegrationService.createOrGetActiveMeeting(
                        eq(communityId), eq(1L), anyString(), anyInt()))
                .thenReturn(meeting);

        CreateZoomMeetingRequest request = new CreateZoomMeetingRequest("Test Meeting", 60);
        ResponseEntity<?> response = controller.createOrGetMeeting(communityId, request, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void createOrGetMeetingShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> response = controller.createOrGetMeeting(communityId, null, null);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void createOrGetMeetingShouldReturnForbiddenWhenNotMember() throws Exception {
        when(authorizationService.isMemberOf(usuario.getId(), communityId)).thenReturn(false);

        ResponseEntity<?> response = controller.createOrGetMeeting(communityId, null, usuario);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void createOrGetMeetingShouldReturnServiceUnavailableWhenZoomNotConfigured() throws Exception {
        when(authorizationService.isMemberOf(usuario.getId(), communityId)).thenReturn(true);
        when(zoomIntegrationService.createOrGetActiveMeeting(eq(communityId), eq(1L), any(), any()))
                .thenThrow(
                        new RuntimeException(
                                "Faltan credenciales Zoom. Revisa zoom.client-id,"
                                        + " zoom.client-secret y zoom.account-id"));

        ResponseEntity<?> response = controller.createOrGetMeeting(communityId, null, usuario);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void createOrGetMeetingShouldReturnBadRequestOnIllegalArgument() throws Exception {
        when(authorizationService.isMemberOf(usuario.getId(), communityId)).thenReturn(true);
        when(zoomIntegrationService.createOrGetActiveMeeting(eq(communityId), eq(1L), any(), any()))
                .thenThrow(new IllegalArgumentException("Invalid request"));

        ResponseEntity<?> response = controller.createOrGetMeeting(communityId, null, usuario);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ============ GET ACTIVE MEETING TESTS ============
    @Test
    void getActiveMeetingShouldReturnMeetingWhenExists() throws Exception {
        when(authorizationService.isMemberOf(usuario.getId(), communityId)).thenReturn(true);
        when(zoomIntegrationService.getActiveMeeting(communityId, 1L)).thenReturn(meeting);

        ResponseEntity<?> response = controller.getActiveMeeting(communityId, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getActiveMeetingShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> response = controller.getActiveMeeting(communityId, null);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getActiveMeetingShouldReturnNotFoundWhenNoActiveMeeting() throws Exception {
        when(authorizationService.isMemberOf(usuario.getId(), communityId)).thenReturn(true);
        when(zoomIntegrationService.getActiveMeeting(communityId, 1L))
                .thenThrow(new RuntimeException("No hay llamada activa en esta comunidad"));

        ResponseEntity<?> response = controller.getActiveMeeting(communityId, usuario);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ============ JOIN MEETING TESTS ============
    @Test
    void joinMeetingShouldReturnAccessDetailsWhenSuccessful() throws Exception {
        when(authorizationService.isMemberOf(usuario.getId(), communityId)).thenReturn(true);
        ZoomJoinResponse zoomResponse =
                new ZoomJoinResponse(
                        "zoom_123", "Test Topic", "https://zoom.us/j/123", "password123");
        when(zoomIntegrationService.joinActiveMeeting(communityId, 1L)).thenReturn(zoomResponse);

        ResponseEntity<?> response = controller.joinMeeting(communityId, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(zoomIntegrationService).joinActiveMeeting(communityId, 1L);
    }

    @Test
    void joinMeetingShouldReturnUnauthorizedWhenUserIsNull() throws Exception {
        ResponseEntity<?> response = controller.joinMeeting(communityId, null);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void joinMeetingShouldReturnNotFoundWhenNoActiveMeeting() throws Exception {
        when(authorizationService.isMemberOf(usuario.getId(), communityId)).thenReturn(true);
        when(zoomIntegrationService.joinActiveMeeting(communityId, 1L))
                .thenThrow(new RuntimeException("No hay llamada activa en esta comunidad"));

        ResponseEntity<?> response = controller.joinMeeting(communityId, usuario);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ============ LIST PARTICIPANTS TESTS ============
    @Test
    void listParticipantsShouldReturnParticipantList() throws Exception {
        when(authorizationService.isMemberOf(usuario.getId(), communityId)).thenReturn(true);
        when(zoomIntegrationService.getActiveParticipants(communityId, 1L)).thenReturn(List.of());

        ResponseEntity<?> response = controller.listParticipants(communityId, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void listParticipantsShouldReturnUnauthorizedWhenUserIsNull() throws Exception {
        ResponseEntity<?> response = controller.listParticipants(communityId, null);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void listParticipantsShouldReturnNotFoundWhenNoActiveMeeting() throws Exception {
        when(authorizationService.isMemberOf(usuario.getId(), communityId)).thenReturn(true);
        when(zoomIntegrationService.getActiveParticipants(communityId, 1L))
                .thenThrow(new RuntimeException("No hay llamada activa en esta comunidad"));

        ResponseEntity<?> response = controller.listParticipants(communityId, usuario);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ============ LIST MEETINGS TESTS ============
    @Test
    void listMeetingsShouldReturnMeetingList() throws Exception {
        when(authorizationService.isMemberOf(usuario.getId(), communityId)).thenReturn(true);
        when(zoomIntegrationService.getMeetingsForCommunity(communityId, 1L))
                .thenReturn(List.of(meeting));

        ResponseEntity<?> response = controller.listMeetings(communityId, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void listMeetingsShouldReturnEmptyList() {
        when(authorizationService.isMemberOf(usuario.getId(), communityId)).thenReturn(true);
        when(zoomIntegrationService.getMeetingsForCommunity(communityId, 1L)).thenReturn(List.of());

        ResponseEntity<?> response = controller.listMeetings(communityId, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ============ LIST RECORDINGS TESTS ============
    @Test
    void listRecordingsShouldReturnRecordingList() throws Exception {
        when(authorizationService.isMemberOf(usuario.getId(), communityId)).thenReturn(true);
        when(zoomIntegrationService.getRecordingsForCommunity(communityId, 1L))
                .thenReturn(List.of());

        ResponseEntity<?> response = controller.listRecordings(communityId, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void listRecordingsShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> response = controller.listRecordings(communityId, null);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ============ GET RECORDING TESTS ============
    @Test
    void getRecordingShouldReturnRecordingDetails() throws Exception {
        String recordingId = "rec_123";
        when(authorizationService.isMemberOf(usuario.getId(), communityId)).thenReturn(true);
        ZoomRecordingResponse recording =
                new ZoomRecordingResponse(
                        recordingId,
                        "zoom_123",
                        communityId,
                        "Comunidad",
                        "mp4",
                        "https://url",
                        "https://url",
                        true,
                        "https://url",
                        1000L,
                        null,
                        null,
                        null,
                        "inactive",
                        null);
        when(zoomIntegrationService.getRecordingForCommunity(communityId, recordingId, 1L))
                .thenReturn(recording);

        ResponseEntity<?> response = controller.getRecording(communityId, recordingId, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getRecordingShouldReturnNotFoundWhenRecordingNotExists() {
        String recordingId = "rec_notfound";
        when(authorizationService.isMemberOf(usuario.getId(), communityId)).thenReturn(true);
        when(zoomIntegrationService.getRecordingForCommunity(communityId, recordingId, 1L))
                .thenThrow(new RuntimeException("Recording not found"));

        ResponseEntity<?> response = controller.getRecording(communityId, recordingId, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ============ UPLOAD RECORDING TESTS ============
    @Test
    void uploadRecordingShouldReturnOkWhenSuccessful() throws Exception {
        when(authorizationService.isMemberOf(usuario.getId(), communityId)).thenReturn(true);

        MultipartFile file =
                new MockMultipartFile(
                        "file", "recording.mp4", "video/mp4", "video data".getBytes());
        ZoomRecordingResponse result =
                new ZoomRecordingResponse(
                        "rec_uploaded",
                        "zoom_123",
                        communityId,
                        "Comunidad",
                        "mp4",
                        "https://url",
                        "https://url",
                        true,
                        "https://url",
                        1000L,
                        null,
                        null,
                        null,
                        "active",
                        null);
        when(zoomIntegrationService.uploadRecordingForMeeting(communityId, meetingId, 1L, file))
                .thenReturn(result);

        ResponseEntity<?> response =
                controller.uploadRecording(communityId, meetingId, usuario, file);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void uploadRecordingShouldReturnNotFoundWhenMeetingNotExists() throws Exception {
        when(authorizationService.isMemberOf(usuario.getId(), communityId)).thenReturn(true);

        MultipartFile file =
                new MockMultipartFile(
                        "file", "recording.mp4", "video/mp4", "video data".getBytes());
        when(zoomIntegrationService.uploadRecordingForMeeting(communityId, meetingId, 1L, file))
                .thenThrow(new RuntimeException("Reunion no encontrada"));

        ResponseEntity<?> response =
                controller.uploadRecording(communityId, meetingId, usuario, file);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void uploadRecordingShouldReturnBadRequestOnInvalidFile() throws Exception {
        when(authorizationService.isMemberOf(usuario.getId(), communityId)).thenReturn(true);

        MultipartFile file =
                new MockMultipartFile(
                        "file", "recording.mp4", "video/mp4", "video data".getBytes());
        when(zoomIntegrationService.uploadRecordingForMeeting(communityId, meetingId, 1L, file))
                .thenThrow(new RuntimeException("Invalid file format"));

        ResponseEntity<?> response =
                controller.uploadRecording(communityId, meetingId, usuario, file);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void downloadRecordingShouldReturnBinaryBodyAndHeadersWhenSuccessful() {
        when(authorizationService.isMemberOf(usuario.getId(), communityId)).thenReturn(true);
        byte[] content = "recording-data".getBytes(StandardCharsets.UTF_8);
        ZoomIntegrationService.RecordingDownload download =
                new ZoomIntegrationService.RecordingDownload(content, "meeting.mp4", "video/mp4");

        when(zoomIntegrationService.downloadRecordingForCommunity(communityId, "rec_1", 1L))
                .thenReturn(download);

        ResponseEntity<?> response = controller.downloadRecording(communityId, "rec_1", usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().get(HttpHeaders.CONTENT_DISPOSITION).get(0))
                .contains("meeting.mp4");
        assertThat(response.getBody()).isEqualTo(content);
    }

    @Test
    void downloadRecordingShouldReturnNotFoundWhenRecordingDoesNotExist() {
        when(authorizationService.isMemberOf(usuario.getId(), communityId)).thenReturn(true);
        when(zoomIntegrationService.downloadRecordingForCommunity(communityId, "missing", 1L))
                .thenThrow(new RuntimeException("No encontrada"));

        ResponseEntity<?> response = controller.downloadRecording(communityId, "missing", usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void endMeetingShouldReturnForbiddenWhenUserIsNotCreator() {
        when(authorizationService.isMemberOf(usuario.getId(), communityId)).thenReturn(true);
        org.mockito.Mockito.doThrow(
                        new RuntimeException("Solo el creador puede finalizar la llamada"))
                .when(zoomIntegrationService)
                .endActiveMeeting(communityId, 1L);

        ResponseEntity<?> response = controller.endMeeting(communityId, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void endMeetingShouldReturnNotFoundWhenNoActiveMeeting() {
        when(authorizationService.isMemberOf(usuario.getId(), communityId)).thenReturn(true);
        org.mockito.Mockito.doThrow(new RuntimeException("No hay llamada activa en esta comunidad"))
                .when(zoomIntegrationService)
                .endActiveMeeting(communityId, 1L);

        ResponseEntity<?> response = controller.endMeeting(communityId, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createOrGetEventMeetingShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> response = controller.createOrGetEventMeeting(55L, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void createOrGetEventMeetingShouldReturnServiceUnavailableWhenZoomNotConfigured()
            throws Exception {
        when(zoomIntegrationService.createOrGetActiveMeetingForEvent(eq(66L), eq(1L), any(), any()))
                .thenThrow(new RuntimeException("Faltan credenciales Zoom en entorno"));

        ResponseEntity<?> response = controller.createOrGetEventMeeting(66L, null, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void getActiveEventMeetingShouldReturnNotFoundWhenNoMeetingExists() {
        when(zoomIntegrationService.getActiveMeetingForEvent(77L, 1L))
                .thenThrow(new RuntimeException("No hay llamada activa en este evento"));

        ResponseEntity<?> response = controller.getActiveEventMeeting(77L, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void joinEventMeetingShouldReturnNotFoundWhenNoMeetingExists() {
        when(zoomIntegrationService.joinActiveMeetingForEvent(88L, 1L))
                .thenThrow(new RuntimeException("No hay llamada activa en este evento"));

        ResponseEntity<?> response = controller.joinEventMeeting(88L, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void listEventParticipantsShouldReturnNotFoundWhenNoMeetingExists() {
        when(zoomIntegrationService.getActiveParticipantsForEvent(99L, 1L))
                .thenThrow(new RuntimeException("No hay llamada activa en este evento"));

        ResponseEntity<?> response = controller.listEventParticipants(99L, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void endEventMeetingShouldReturnForbiddenWhenNotCreator() {
        org.mockito.Mockito.doThrow(
                        new RuntimeException("Solo el creador puede finalizar la llamada"))
                .when(zoomIntegrationService)
                .endActiveMeetingForEvent(101L, 1L);

        ResponseEntity<?> response = controller.endEventMeeting(101L, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void zoomWebhookShouldReturnUnauthorizedWhenServiceThrowsError() {
        when(zoomIntegrationService.processWebhook(any(), eq("sig"), eq("ts")))
                .thenThrow(new RuntimeException("invalid webhook"));

        ResponseEntity<Map<String, Object>> response =
                controller.zoomWebhook(Map.of("type", "event"), "sig", "ts");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).containsEntry("error", "invalid webhook");
    }

    @Test
    void zoomWebhookShouldReturnOkWhenProcessedSuccessfully() {
        when(zoomIntegrationService.processWebhook(any(), eq(null), eq(null)))
                .thenReturn(Map.of("status", "ok", "challenge", "abc"));

        ResponseEntity<Map<String, Object>> response =
                controller.zoomWebhook(Map.of("event", "endpoint.url_validation"), null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "ok");
    }
}
