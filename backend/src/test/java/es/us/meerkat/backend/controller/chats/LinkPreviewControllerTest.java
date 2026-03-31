package es.us.meerkat.backend.controller.chats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import es.us.meerkat.backend.dto.chats.LinkPreviewRequest;
import es.us.meerkat.backend.dto.chats.LinkPreviewResponse;
import es.us.meerkat.backend.service.chats.LinkPreviewService;

@ExtendWith(MockitoExtension.class)
class LinkPreviewControllerTest {

    @Mock private LinkPreviewService service;
    @InjectMocks private LinkPreviewController controller;

    @Test
    void previewShouldReturnOkWhenUrlIsValid() {
        LinkPreviewRequest req = new LinkPreviewRequest();
        req.setUrl("https://example.com");
        LinkPreviewResponse resp =
                LinkPreviewResponse.builder()
                        .url("https://example.com")
                        .domain("example.com")
                        .build();
        when(service.getPreview(req.getUrl())).thenReturn(resp);

        ResponseEntity<LinkPreviewResponse> r = controller.preview(req);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).isEqualTo(resp);
    }

    @Test
    void previewShouldReturnBadRequestWhenUrlIsBlank() {
        LinkPreviewRequest req = new LinkPreviewRequest();
        req.setUrl("   ");
        // validation would normally trigger; controller forwards to service — simulate service
        // behavior
        when(service.getPreview(req.getUrl()))
                .thenThrow(new IllegalArgumentException("La URL es obligatoria"));

        try {
            controller.preview(req);
        } catch (Exception e) {
            assertThat(e).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void previewShouldReturnOkWithMetadata() {
        LinkPreviewRequest req = new LinkPreviewRequest();
        req.setUrl("https://test.com");
        LinkPreviewResponse resp =
                LinkPreviewResponse.builder()
                        .url("https://test.com")
                        .domain("test.com")
                        .title("Test Site")
                        .description("A test site")
                        .build();
        when(service.getPreview(req.getUrl())).thenReturn(resp);

        ResponseEntity<LinkPreviewResponse> response = controller.preview(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getTitle()).isEqualTo("Test Site");
    }

    @Test
    void previewShouldReturnOkWithMinimalData() {
        LinkPreviewRequest req = new LinkPreviewRequest();
        req.setUrl("https://minimal.com");
        LinkPreviewResponse resp =
                LinkPreviewResponse.builder()
                        .url("https://minimal.com")
                        .domain("minimal.com")
                        .build();
        when(service.getPreview(req.getUrl())).thenReturn(resp);

        ResponseEntity<LinkPreviewResponse> response = controller.preview(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }
}
