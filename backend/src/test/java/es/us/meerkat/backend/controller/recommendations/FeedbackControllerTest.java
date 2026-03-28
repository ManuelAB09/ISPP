package es.us.meerkat.backend.controller.recommendations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import es.us.meerkat.backend.controller.recommendations.FeedbackController;
import es.us.meerkat.backend.service.recommendations.FeedbackService;

@ExtendWith(MockitoExtension.class)
class FeedbackControllerTest {

    @Mock private FeedbackService feedbackService;

    @InjectMocks private FeedbackController controller;

    @Test
    void createFeedbackShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> response = controller.createFeedback(1L, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void listFeedbacksShouldReturnOk() {
        when(feedbackService.listFeedbacksByCommunity(1L, PageRequest.of(0, 20)))
                .thenReturn(Page.empty());

        ResponseEntity<?> response = controller.listFeedbacks(1L, 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
