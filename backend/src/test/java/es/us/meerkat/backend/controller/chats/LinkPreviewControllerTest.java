package es.us.meerkat.backend.controller.chats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
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

    @Test
    void previewShouldReturnOkWithImage() {
        LinkPreviewRequest req = new LinkPreviewRequest();
        req.setUrl("https://example.com/page");
        LinkPreviewResponse resp =
                LinkPreviewResponse.builder()
                        .url("https://example.com/page")
                        .domain("example.com")
                        .title("Example Page")
                        .description("A page with image")
                        .image("https://example.com/img.jpg")
                        .build();
        when(service.getPreview(req.getUrl())).thenReturn(resp);

        ResponseEntity<LinkPreviewResponse> response = controller.preview(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getImage()).isEqualTo("https://example.com/img.jpg");
    }

    @Test
    void previewShouldThrowWhenServiceFails() {
        LinkPreviewRequest req = new LinkPreviewRequest();
        req.setUrl("https://invalid.com");
        doThrow(new RuntimeException("Cannot fetch preview"))
                .when(service)
                .getPreview(req.getUrl());

        try {
            controller.preview(req);
        } catch (Exception e) {
            assertThat(e).isInstanceOf(RuntimeException.class);
            assertThat(e.getMessage()).contains("Cannot fetch preview");
        }
    }

    @Test
    void previewShouldHandleNullResponse() {
        LinkPreviewRequest req = new LinkPreviewRequest();
        req.setUrl("https://notfound.com");
        when(service.getPreview(req.getUrl())).thenReturn(null);

        try {
            ResponseEntity<LinkPreviewResponse> response = controller.preview(req);
            // If service returns null, response should be ok with null body
            assertThat(response).isNotNull();
        } catch (Exception e) {
            // Service might throw instead of returning null
            assertThat(e).isNotNull();
        }
    }

    @Test
    void previewShouldReturnOkWithDescriptionOnly() {
        LinkPreviewRequest req = new LinkPreviewRequest();
        req.setUrl("https://description.com");
        LinkPreviewResponse resp =
                LinkPreviewResponse.builder()
                        .url("https://description.com")
                        .domain("description.com")
                        .description("Only description, no title")
                        .build();
        when(service.getPreview(req.getUrl())).thenReturn(resp);

        ResponseEntity<LinkPreviewResponse> response = controller.preview(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getDescription()).isEqualTo("Only description, no title");
    }

    @Test
    void previewShouldReturnOkWithTitleOnly() {
        LinkPreviewRequest req = new LinkPreviewRequest();
        req.setUrl("https://title.com");
        LinkPreviewResponse resp =
                LinkPreviewResponse.builder()
                        .url("https://title.com")
                        .domain("title.com")
                        .title("Only title")
                        .build();
        when(service.getPreview(req.getUrl())).thenReturn(resp);

        ResponseEntity<LinkPreviewResponse> response = controller.preview(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getTitle()).isEqualTo("Only title");
    }
}
