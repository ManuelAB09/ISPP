package es.us.meerkat.backend.service.zoom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import es.us.meerkat.backend.entity.communities.Comunidad;
import es.us.meerkat.backend.entity.communities.MiembroComunidad;
import es.us.meerkat.backend.entity.communities.RolComunidad;
import es.us.meerkat.backend.entity.communities.TipoGrupo;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.entity.zoom.ZoomMeeting;
import es.us.meerkat.backend.entity.zoom.ZoomMeetingParticipant;
import es.us.meerkat.backend.entity.zoom.ZoomMeetingStatus;
import es.us.meerkat.backend.entity.zoom.ZoomRecording;
import es.us.meerkat.backend.repository.communities.ComunidadRepository;
import es.us.meerkat.backend.repository.communities.MiembroComunidadRepository;
import es.us.meerkat.backend.repository.users.UsuarioRepository;
import es.us.meerkat.backend.repository.zoom.ZoomMeetingParticipantRepository;
import es.us.meerkat.backend.repository.zoom.ZoomMeetingRepository;
import es.us.meerkat.backend.repository.zoom.ZoomRecordingRepository;
import es.us.meerkat.backend.service.zoom.ZoomIntegrationService;
import es.us.meerkat.backend.service.zoom.ZoomRecordingStorageService;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class ZoomIntegrationServiceTest {

    @Autowired private ZoomIntegrationService service;
    @Autowired private ZoomRecordingStorageService zoomRecordingStorageService;

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ComunidadRepository comunidadRepository;
    @Autowired private MiembroComunidadRepository miembroComunidadRepository;
    @Autowired private ZoomMeetingRepository zoomMeetingRepository;
    @Autowired private ZoomMeetingParticipantRepository participantRepository;
    @Autowired private ZoomRecordingRepository recordingRepository;

    private Long memberUserId;
    private Long outsiderUserId;
    private Long comunidadId;

    @BeforeEach
    void setUp() throws Exception {
        Usuario member = createUser("member@test.com", "Member");
        Usuario outsider = createUser("outsider@test.com", "Outsider");
        Comunidad comunidad = createCommunity("Comunidad Zoom Test");
        createMembership(member, comunidad, RolComunidad.ADMIN);

        memberUserId = member.getId();
        outsiderUserId = outsider.getId();
        comunidadId = comunidad.getId();

        ReflectionTestUtils.setField(service, "zoomClientId", "test-client-id");
        ReflectionTestUtils.setField(service, "zoomClientSecret", "test-client-secret");
        ReflectionTestUtils.setField(service, "zoomAccountId", "test-account-id");
        ReflectionTestUtils.setField(service, "zoomWebhookSecretToken", "");
        ReflectionTestUtils.setField(service, "zoomApiBaseUrl", "https://api.zoom.us/v2");

        ReflectionTestUtils.setField(service, "restTemplate", new FakeZoomRestTemplate());

        Path tempStorage = Files.createTempDirectory("zoom-recordings-test-");
        ReflectionTestUtils.setField(zoomRecordingStorageService, "storageMode", "local");
        ReflectionTestUtils.setField(
                zoomRecordingStorageService, "localStoragePath", tempStorage.toString());
    }

    @Test
    void createOrGetActiveMeeting_shouldThrowWhenUserIsNotMember() {
        assertThatThrownBy(
                        () ->
                                service.createOrGetActiveMeeting(
                                        comunidadId, outsiderUserId, null, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("No perteneces a esta comunidad");
    }

    @Test
    void createOrGetActiveMeeting_shouldCreateWhenNoActiveMeeting() {
        ZoomMeeting result =
                service.createOrGetActiveMeeting(comunidadId, memberUserId, "Sesion Zoom", 45);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getStatus()).isEqualTo(ZoomMeetingStatus.ACTIVE);
        assertThat(result.getTopic()).isEqualTo("Sesion Zoom");
        assertThat(result.getDurationMinutes()).isEqualTo(45);
        assertThat(result.getJoinUrl()).contains("zoom.us/j/");
    }

    @Test
    void createOrGetActiveMeeting_shouldReuseActiveMeeting() {
        ZoomMeeting first =
                service.createOrGetActiveMeeting(comunidadId, memberUserId, "Topic A", null);

        ZoomMeeting second =
                service.createOrGetActiveMeeting(comunidadId, memberUserId, "Topic B", 30);

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(zoomMeetingRepository.findByComunidadIdOrderByCreatedAtDesc(comunidadId))
                .hasSize(1);
    }

    @Test
    void createOrGetActiveMeeting_shouldThrowWhenDurationIsNegative() {
        assertThatThrownBy(
                        () ->
                                service.createOrGetActiveMeeting(
                                        comunidadId, memberUserId, "Topic", -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("durationMinutes no puede ser menor que cero");
    }

    @Test
    void createOrGetActiveMeeting_shouldSetDefaultTopicAndNullDurationWhenNotProvided() {
        ZoomMeeting result =
                service.createOrGetActiveMeeting(comunidadId, memberUserId, null, null);

        assertThat(result.getTopic()).isEqualTo("Llamada de Comunidad Zoom Test");
        assertThat(result.getDurationMinutes()).isNull();
    }

    @Test
    void getActiveMeeting_shouldReturnMeetingForMember() {
        ZoomMeeting created =
                service.createOrGetActiveMeeting(comunidadId, memberUserId, "Sesion", null);

        ZoomMeeting found = service.getActiveMeeting(comunidadId, memberUserId);

        assertThat(found.getId()).isEqualTo(created.getId());
    }

    @Test
    void endActiveMeeting_shouldEndMeetingForCreator() {
        ZoomMeeting created =
                service.createOrGetActiveMeeting(comunidadId, memberUserId, "Sesion", null);

        service.endActiveMeeting(comunidadId, memberUserId);

        ZoomMeeting ended = zoomMeetingRepository.findById(created.getId()).orElseThrow();
        assertThat(ended.getStatus()).isEqualTo(ZoomMeetingStatus.ENDED);
        assertThat(ended.getEndedAt()).isNotNull();
    }

    @Test
    void getMeetingsForCommunity_shouldReturnHistory() {
        service.createOrGetActiveMeeting(comunidadId, memberUserId, "Sesion", null);

        List<ZoomMeeting> meetings = service.getMeetingsForCommunity(comunidadId, memberUserId);

        assertThat(meetings).hasSize(1);
    }

    @Test
    void joinActiveMeeting_shouldCreatePendingParticipantAndReturnCredentials() {
        ZoomMeeting meeting =
                service.createOrGetActiveMeeting(comunidadId, memberUserId, "Sesion", null);

        var response = service.joinActiveMeeting(comunidadId, memberUserId);

        assertThat(response.zoomMeetingId()).isEqualTo(meeting.getZoomMeetingId());
        assertThat(response.joinUrl()).isEqualTo(meeting.getJoinUrl());
        assertThat(
                        participantRepository.findByZoomMeetingIdAndInCallTrueOrderByJoinedAtAsc(
                                meeting.getId()))
                .isEmpty();
        assertThat(
                        participantRepository
                                .findFirstByZoomMeetingIdAndUsuarioIdOrderByJoinedAtDesc(
                                        meeting.getId(), memberUserId))
                .isPresent();
    }

    @Test
    void getActiveParticipants_shouldReturnOnlyInCallParticipants() {
        ZoomMeeting meeting =
                service.createOrGetActiveMeeting(comunidadId, memberUserId, "Sesion", null);
        ZoomMeetingParticipant participant =
                participantRepository
                        .findFirstByZoomMeetingIdAndUsuarioIdOrderByJoinedAtDesc(
                                meeting.getId(), memberUserId)
                        .orElseGet(
                                () -> {
                                    ZoomMeetingParticipant p = new ZoomMeetingParticipant();
                                    p.setZoomMeeting(meeting);
                                    p.setUsuario(
                                            usuarioRepository.findById(memberUserId).orElseThrow());
                                    p.setZoomParticipantId("p-1");
                                    p.setDisplayName("Member");
                                    p.setEmail("member@test.com");
                                    p.setJoinedAt(LocalDateTime.now());
                                    p.setInCall(false);
                                    return participantRepository.save(p);
                                });

        participant.setInCall(true);
        participantRepository.save(participant);

        var activeParticipants = service.getActiveParticipants(comunidadId, memberUserId);

        assertThat(activeParticipants).hasSize(1);
        assertThat(activeParticipants.get(0).userId()).isEqualTo(memberUserId);
        assertThat(activeParticipants.get(0).inCall()).isTrue();
    }

    @Test
    void getActiveCallsForUser_shouldReturnActiveCalls() {
        ZoomMeeting meeting =
                service.createOrGetActiveMeeting(comunidadId, memberUserId, "Sesion", null);
        ZoomMeetingParticipant participant = new ZoomMeetingParticipant();
        participant.setZoomMeeting(meeting);
        participant.setUsuario(usuarioRepository.findById(memberUserId).orElseThrow());
        participant.setZoomParticipantId("participant-active");
        participant.setDisplayName("Member");
        participant.setEmail("member@test.com");
        participant.setJoinedAt(LocalDateTime.now().minusMinutes(3));
        participant.setInCall(true);
        participantRepository.save(participant);

        var calls = service.getActiveCallsForUser(memberUserId);

        assertThat(calls).hasSize(1);
        assertThat(calls.get(0).communityId()).isEqualTo(comunidadId);
        assertThat(calls.get(0).zoomMeetingId()).isEqualTo(meeting.getZoomMeetingId());
    }

    @Test
    void uploadAndDownloadRecording_shouldWorkWithLocalStorage() {
        ZoomMeeting meeting =
                service.createOrGetActiveMeeting(comunidadId, memberUserId, "Sesion", null);

        MultipartFile file = new InMemoryMultipartFile("recording.mp4", "video/mp4", "abc123");

        var upload =
                service.uploadRecordingForMeeting(comunidadId, meeting.getId(), memberUserId, file);

        assertThat(upload.zoomRecordingId()).isNotBlank();
        assertThat(upload.storedInApp()).isTrue();
        assertThat(upload.fileType()).isEqualTo("MP4");

        var download =
                service.downloadRecordingForCommunity(
                        comunidadId, upload.zoomRecordingId(), memberUserId);

        assertThat(new String(download.content(), StandardCharsets.UTF_8)).isEqualTo("abc123");
        assertThat(download.mimeType()).isEqualTo("video/mp4");
    }

    @Test
    void cleanupExpiredRecordings_shouldRemoveExpiredStoredRecording() {
        ZoomMeeting meeting =
                service.createOrGetActiveMeeting(comunidadId, memberUserId, "Sesion", null);
        MultipartFile file = new InMemoryMultipartFile("recording.mp4", "video/mp4", "abc123");

        var upload =
                service.uploadRecordingForMeeting(comunidadId, meeting.getId(), memberUserId, file);
        ZoomRecording recording =
                recordingRepository.findByZoomRecordingId(upload.zoomRecordingId()).orElseThrow();
        recording.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        recordingRepository.save(recording);

        service.cleanupExpiredRecordings();

        assertThat(recordingRepository.findByZoomRecordingId(upload.zoomRecordingId())).isEmpty();
    }

    @Test
    void processWebhook_shouldHandleMeetingEnded() {
        ZoomMeeting meeting =
                service.createOrGetActiveMeeting(comunidadId, memberUserId, "Sesion", null);

        Map<String, Object> payload = webhookPayload("meeting.ended", meeting.getZoomMeetingId());

        Map<String, Object> response = service.processWebhook(payload, null, null);

        assertThat(response).containsEntry("status", "ok");
        ZoomMeeting updated = zoomMeetingRepository.findById(meeting.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ZoomMeetingStatus.ENDED);
    }

    @Test
    void processWebhook_shouldHandleParticipantJoinedAndParticipantLeft() {
        ZoomMeeting meeting =
                service.createOrGetActiveMeeting(comunidadId, memberUserId, "Sesion", null);

        Map<String, Object> joinPayload = new HashMap<>();
        joinPayload.put("event", "meeting.participant_joined");
        Map<String, Object> joinEventPayload = new HashMap<>();
        Map<String, Object> joinObject = new HashMap<>();
        joinObject.put("id", meeting.getZoomMeetingId());
        Map<String, Object> joinParticipant = new HashMap<>();
        joinParticipant.put("id", "zoom-participant-1");
        joinParticipant.put("user_name", "Member");
        joinParticipant.put("email", "member@test.com");
        joinObject.put("participant", joinParticipant);
        joinEventPayload.put("object", joinObject);
        joinPayload.put("payload", joinEventPayload);

        service.processWebhook(joinPayload, null, null);

        ZoomMeetingParticipant createdParticipant =
                participantRepository
                        .findFirstByZoomMeetingZoomMeetingIdAndZoomParticipantIdAndInCallTrueOrderByJoinedAtDesc(
                                meeting.getZoomMeetingId(), "zoom-participant-1")
                        .orElseThrow();
        assertThat(createdParticipant.getInCall()).isTrue();

        Map<String, Object> leftPayload = new HashMap<>();
        leftPayload.put("event", "meeting.participant_left");
        Map<String, Object> leftEventPayload = new HashMap<>();
        Map<String, Object> leftObject = new HashMap<>();
        leftObject.put("id", meeting.getZoomMeetingId());
        Map<String, Object> leftParticipant = new HashMap<>();
        leftParticipant.put("id", "zoom-participant-1");
        leftObject.put("participant", leftParticipant);
        leftEventPayload.put("object", leftObject);
        leftPayload.put("payload", leftEventPayload);

        service.processWebhook(leftPayload, null, null);

        ZoomMeetingParticipant updatedParticipant =
                participantRepository
                        .findFirstByZoomMeetingIdAndUsuarioIdOrderByJoinedAtDesc(
                                meeting.getId(), memberUserId)
                        .orElseThrow();
        assertThat(updatedParticipant.getInCall()).isFalse();
    }

    @Test
    void processWebhook_shouldThrowUnauthorizedWhenSignatureIsInvalidAndSecretConfigured() {
        ReflectionTestUtils.setField(service, "zoomWebhookSecretToken", "secret-enabled");

        ZoomMeeting meeting =
                service.createOrGetActiveMeeting(comunidadId, memberUserId, "Sesion", null);
        Map<String, Object> payload = webhookPayload("meeting.ended", meeting.getZoomMeetingId());

        assertThatThrownBy(() -> service.processWebhook(payload, "v0=invalid", "123456"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Webhook no autorizado");
    }

    private Usuario createUser(final String email, final String nombre) {
        Usuario u = new Usuario();
        u.setEmail(email);
        u.setPassword("password");
        u.setNombre(nombre);
        return usuarioRepository.save(u);
    }

    private Comunidad createCommunity(final String name) {
        Comunidad c = new Comunidad();
        c.setNombre(name);
        c.setDescripcion("desc");
        c.setTipoGrupo(TipoGrupo.GRUPO_PRIVADO);
        return comunidadRepository.save(c);
    }

    private void createMembership(
            final Usuario usuario, final Comunidad comunidad, final RolComunidad rol) {
        MiembroComunidad m = new MiembroComunidad();
        m.setUsuario(usuario);
        m.setComunidad(comunidad);
        m.setRol(rol);
        miembroComunidadRepository.save(m);
    }

    private Map<String, Object> webhookPayload(final String event, final String zoomMeetingId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("event", event);

        Map<String, Object> eventPayload = new HashMap<>();
        Map<String, Object> object = new HashMap<>();
        object.put("id", zoomMeetingId);
        eventPayload.put("object", object);

        payload.put("payload", eventPayload);
        return payload;
    }

    private static final class FakeZoomRestTemplate extends RestTemplate {

        @Override
        public <T> ResponseEntity<T> exchange(
                final String url,
                final HttpMethod method,
                final HttpEntity<?> requestEntity,
                final Class<T> responseType,
                final Object... uriVariables) {
            if (url.contains("/oauth/token")
                    && HttpMethod.POST.equals(method)
                    && responseType == Map.class) {
                Map<String, Object> body = Map.of("access_token", "fake-access-token");
                @SuppressWarnings("unchecked")
                T casted = (T) body;
                return new ResponseEntity<>(casted, HttpStatusCode.valueOf(200));
            }
            throw new RuntimeException("Unexpected exchange call in FakeZoomRestTemplate: " + url);
        }

        @Override
        public <T> ResponseEntity<T> postForEntity(
                final String url,
                final Object request,
                final Class<T> responseType,
                final Object... uriVariables) {
            if (url.contains("/users/me/meetings") && responseType == Map.class) {
                Map<String, Object> body = new HashMap<>();
                body.put("id", 555001L);
                body.put("join_url", "https://zoom.us/j/555001");
                body.put("start_url", "https://zoom.us/s/555001");
                body.put("password", "pass123456");
                @SuppressWarnings("unchecked")
                T casted = (T) body;
                return new ResponseEntity<>(casted, HttpStatusCode.valueOf(201));
            }
            throw new RuntimeException(
                    "Unexpected postForEntity call in FakeZoomRestTemplate: " + url);
        }
    }

    private static final class InMemoryMultipartFile implements MultipartFile {

        private final String filename;
        private final String contentType;
        private final byte[] content;

        private InMemoryMultipartFile(
                final String filename, final String contentType, final String content) {
            this.filename = filename;
            this.contentType = contentType;
            this.content = content.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public String getName() {
            return "file";
        }

        @Override
        public String getOriginalFilename() {
            return filename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return content.length == 0;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public byte[] getBytes() {
            return content;
        }

        @Override
        public java.io.InputStream getInputStream() {
            return new java.io.ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(final java.io.File dest) throws java.io.IOException {
            Files.write(dest.toPath(), content);
        }
    }
}
