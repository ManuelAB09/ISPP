package es.us.meerkat.backend.service.notifications;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import es.us.meerkat.backend.exception.ValidationException;
import es.us.meerkat.backend.service.chats.LinkPreviewService;

class LinkPreviewServiceTest {

    private final LinkPreviewService service = new LinkPreviewService();

    @Test
    void getPreviewShouldThrowWhenUrlIsNull() {
        assertThatThrownBy(() -> service.getPreview(null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("La URL es obligatoria");
    }

    @Test
    void getPreviewShouldThrowWhenUrlIsBlank() {
        assertThatThrownBy(() -> service.getPreview("   "))
                .isInstanceOf(ValidationException.class)
                .hasMessage("La URL es obligatoria");
    }

    @Test
    void getPreviewShouldThrowWhenUrlHasInvalidScheme() {
        assertThatThrownBy(() -> service.getPreview("ftp://example.com"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Solo se permiten URLs http/https");
    }

    @Test
    void getPreviewShouldThrowWhenUrlIsLocalhost() {
        assertThatThrownBy(() -> service.getPreview("http://localhost/test"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("No se permiten hosts locales");
    }

    @Test
    void getPreviewShouldThrowWhenUrlHasLocalDomain() {
        assertThatThrownBy(() -> service.getPreview("http://myhost.local/test"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("No se permiten hosts locales");
    }

    @Test
    void getPreviewShouldThrowWhenUrlHasInvalidFormat() {
        assertThatThrownBy(() -> service.getPreview("ht tp://bad url"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("La URL no tiene un formato válido");
    }

    @Test
    void getPreviewShouldThrowWhenHostIsPrivateIp() {
        assertThatThrownBy(() -> service.getPreview("http://192.168.1.1/test"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("No se permiten hosts privados o internos");
    }

    @Test
    void getPreviewShouldThrowWhenHostIsLoopback() {
        assertThatThrownBy(() -> service.getPreview("http://127.0.0.1/test"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("No se permiten hosts privados o internos");
    }
}
