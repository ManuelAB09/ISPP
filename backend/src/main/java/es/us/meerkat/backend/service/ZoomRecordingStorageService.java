package es.us.meerkat.backend.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import es.us.meerkat.backend.entity.ZoomRecording;

/** Gestiona el almacenamiento persistente de grabaciones de Zoom. */
@Service
public class ZoomRecordingStorageService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${zoom.recordings.storage-mode:local}")
    private String storageMode;

    @Value("${zoom.recordings.storage-path:storage/zoom-recordings}")
    private String localStoragePath;

    @Value("${zoom.recordings.supabase.url:}")
    private String supabaseUrl;

    @Value("${zoom.recordings.supabase.service-role-key:}")
    private String supabaseServiceRoleKey;

    @Value("${zoom.recordings.supabase.bucket:zoom-recordings}")
    private String supabaseBucket;

    /** Guarda una grabacion ya descargada desde Zoom en el almacenamiento configurado. */
    public StoredRecording store(final ZoomRecording recording, final byte[] content) {
        return usesSupabase()
                ? storeInSupabase(recording, content)
                : storeInLocalFilesystem(recording, content);
    }

    /** Recupera una grabacion previamente almacenada por la app. */
    public byte[] download(final ZoomRecording recording) {
        return usesSupabase()
                ? downloadFromSupabase(recording)
                : downloadFromLocalFilesystem(recording);
    }

    /** Elimina una grabacion almacenada por la app. */
    public void delete(final ZoomRecording recording) {
        if (usesSupabase()) {
            deleteFromSupabase(recording);
            return;
        }
        deleteFromLocalFilesystem(recording);
    }

    private StoredRecording storeInLocalFilesystem(
            final ZoomRecording recording, final byte[] content) {
        try {
            Path baseDir = Paths.get(localStoragePath);
            Files.createDirectories(baseDir);

            Path filePath = baseDir.resolve(buildStoredFileName(recording));
            Files.write(filePath, content);

            return new StoredRecording("LOCAL", filePath.toString(), null, (long) content.length);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo guardar la grabacion en disco local", e);
        }
    }

    private byte[] downloadFromLocalFilesystem(final ZoomRecording recording) {
        if (recording.getLocalFilePath() == null || recording.getLocalFilePath().isBlank()) {
            throw new RuntimeException("La grabacion no tiene fichero local asociado");
        }
        try {
            return Files.readAllBytes(Paths.get(recording.getLocalFilePath()));
        } catch (Exception e) {
            throw new RuntimeException("No se pudo leer la grabacion almacenada", e);
        }
    }

    private void deleteFromLocalFilesystem(final ZoomRecording recording) {
        if (recording.getLocalFilePath() == null || recording.getLocalFilePath().isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(Paths.get(recording.getLocalFilePath()));
        } catch (Exception e) {
            throw new RuntimeException("No se pudo eliminar la grabacion local", e);
        }
    }

    private StoredRecording storeInSupabase(final ZoomRecording recording, final byte[] content) {
        validateSupabaseConfiguration();

        String objectKey = buildObjectKey(recording);
        HttpHeaders headers = buildSupabaseHeaders(resolveMediaType(recording));
        headers.set("x-upsert", "true");

        HttpEntity<byte[]> request = new HttpEntity<>(content, headers);
        ResponseEntity<String> response =
                restTemplate.exchange(
                        buildSupabaseObjectUrl(
                                "/storage/v1/object/" + supabaseBucket + "/" + objectKey),
                        HttpMethod.POST,
                        request,
                        String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Supabase no acepto la grabacion");
        }

        return new StoredRecording("SUPABASE", null, objectKey, (long) content.length);
    }

    private byte[] downloadFromSupabase(final ZoomRecording recording) {
        validateSupabaseConfiguration();
        if (recording.getStorageObjectKey() == null || recording.getStorageObjectKey().isBlank()) {
            throw new RuntimeException("La grabacion no tiene objeto almacenado en Supabase");
        }

        HttpEntity<Void> request =
                new HttpEntity<>(buildSupabaseHeaders(resolveMediaType(recording)));
        ResponseEntity<byte[]> response =
                restTemplate.exchange(
                        buildSupabaseObjectUrl(
                                "/storage/v1/object/authenticated/"
                                        + supabaseBucket
                                        + "/"
                                        + recording.getStorageObjectKey()),
                        HttpMethod.GET,
                        request,
                        byte[].class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("No se pudo descargar la grabacion almacenada en Supabase");
        }

        return response.getBody();
    }

    private void deleteFromSupabase(final ZoomRecording recording) {
        validateSupabaseConfiguration();
        if (recording.getStorageObjectKey() == null || recording.getStorageObjectKey().isBlank()) {
            return;
        }

        HttpEntity<Void> request =
                new HttpEntity<>(buildSupabaseHeaders(MediaType.APPLICATION_JSON_VALUE));
        ResponseEntity<String> response =
                restTemplate.exchange(
                        buildSupabaseObjectUrl(
                                "/storage/v1/object/"
                                        + supabaseBucket
                                        + "/"
                                        + recording.getStorageObjectKey()),
                        HttpMethod.DELETE,
                        request,
                        String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("No se pudo eliminar la grabacion almacenada en Supabase");
        }
    }

    private boolean usesSupabase() {
        return "supabase".equalsIgnoreCase(storageMode);
    }

    private void validateSupabaseConfiguration() {
        if (isBlank(supabaseUrl) || isBlank(supabaseServiceRoleKey) || isBlank(supabaseBucket)) {
            throw new RuntimeException(
                    "Falta configurar Supabase para grabaciones. Define"
                            + " ZOOM_RECORDINGS_SUPABASE_URL,"
                            + " ZOOM_RECORDINGS_SUPABASE_SERVICE_ROLE_KEY y"
                            + " ZOOM_RECORDINGS_SUPABASE_BUCKET");
        }
    }

    private HttpHeaders buildSupabaseHeaders(final String contentType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(supabaseServiceRoleKey);
        headers.set("apikey", supabaseServiceRoleKey);
        headers.setContentType(MediaType.parseMediaType(contentType));
        return headers;
    }

    private String buildSupabaseObjectUrl(final String path) {
        return trimTrailingSlash(supabaseUrl) + path;
    }

    private String buildObjectKey(final ZoomRecording recording) {
        return "communities/"
                + recording.getZoomMeeting().getComunidad().getId()
                + "/"
                + recording.getZoomMeeting().getZoomMeetingId()
                + "/"
                + buildStoredFileName(recording);
    }

    private String buildStoredFileName(final ZoomRecording recording) {
        return "zoom-"
                + recording.getZoomMeeting().getComunidad().getId()
                + "-"
                + recording.getZoomMeeting().getZoomMeetingId()
                + "-"
                + recording.getZoomRecordingId()
                + "."
                + resolveExtension(recording);
    }

    private String resolveExtension(final ZoomRecording recording) {
        String fileType = recording.getFileType();
        if (fileType == null || fileType.isBlank()) {
            return "bin";
        }
        return fileType.toLowerCase().replaceAll("[^a-z0-9]+", "");
    }

    private String resolveMediaType(final ZoomRecording recording) {
        return switch (resolveExtension(recording)) {
            case "mp4" -> MediaType.valueOf("video/mp4").toString();
            case "m4a" -> MediaType.valueOf("audio/mp4").toString();
            case "txt" -> MediaType.TEXT_PLAIN_VALUE;
            case "json" -> MediaType.APPLICATION_JSON_VALUE;
            default -> MediaType.APPLICATION_OCTET_STREAM_VALUE;
        };
    }

    private String trimTrailingSlash(final String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }

    /** Resultado de persistir una grabacion en almacenamiento externo o local. */
    public record StoredRecording(
            String provider, String localFilePath, String storageObjectKey, Long fileSizeBytes) {}
}
