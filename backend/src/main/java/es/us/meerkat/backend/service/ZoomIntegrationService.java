package es.us.meerkat.backend.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

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
import lombok.extern.slf4j.Slf4j;

/** Servicio principal para gestionar reuniones Zoom vinculadas a comunidades. */
@Service
@RequiredArgsConstructor
@Slf4j
public class ZoomIntegrationService {

    private static final long MAX_MANUAL_RECORDING_SIZE_BYTES = 50L * 1024L * 1024L;

    private final ZoomMeetingRepository zoomMeetingRepository;
    private final ZoomMeetingParticipantRepository participantRepository;
    private final ZoomRecordingRepository recordingRepository;
    private final ComunidadRepository comunidadRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuthorizationService authorizationService;
    private final ZoomRecordingStorageService zoomRecordingStorageService;

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

    @Value("${zoom.recordings.retention-days:30}")
    private long zoomRecordingsRetentionDays;

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
            ZoomMeeting activeMeeting = existing.get();
            if (activeMeeting.getStartedAt() != null
                    && participantRepository
                            .findByZoomMeetingIdAndInCallTrueOrderByJoinedAtAsc(
                                    activeMeeting.getId())
                            .isEmpty()) {
                activeMeeting.endMeeting();
                zoomMeetingRepository.save(activeMeeting);
            } else {
                return activeMeeting;
            }
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
        if (durationMinutesParam != null && durationMinutesParam < 0) {
            throw new IllegalArgumentException("durationMinutes no puede ser menor que cero");
        }

        Integer durationMinutes =
                durationMinutesParam != null && durationMinutesParam > 0
                        ? durationMinutesParam
                        : null;

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
                        .durationMinutes(durationMinutes)
                        .build();

        return zoomMeetingRepository.save(meeting);
    }

    /** Finaliza manualmente la llamada activa de una comunidad. */
    @Transactional
    public void endActiveMeeting(final Long comunidadId, final Long userId) {
        ZoomMeeting meeting = getActiveMeeting(comunidadId, userId);

        if (!meeting.getCreador().getId().equals(userId)) {
            throw new RuntimeException("Solo el creador puede finalizar la llamada");
        }

        closeAllActiveParticipants(meeting);
        meeting.endMeeting();
        zoomMeetingRepository.save(meeting);
    }

    /** Obtiene la reunion activa de una comunidad si el usuario pertenece a ella. */
    public ZoomMeeting getActiveMeeting(final Long comunidadId, final Long userId) {
        assertMember(comunidadId, userId);

        return zoomMeetingRepository
                .findFirstByComunidadIdAndStatusOrderByCreatedAtDesc(
                        comunidadId, ZoomMeetingStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("No hay llamada activa en esta comunidad"));
    }

    /** Lista el historico de reuniones de una comunidad. */
    public List<ZoomMeeting> getMeetingsForCommunity(final Long comunidadId, final Long userId) {
        assertMember(comunidadId, userId);
        return zoomMeetingRepository.findByComunidadIdOrderByCreatedAtDesc(comunidadId);
    }

    /** Devuelve el acceso a la reunion comun y registra presencia pendiente en la app. */
    @Transactional
    public ZoomJoinResponse joinActiveMeeting(final Long comunidadId, final Long userId) {
        ZoomMeeting meeting = getActiveMeeting(comunidadId, userId);
        Usuario user =
                usuarioRepository
                        .findById(userId)
                        .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        boolean alreadyTracked =
                participantRepository
                        .findFirstByZoomMeetingIdAndUsuarioIdOrderByJoinedAtDesc(
                                meeting.getId(), userId)
                        .isPresent();

        if (!alreadyTracked) {
            participantRepository.save(
                    ZoomMeetingParticipant.builder()
                            .zoomMeeting(meeting)
                            .usuario(user)
                            .zoomParticipantId(
                                    "app-" + user.getId() + "-" + System.currentTimeMillis())
                            .displayName(user.getNombre())
                            .email(user.getEmail())
                            .joinedAt(LocalDateTime.now())
                            .inCall(false)
                            .build());
        }

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
                        participant ->
                                new ZoomParticipantResponse(
                                        participant.getUsuario() != null
                                                ? participant.getUsuario().getId()
                                                : null,
                                        participant.getDisplayName(),
                                        participant.getEmail(),
                                        participant.getInCall(),
                                        participant.getJoinedAt(),
                                        participant.getLeftAt()))
                .toList();
    }

    /** Devuelve en que llamadas esta actualmente el usuario autenticado. */
    public List<ZoomUserCallResponse> getActiveCallsForUser(final Long userId) {
        return participantRepository
                .findByUsuarioIdAndInCallTrueOrderByJoinedAtDesc(userId)
                .stream()
                .map(
                        participant ->
                                new ZoomUserCallResponse(
                                        participant.getZoomMeeting().getComunidad().getId(),
                                        participant.getZoomMeeting().getComunidad().getNombre(),
                                        participant.getZoomMeeting().getZoomMeetingId(),
                                        participant.getZoomMeeting().getTopic(),
                                        participant.getJoinedAt()))
                .toList();
    }

    /** Lista las grabaciones registradas para una comunidad. */
    public List<ZoomRecordingResponse> getRecordingsForCommunity(
            final Long comunidadId, final Long userId) {
        assertMember(comunidadId, userId);

        return recordingRepository
                .findByZoomMeetingComunidadIdOrderByCreatedAtDesc(comunidadId)
                .stream()
                .map(this::toRecordingResponse)
                .toList();
    }

    /** Devuelve el detalle de una grabacion de comunidad. */
    public ZoomRecordingResponse getRecordingForCommunity(
            final Long comunidadId, final String recordingId, final Long userId) {
        return toRecordingResponse(findRecordingForCommunity(comunidadId, recordingId, userId));
    }

    /** Sube manualmente una grabacion y la vincula a una reunion de la comunidad. */
    @Transactional
    public ZoomRecordingResponse uploadRecordingForMeeting(
            final Long comunidadId,
            final Long meetingId,
            final Long userId,
            final MultipartFile file) {

        ZoomMeeting meeting = findMeetingForCommunity(comunidadId, meetingId, userId);
        ValidatedManualRecording validatedRecording = validateManualRecording(file);

        ZoomRecording recording =
                ZoomRecording.builder()
                        .zoomMeeting(meeting)
                        .zoomRecordingId(UUID.randomUUID().toString())
                        .fileType(validatedRecording.fileType())
                        .storedInApp(false)
                        .recordingStart(
                                meeting.getStartedAt() != null
                                        ? meeting.getStartedAt()
                                        : meeting.getCreatedAt())
                        .recordingEnd(meeting.getEndedAt())
                        .expiresAt(LocalDateTime.now().plusDays(zoomRecordingsRetentionDays))
                        .status("UPLOADED")
                        .build();

        ZoomRecording savedRecording = recordingRepository.save(recording);
        ZoomRecordingStorageService.StoredRecording storedRecording =
                zoomRecordingStorageService.store(savedRecording, validatedRecording.content());

        savedRecording.setStoredInApp(true);
        savedRecording.setStorageProvider(storedRecording.provider());
        savedRecording.setLocalFilePath(storedRecording.localFilePath());
        savedRecording.setStorageObjectKey(storedRecording.storageObjectKey());
        savedRecording.setFileSizeBytes(storedRecording.fileSizeBytes());
        savedRecording.setStatus("STORED");

        return toRecordingResponse(recordingRepository.save(savedRecording));
    }

    /** Descarga una grabacion almacenada por la aplicacion. */
    public RecordingDownload downloadRecordingForCommunity(
            final Long comunidadId, final String recordingId, final Long userId) {
        ZoomRecording recording = findRecordingForCommunity(comunidadId, recordingId, userId);

        if (!Boolean.TRUE.equals(recording.getStoredInApp())) {
            throw new RuntimeException("La grabacion aun no esta disponible en la app");
        }
        if (recording.getExpiresAt() != null
                && recording.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("La grabacion ha expirado");
        }

        byte[] content = zoomRecordingStorageService.download(recording);
        return new RecordingDownload(
                content,
                buildStoredFileName(recording),
                resolveRecordingMimeType(recording.getFileType()));
    }

    /** Elimina diariamente las grabaciones expiradas del almacenamiento y de la base de datos. */
    @Scheduled(cron = "${zoom.recordings.cleanup-cron:0 0 3 * * *}")
    @Transactional
    public void cleanupExpiredRecordings() {
        LocalDateTime now = LocalDateTime.now();
        for (ZoomRecording recording : recordingRepository.findByExpiresAtBefore(now)) {
            try {
                if (Boolean.TRUE.equals(recording.getStoredInApp())) {
                    zoomRecordingStorageService.delete(recording);
                }
                recordingRepository.delete(recording);
            } catch (RuntimeException e) {
                recording.setStatus("PURGE_FAILED");
                recordingRepository.save(recording);
            }
        }
    }

    /** Procesa webhooks de Zoom para participantes y fin de llamada. */
    @Transactional
    public Map<String, Object> processWebhook(
            final Map<String, Object> payload, final String zmSignature, final String zmTimestamp) {

        String event = (String) payload.get("event");
        if ("endpoint.url_validation".equals(event)) {
            return buildValidationResponse(payload);
        }

        validateWebhookSignature(zmSignature, zmTimestamp, payload);

        Map<String, Object> eventPayload = getMap(payload, "payload");
        Map<String, Object> object = getMap(eventPayload, "object");
        String zoomMeetingId = String.valueOf(object.get("id"));

        switch (event) {
            case "meeting.participant_joined" -> handleParticipantJoined(zoomMeetingId, object);
            case "meeting.participant_left" -> handleParticipantLeft(zoomMeetingId, object);
            case "meeting.ended" -> handleMeetingEnded(zoomMeetingId);
            default ->
                    log.debug("Evento Zoom no gestionado: {} (meetingId={})", event, zoomMeetingId);
        }

        return Map.of("status", "ok");
    }

    private void handleParticipantJoined(
            final String zoomMeetingId, final Map<String, Object> object) {
        Optional<ZoomMeeting> meetingOptional =
                zoomMeetingRepository.findByZoomMeetingId(zoomMeetingId);
        if (meetingOptional.isEmpty()) {
            log.warn("Webhook join ignorado: no existe reunion para meetingId={}", zoomMeetingId);
            return;
        }

        ZoomMeeting meeting = meetingOptional.get();
        Map<String, Object> participant = getMap(object, "participant");
        String participantId =
                String.valueOf(
                        participant.getOrDefault("id", "zoom-" + System.currentTimeMillis()));
        String displayName = String.valueOf(participant.getOrDefault("user_name", "Usuario Zoom"));
        String email =
                participant.get("email") != null ? String.valueOf(participant.get("email")) : null;
        Usuario user = email != null ? usuarioRepository.findByEmail(email).orElse(null) : null;

        Optional<ZoomMeetingParticipant> activeParticipant =
                participantRepository
                        .findFirstByZoomMeetingZoomMeetingIdAndZoomParticipantIdAndInCallTrueOrderByJoinedAtDesc(
                                zoomMeetingId, participantId);
        if (activeParticipant.isPresent()) {
            return;
        }

        Optional<ZoomMeetingParticipant> pendingParticipant =
                email != null
                        ? participantRepository
                                .findFirstByZoomMeetingIdAndEmailAndInCallFalseOrderByJoinedAtDesc(
                                        meeting.getId(), email)
                        : Optional.empty();

        ZoomMeetingParticipant participantEntity =
                pendingParticipant.orElseGet(
                        () ->
                                ZoomMeetingParticipant.builder()
                                        .zoomMeeting(meeting)
                                        .usuario(user)
                                        .zoomParticipantId(participantId)
                                        .displayName(displayName)
                                        .email(email)
                                        .build());

        participantEntity.setZoomParticipantId(participantId);
        participantEntity.setDisplayName(displayName);
        participantEntity.setEmail(email);
        participantEntity.setUsuario(user);
        participantEntity.setJoinedAt(LocalDateTime.now());
        participantEntity.setLeftAt(null);
        participantEntity.setInCall(true);
        participantRepository.save(participantEntity);

        if (meeting.getStartedAt() == null) {
            meeting.markStarted();
            zoomMeetingRepository.save(meeting);
        }
    }

    private void handleParticipantLeft(
            final String zoomMeetingId, final Map<String, Object> object) {
        Map<String, Object> participant = getMap(object, "participant");
        String participantId = String.valueOf(participant.getOrDefault("id", ""));
        if (participantId.isBlank()) {
            return;
        }

        participantRepository
                .findFirstByZoomMeetingZoomMeetingIdAndZoomParticipantIdAndInCallTrueOrderByJoinedAtDesc(
                        zoomMeetingId, participantId)
                .ifPresent(
                        participantEntity -> {
                            participantEntity.markLeft();
                            participantRepository.save(participantEntity);
                        });

        zoomMeetingRepository
                .findByZoomMeetingId(zoomMeetingId)
                .ifPresent(
                        meeting -> {
                            if (participantRepository
                                    .findByZoomMeetingIdAndInCallTrueOrderByJoinedAtAsc(
                                            meeting.getId())
                                    .isEmpty()) {
                                meeting.endMeeting();
                                zoomMeetingRepository.save(meeting);
                            }
                        });
    }

    private void handleMeetingEnded(final String zoomMeetingId) {
        zoomMeetingRepository
                .findByZoomMeetingId(zoomMeetingId)
                .ifPresent(
                        meeting -> {
                            closeAllActiveParticipants(meeting);
                            meeting.endMeeting();
                            zoomMeetingRepository.save(meeting);
                        });
    }

    private void closeAllActiveParticipants(final ZoomMeeting meeting) {
        participantRepository
                .findByZoomMeetingIdAndInCallTrueOrderByJoinedAtAsc(meeting.getId())
                .forEach(
                        participant -> {
                            participant.markLeft();
                            participantRepository.save(participant);
                        });
    }

    private ZoomMeeting findMeetingForCommunity(
            final Long comunidadId, final Long meetingId, final Long userId) {
        assertMember(comunidadId, userId);

        ZoomMeeting meeting =
                zoomMeetingRepository
                        .findById(meetingId)
                        .orElseThrow(() -> new RuntimeException("Reunion no encontrada"));
        if (!meeting.getComunidad().getId().equals(comunidadId)) {
            throw new RuntimeException("Reunion no encontrada");
        }
        return meeting;
    }

    private void assertMember(final Long comunidadId, final Long userId) {
        if (!authorizationService.isMemberOf(userId, comunidadId)) {
            throw new RuntimeException("No perteneces a esta comunidad");
        }
    }

    private ZoomRecording findRecordingForCommunity(
            final Long comunidadId, final String recordingId, final Long userId) {
        assertMember(comunidadId, userId);

        return recordingRepository
                .findByZoomMeetingComunidadIdAndZoomRecordingId(comunidadId, recordingId)
                .orElseThrow(() -> new RuntimeException("Grabacion no encontrada"));
    }

    private ZoomRecordingResponse toRecordingResponse(final ZoomRecording recording) {
        return new ZoomRecordingResponse(
                recording.getZoomRecordingId(),
                recording.getZoomMeeting().getZoomMeetingId(),
                recording.getZoomMeeting().getComunidad().getId(),
                recording.getZoomMeeting().getComunidad().getNombre(),
                recording.getFileType(),
                recording.getPlayUrl(),
                recording.getDownloadUrl(),
                recording.getStoredInApp(),
                buildRecordingDownloadPath(
                        recording.getZoomMeeting().getComunidad().getId(),
                        recording.getZoomRecordingId()),
                recording.getFileSizeBytes(),
                recording.getRecordingStart(),
                recording.getRecordingEnd(),
                recording.getExpiresAt(),
                recording.getStatus(),
                recording.getCreatedAt());
    }

    private String buildRecordingDownloadPath(final Long comunidadId, final String recordingId) {
        return "/api/v1/zoom/communities/"
                + comunidadId
                + "/recordings/"
                + recordingId
                + "/download";
    }

    private String buildStoredFileName(final ZoomRecording recording) {
        return "zoom-"
                + recording.getZoomMeeting().getComunidad().getId()
                + "-"
                + recording.getZoomMeeting().getZoomMeetingId()
                + "-"
                + recording.getZoomRecordingId()
                + "."
                + resolveRecordingExtension(recording.getFileType());
    }

    private String resolveRecordingExtension(final String fileType) {
        if (fileType == null || fileType.isBlank()) {
            return "bin";
        }
        return fileType.toLowerCase().replaceAll("[^a-z0-9]+", "");
    }

    private String resolveRecordingMimeType(final String fileType) {
        return switch (resolveRecordingExtension(fileType)) {
            case "mp4" -> "video/mp4";
            case "mov" -> "video/quicktime";
            case "webm" -> "video/webm";
            case "m4a" -> "audio/mp4";
            case "txt" -> MediaType.TEXT_PLAIN_VALUE;
            case "json" -> MediaType.APPLICATION_JSON_VALUE;
            default -> MediaType.APPLICATION_OCTET_STREAM_VALUE;
        };
    }

    private ValidatedManualRecording validateManualRecording(final MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Debes subir un archivo de grabacion");
        }
        if (file.getSize() > MAX_MANUAL_RECORDING_SIZE_BYTES) {
            throw new RuntimeException("La grabacion supera el limite de 50 MB");
        }

        try {
            byte[] content = file.getBytes();
            if (content.length == 0) {
                throw new RuntimeException("Debes subir un archivo de grabacion");
            }
            return new ValidatedManualRecording(content, resolveManualRecordingFileType(file));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("No se pudo leer el archivo subido", e);
        }
    }

    private String resolveManualRecordingFileType(final MultipartFile file) {
        String filename =
                file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        String contentType =
                file.getContentType() != null ? file.getContentType().toLowerCase() : "";

        if (filename.endsWith(".mp4") || "video/mp4".equals(contentType)) {
            return "MP4";
        }
        if (filename.endsWith(".mov") || "video/quicktime".equals(contentType)) {
            return "MOV";
        }
        if (filename.endsWith(".webm") || "video/webm".equals(contentType)) {
            return "WEBM";
        }
        if (filename.endsWith(".m4a") || "audio/mp4".equals(contentType)) {
            return "M4A";
        }

        throw new RuntimeException("Formato no permitido. Solo MP4, MOV, WEBM o M4A");
    }

    private Map<String, Object> createZoomMeeting(
            final String topic, final Integer durationMinutes) {
        String token = getZoomAccessToken();
        String password = RandomStringUtils.secureStrong().nextAlphanumeric(10);

        Map<String, Object> body = new HashMap<>();
        body.put("topic", topic);
        body.put("type", 2);
        if (durationMinutes != null) {
            body.put("duration", durationMinutes);
        }
        body.put("password", password);

        Map<String, Object> settings = new HashMap<>();
        settings.put("join_before_host", true);
        settings.put("waiting_room", false);
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

    private void validateWebhookSignature(
            final String zmSignature, final String zmTimestamp, final Map<String, Object> payload) {
        if (isBlank(zoomWebhookSecretToken)) {
            return;
        }
        if (zmSignature == null || zmTimestamp == null) {
            throw new RuntimeException("Webhook no autorizado");
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            String bodyJson = mapper.writeValueAsString(payload);
            String message = "v0:" + zmTimestamp + ":" + bodyJson;
            String expectedHash = "v0=" + hmacSha256Hex(message, zoomWebhookSecretToken);
            if (!MessageDigest.isEqual(
                    expectedHash.getBytes(StandardCharsets.UTF_8),
                    zmSignature.getBytes(StandardCharsets.UTF_8))) {
                throw new RuntimeException("Webhook no autorizado");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("No se pudo validar la firma del webhook de Zoom", e);
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

    private record ValidatedManualRecording(byte[] content, String fileType) {}

    public record RecordingDownload(byte[] content, String fileName, String mimeType) {}
}
