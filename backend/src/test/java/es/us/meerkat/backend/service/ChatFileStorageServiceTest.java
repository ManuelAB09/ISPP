package es.us.meerkat.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class ChatFileStorageServiceTest {

    private final ChatFileStorageService service = new ChatFileStorageService();

    @Mock private MultipartFile file;

    @Test
    void validateAndExtractShouldReturnValidatedFileForJpeg() throws Exception {
        byte[] content = new byte[] {1, 2, 3};
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getOriginalFilename()).thenReturn("photo.jpg");
        when(file.getBytes()).thenReturn(content);

        ChatFileStorageService.ValidatedChatFile result = service.validateAndExtract(file);

        assertThat(result.content()).isEqualTo(content);
        assertThat(result.originalName()).isEqualTo("photo.jpg");
        assertThat(result.mimeType()).isEqualTo("image/jpeg");
        assertThat(result.sizeBytes()).isEqualTo(1024L);
    }

    @Test
    void validateAndExtractShouldAcceptPng() throws Exception {
        byte[] content = new byte[] {1};
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(100L);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getOriginalFilename()).thenReturn("img.png");
        when(file.getBytes()).thenReturn(content);

        ChatFileStorageService.ValidatedChatFile result = service.validateAndExtract(file);

        assertThat(result.mimeType()).isEqualTo("image/png");
    }

    @Test
    void validateAndExtractShouldAcceptPdf() throws Exception {
        byte[] content = new byte[] {1};
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(100L);
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getOriginalFilename()).thenReturn("doc.pdf");
        when(file.getBytes()).thenReturn(content);

        ChatFileStorageService.ValidatedChatFile result = service.validateAndExtract(file);

        assertThat(result.mimeType()).isEqualTo("application/pdf");
    }

    @Test
    void validateAndExtractShouldThrowWhenFileIsNull() {
        assertThatThrownBy(() -> service.validateAndExtract(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Archivo requerido");
    }

    @Test
    void validateAndExtractShouldThrowWhenFileIsEmpty() {
        when(file.isEmpty()).thenReturn(true);

        assertThatThrownBy(() -> service.validateAndExtract(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Archivo requerido");
    }

    @Test
    void validateAndExtractShouldThrowWhenFileTooLarge() {
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(6L * 1024L * 1024L);

        assertThatThrownBy(() -> service.validateAndExtract(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El archivo supera el límite de 5MB");
    }

    @Test
    void validateAndExtractShouldThrowWhenMimeTypeNotAllowed() {
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(100L);
        when(file.getContentType()).thenReturn("text/html");

        assertThatThrownBy(() -> service.validateAndExtract(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Formato no permitido. Solo JPG, PNG, WEBP o PDF");
    }

    @Test
    void validateAndExtractShouldThrowWhenMimeTypeIsNull() {
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(100L);
        when(file.getContentType()).thenReturn(null);

        assertThatThrownBy(() -> service.validateAndExtract(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Formato no permitido. Solo JPG, PNG, WEBP o PDF");
    }

    @Test
    void validateAndExtractShouldSanitizeFilenameWithBackslash() throws Exception {
        byte[] content = new byte[] {1};
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(100L);
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getOriginalFilename()).thenReturn("path\\file.jpg");
        when(file.getBytes()).thenReturn(content);

        ChatFileStorageService.ValidatedChatFile result = service.validateAndExtract(file);

        assertThat(result.originalName()).isEqualTo("path_file.jpg");
    }

    @Test
    void validateAndExtractShouldSanitizeFilenameWithSlash() throws Exception {
        byte[] content = new byte[] {1};
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(100L);
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getOriginalFilename()).thenReturn("path/file.jpg");
        when(file.getBytes()).thenReturn(content);

        ChatFileStorageService.ValidatedChatFile result = service.validateAndExtract(file);

        assertThat(result.originalName()).isEqualTo("path_file.jpg");
    }

    @Test
    void validateAndExtractShouldDefaultToArchivoWhenFilenameIsBlank() throws Exception {
        byte[] content = new byte[] {1};
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(100L);
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getOriginalFilename()).thenReturn("   ");
        when(file.getBytes()).thenReturn(content);

        ChatFileStorageService.ValidatedChatFile result = service.validateAndExtract(file);

        assertThat(result.originalName()).isEqualTo("archivo");
    }
}
