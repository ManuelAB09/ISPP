package es.us.meerkat.backend.service.chats;

import java.io.IOException;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Servicio para validar y extraer archivos adjuntos del chat en memoria. */
@Service
public class ChatFileStorageService {

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024L * 1024L;

    private static final Set<String> ALLOWED_MIME_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "application/pdf");

    /**
     * Valida y extrae un archivo adjunto de chat.
     *
     * @param file archivo recibido en multipart/form-data.
     * @return metadatos del archivo validados junto al contenido binario.
     */
    public ValidatedChatFile validateAndExtract(final MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Archivo requerido");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("El archivo supera el límite de 5MB");
        }

        final String mimeType = file.getContentType();
        if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new IllegalArgumentException("Formato no permitido. Solo JPG, PNG, WEBP o PDF");
        }

        final String originalName = sanitizeOriginalName(file.getOriginalFilename());

        try {
            return new ValidatedChatFile(file.getBytes(), originalName, mimeType, file.getSize());
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo procesar el archivo", e);
        }
    }

    private String sanitizeOriginalName(final String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "archivo";
        }
        return originalName.replace("\\", "_").replace("/", "_");
    }

    /** Archivo validado con metadatos listos para persistencia. */
    public record ValidatedChatFile(
            byte[] content, String originalName, String mimeType, long sizeBytes) {}
}
