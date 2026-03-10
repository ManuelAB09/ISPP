package es.us.meerkat.backend.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import es.us.meerkat.backend.dto.ZoomJoinResponse;
import es.us.meerkat.backend.dto.ZoomParticipantResponse;
import es.us.meerkat.backend.dto.ZoomRecordingResponse;
import es.us.meerkat.backend.dto.ZoomUserCallResponse;
import es.us.meerkat.backend.entity.Comunidad;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.entity.ZoomMeeting;
import es.us.meerkat.backend.entity.ZoomMeetingParticipant;
import es.us.meerkat.backend.entity.ZoomMeetingStatus;
import es.us.meerkat.backend.entity.ZoomRecording;
import es.us.meerkat.backend.repository.ComunidadRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;
import es.us.meerkat.backend.repository.ZoomMeetingParticipantRepository;
import es.us.meerkat.backend.repository.ZoomMeetingRepository;
import es.us.meerkat.backend.repository.ZoomRecordingRepository;
import lombok.RequiredArgsConstructor;

/** Servicio principal para gestionar reuniones Zoom vinculadas a comunidades. */
@Service
@RequiredArgsConstructor
public class ZoomIntegrationService {

    private final ZoomMeetingRepository zoomMeetingRepository;
    private final ZoomMeetingParticipantRepository participantRepository;
    private final ZoomRecordingRepository recordingRepository;
    private final ComunidadRepository comunidadRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuthorizationService authorizationService;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${zoom.client-id:}")
    private String zoomClientId;

    @Value("${zoom.client-secret:}")
    private String zoomClientSecret;

    @Value("${zoom.account-id:}")
    private String zoomAccountId;

    @Value("${zoom.webhook-secret-token:}")
    private String zoomWebhookSecretToken;

    @Value("${zoom.api-base-url:https://api.zoom.us/v2}")
    private String zoomApiBaseUrl;

    @Value("${zoom.recordings.storage-path:storage/zoom-recordings}")
    private String zoomRecordingsStoragePath;

    /** Crea una reunion privada para una comunidad o devuelve la activa. */
    @Transactional
    public ZoomMeeting createOrGetActiveMeeting(
            final Long comunidadId,
            final Long userId,
            final String topicParam,
            final Integer durationMinutesParam) {

        assertMember(comunidadId, userId);

        Optional<ZoomMeeting> existing =
                zoomMeetingRepository.findFirstByComunidadIdAndStatusOrderByCreatedAtDesc(
                        comunidadId, ZoomMeetingStatus.ACTIVE);
        if (existing.isPresent()) {
            return existing.get();
        }

        Comunidad comunidad =
                comunidadRepository
                        .findById(comunidadId)
                        .orElseThrow(() -> new RuntimeException("Comunidad no encontrada"));

        Usuario user =
                usuarioRepository
                        .findById(userId)
                        .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String topic =
                (topicParam == null || topicParam.isBlank())
                        ? "Llamada de " + comunidad.getNombre()
                        : topicParam;
        Integer durationMinutes =
                durationMinutesParam == null || durationMinutesParam <= 0
                        ? 60
                        : durationMinutesParam;

        Map<String, Object> zoomMeeting = createZoomMeeting(topic, durationMinutes);

        ZoomMeeting meeting =
                ZoomMeeting.builder()
                        .comunidad(comunidad)
                        .creador(user)
                        .zoomMeetingId(String.valueOf(zoomMeeting.get("id")))
                        .topic(topic)
                        .joinUrl((String) zoomMeeting.get("join_url"))
                        .startUrl((String) zoomMeeting.get("start_url"))
                        .password((String) zoomMeeting.get("password"))
                        .status(ZoomMeetingStatus.ACTIVE)
                        .build();

        return zoomMeetingRepository.save(meeting);
    }

    /** Obtiene la reunion activa de una comunidad si el usuario pertenece a ella. */
    public ZoomMeeting getActiveMeeting(final Long comunidadId, final Long userId) {
        assertMember(comunidadId, userId);

        return zoomMeetingRepository
                .findFirstByComunidadIdAndStatusOrderByCreatedAtDesc(
                        comunidadId, ZoomMeetingStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("No hay llamada activa en esta comunidad"));
    }

    /** Devuelve el acceso a la reunion comun y registra presencia en la app. */
    @Transactional
    public ZoomJoinResponse joinActiveMeeting(final Long comunidadId, final Long userId) {
        ZoomMeeting meeting = getActiveMeeting(comunidadId, userId);

        Usuario user =
                usuarioRepository
                        .findById(userId)
                        .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        participantRepository
                .findFirstByZoomMeetingIdAndUsuarioIdAndInCallTrueOrderByJoinedAtDesc(
                        meeting.getId(), userId)
                .orElseGet(
                        () ->
                                participantRepository.save(
                                        ZoomMeetingParticipant.builder()
                                                .zoomMeeting(meeting)
                                                .usuario(user)
                                                .zoomParticipantId(
                                                        "app-"
                                                                + user.getId()
                                                                + "-"
                                                                + System.currentTimeMillis())
                                                .displayName(user.getNombre())
                                                .email(user.getEmail())
                                                .joinedAt(LocalDateTime.now())
                                                .inCall(true)
                                                .build()));

        return new ZoomJoinResponse(
                meeting.getZoomMeetingId(),
                meeting.getTopic(),
                meeting.getJoinUrl(),
                meeting.getPassword());
    }

    /** Lista participantes presentes en la llamada activa de la comunidad. */
    public List<ZoomParticipantResponse> getActiveParticipants(
            final Long comunidadId, final Long userId) {
        ZoomMeeting meeting = getActiveMeeting(comunidadId, userId);

        return participantRepository
                .findByZoomMeetingIdAndInCallTrueOrderByJoinedAtAsc(meeting.getId())
                .stream()
                .map(
                        p ->
                                new ZoomParticipantResponse(
                                        p.getUsuario() != null ? p.getUsuario().getId() : null,
                                        p.getDisplayName(),
                                        p.getEmail(),
                                        p.getInCall(),
                                        p.getJoinedAt(),
                                        p.getLeftAt()))
                .toList();
    }

    /** Devuelve en que llamadas esta actualmente el usuario autenticado. */
    public List<ZoomUserCallResponse> getActiveCallsForUser(final Long userId) {
        return participantRepository
                .findByUsuarioIdAndInCallTrueOrderByJoinedAtDesc(userId)
                .stream()
                .map(
                        p ->
                                new ZoomUserCallResponse(
                                        p.getZoomMeeting().getComunidad().getId(),
                                        p.getZoomMeeting().getComunidad().getNombre(),
                                        p.getZoomMeeting().getZoomMeetingId(),
                                        p.getZoomMeeting().getTopic(),
                                        p.getJoinedAt()))
                .toList();
    }

    /** Lista las grabaciones registradas para una comunidad. */
    public List<ZoomRecordingResponse> getRecordingsForCommunity(
            final Long comunidadId, final Long userId) {
        assertMember(comunidadId, userId);

        return recordingRepository
                .findByZoomMeetingComunidadIdOrderByCreatedAtDesc(comunidadId)
                .stream()
                .map(
                        r ->
                                new ZoomRecordingResponse(
                                        r.getZoomRecordingId(),
                                        r.getZoomMeeting().getZoomMeetingId(),
                                        r.getZoomMeeting().getComunidad().getId(),
                                        r.getZoomMeeting().getComunidad().getNombre(),
                                        r.getFileType(),
                                        r.getPlayUrl(),
                                        r.getDownloadUrl(),
                                        r.getStoredInApp(),
                                        r.getLocalFilePath(),
                                        r.getFileSizeBytes(),
                                        r.getRecordingStart(),
                                        r.getRecordingEnd(),
                                        r.getCreatedAt()))
                .toList();
    }

    /** Procesa webhooks de Zoom para participantes, fin de llamada y grabaciones. */
    @Transactional
    public Map<String, Object> processWebhook(
            final Map<String, Object> payload, final String authorizationHeader) {

        validateWebhookAuthorization(authorizationHeader);

        String event = (String) payload.get("event");
        if ("endpoint.url_validation".equals(event)) {
            return buildValidationResponse(payload);
        }

        Map<String, Object> eventPayload = getMap(payload, "payload");
        Map<String, Object> object = getMap(eventPayload, "object");

        String zoomMeetingId = String.valueOf(object.get("id"));

        switch (event) {
            case "meeting.participant_joined" -> handleParticipantJoined(zoomMeetingId, object);
            case "meeting.participant_left" -> handleParticipantLeft(zoomMeetingId, object);
            case "meeting.ended" -> handleMeetingEnded(zoomMeetingId);
            case "recording.completed" -> handleRecordingCompleted(zoomMeetingId, object);
            default -> {
                // Evento no gestionado por ahora.
            }
        }

        return Map.of("status", "ok");
    }

    private void handleParticipantJoined(
            final String zoomMeetingId, final Map<String, Object> object) {
        ZoomMeeting meeting =
                zoomMeetingRepository
                        .findByZoomMeetingId(zoomMeetingId)
                        .orElseThrow(() -> new RuntimeException("Reunion Zoom no encontrada"));

        Map<String, Object> participant = getMap(object, "participant");
        String participantId = String.valueOf(participant.get("id"));
        String displayName = (String) participant.getOrDefault("user_name", "Usuario Zoom");
        String email = (String) participant.get("email");

        Usuario user = email != null ? usuarioRepository.findByEmail(email).orElse(null) : null;

        participantRepository.save(
                ZoomMeetingParticipant.builder()
                        .zoomMeeting(meeting)
                        .usuario(user)
                        .zoomParticipantId(participantId)
                        .displayName(displayName)
                        .email(email)
                        .joinedAt(LocalDateTime.now())
                        .inCall(true)
                        .build());
    }

    private void handleParticipantLeft(
            final String zoomMeetingId, final Map<String, Object> object) {
        Map<String, Object> participant = getMap(object, "participant");
        String participantId = String.valueOf(participant.get("id"));

        participantRepository
                .findFirstByZoomMeetingZoomMeetingIdAndZoomParticipantIdAndInCallTrueOrderByJoinedAtDesc(
                        zoomMeetingId, participantId)
                .ifPresent(
                        p -> {
                            p.markLeft();
                            participantRepository.save(p);
                        });
    }

    private void handleMeetingEnded(final String zoomMeetingId) {
        zoomMeetingRepository
                .findByZoomMeetingId(zoomMeetingId)
                .ifPresent(
                        meeting -> {
                            meeting.endMeeting();
                            zoomMeetingRepository.save(meeting);
                        });
    }

    private void handleRecordingCompleted(
            final String zoomMeetingId, final Map<String, Object> object) {
        ZoomMeeting meeting =
                zoomMeetingRepository
                        .findByZoomMeetingId(zoomMeetingId)
                        .orElseThrow(() -> new RuntimeException("Reunion Zoom no encontrada"));

        Object recordingFilesObj = object.get("recording_files");
        if (!(recordingFilesObj instanceof List<?> recordingFiles)) {
            return;
        }

        for (Object fileObj : recordingFiles) {
            if (!(fileObj instanceof Map<?, ?> raw)) {
                continue;
            }
            Map<String, Object> file = convertMap(raw);

            String recordingId = String.valueOf(file.get("id"));
            String fileType = String.valueOf(file.getOrDefault("file_type", "UNKNOWN"));
            String playUrl = (String) file.get("play_url");
            String downloadUrl = (String) file.get("download_url");
            LocalDateTime recordingStart = parseZoomDate((String) file.get("recording_start"));
            LocalDateTime recordingEnd = parseZoomDate((String) file.get("recording_end"));

            ZoomRecording rec =
                    recordingRepository
                            .findByZoomRecordingId(recordingId)
                            .orElse(
                                    ZoomRecording.builder()
                                            .zoomMeeting(meeting)
                                            .zoomRecordingId(recordingId)
                                            .build());

            rec.setZoomMeeting(meeting);
            rec.setFileType(fileType);
            rec.setPlayUrl(playUrl);
            rec.setDownloadUrl(downloadUrl);
            rec.setRecordingStart(recordingStart);
            rec.setRecordingEnd(recordingEnd);
            rec.setStatus("AVAILABLE");

            ZoomRecording saved = recordingRepository.save(rec);
            storeRecordingInApp(saved);
        }
    }

    private void storeRecordingInApp(final ZoomRecording recording) {
        if (recording.getDownloadUrl() == null || recording.getDownloadUrl().isBlank()) {
            return;
        }

        try {
            String token = getZoomAccessToken();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);

            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<byte[]> response =
                    restTemplate.exchange(
                            recording.getDownloadUrl(), HttpMethod.GET, request, byte[].class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("No se pudo descargar la grabacion desde Zoom");
            }

            byte[] content = response.getBody();
            String extension =
                    recording.getFileType() != null ? recording.getFileType().toLowerCase() : "mp4";
            String safeFileName =
                    "zoom-"
                            + recording.getZoomMeeting().getComunidad().getId()
                            + "-"
                            + recording.getZoomMeeting().getZoomMeetingId()
                            + "-"
                            + recording.getZoomRecordingId()
                            + "."
                            + extension;

            Path baseDir = Paths.get(zoomRecordingsStoragePath);
            Files.createDirectories(baseDir);

            Path filePath = baseDir.resolve(safeFileName);
            Files.write(filePath, content);

            recording.setStoredInApp(true);
            recording.setLocalFilePath(filePath.toString());
            recording.setFileSizeBytes((long) content.length);
            recordingRepository.save(recording);
        } catch (HttpClientErrorException.Forbidden e) {
            recording.setStoredInApp(false);
            recording.setStatus("DOWNLOAD_FORBIDDEN");
            recordingRepository.save(recording);
        } catch (Exception e) {
            recording.setStoredInApp(false);
            recording.setStatus("DOWNLOAD_FAILED");
            recordingRepository.save(recording);
        }
    }

    private LocalDateTime parseZoomDate(final String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        return OffsetDateTime.parse(date).atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    private void assertMember(final Long comunidadId, final Long userId) {
        if (!authorizationService.isMemberOf(userId, comunidadId)) {
            throw new RuntimeException("No perteneces a esta comunidad");
        }
    }

    private Map<String, Object> createZoomMeeting(
            final String topic, final Integer durationMinutes) {
        String token = getZoomAccessToken();

        String password = RandomStringUtils.secureStrong().nextAlphanumeric(10);

        Map<String, Object> body = new HashMap<>();
        body.put("topic", topic);
        body.put("type", 2);
        body.put("duration", durationMinutes);
        body.put("password", password);

        Map<String, Object> settings = new HashMap<>();
        settings.put("join_before_host", false);
        settings.put("waiting_room", true);
        settings.put("auto_recording", "cloud");
        settings.put("mute_upon_entry", true);
        body.put("settings", settings);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response =
                restTemplate.postForEntity(
                        zoomApiBaseUrl + "/users/me/meetings", request, Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("No se pudo crear la reunion Zoom");
        }

        return response.getBody();
    }

    private String getZoomAccessToken() {
        if (isBlank(zoomClientId) || isBlank(zoomClientSecret) || isBlank(zoomAccountId)) {
            throw new RuntimeException(
                    "Faltan credenciales Zoom. Revisa zoom.client-id, zoom.client-secret y"
                            + " zoom.account-id");
        }

        String basicAuth =
                Base64.getEncoder()
                        .encodeToString(
                                (zoomClientId + ":" + zoomClientSecret)
                                        .getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + basicAuth);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String tokenUrl =
                "https://zoom.us/oauth/token?grant_type=account_credentials&account_id="
                        + zoomAccountId;

        ResponseEntity<Map> response =
                restTemplate.exchange(tokenUrl, HttpMethod.POST, entity, Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("No se pudo obtener token OAuth de Zoom");
        }

        Object token = response.getBody().get("access_token");
        if (token == null || String.valueOf(token).isBlank()) {
            throw new RuntimeException("Zoom devolvio un token invalido");
        }

        return String.valueOf(token);
    }

    private void validateWebhookAuthorization(final String authorizationHeader) {
        if (isBlank(zoomWebhookSecretToken)) {
            return;
        }
        if (authorizationHeader == null
                || !MessageDigest.isEqual(
                        authorizationHeader.getBytes(StandardCharsets.UTF_8),
                        zoomWebhookSecretToken.getBytes(StandardCharsets.UTF_8))) {
            throw new RuntimeException("Webhook no autorizado");
        }
    }

    private Map<String, Object> buildValidationResponse(final Map<String, Object> payload) {
        Map<String, Object> eventPayload = getMap(payload, "payload");
        String plainToken = String.valueOf(eventPayload.get("plainToken"));

        String encryptedToken = hmacSha256Hex(plainToken, zoomWebhookSecretToken);
        return Map.of("plainToken", plainToken, "encryptedToken", encryptedToken);
    }

    private String hmacSha256Hex(final String data, final String secret) {
        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey =
                    new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKey);
            byte[] hash = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("No se pudo validar el webhook de Zoom", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(final Map<String, Object> source, final String key) {
        Object value = source.get(key);
        if (value instanceof Map<?, ?> map) {
            return convertMap(map);
        }
        return new HashMap<>();
    }

    private Map<String, Object> convertMap(final Map<?, ?> raw) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            result.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return result;
    }

    private boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }
}
