package es.us.meerkat.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import es.us.meerkat.backend.controller.recommendations.RecommendationController;
import es.us.meerkat.backend.dto.RecomendacionResponse;
import es.us.meerkat.backend.service.recommendations.RecommendationService;

@ExtendWith(MockitoExtension.class)
class RecommendationControllerTest {

    @Mock private RecommendationService service;
    @Mock private UserDetails userDetails;

    @InjectMocks private RecommendationController controller;

    @Test
    void activasShouldReturnOkAndDelegateToService() {
        when(userDetails.getUsername()).thenReturn("1");
        when(service.getRecomendacionesActivas(
                        eq(1L), eq(org.springframework.data.domain.PageRequest.of(0, 20))))
                .thenReturn(Page.empty());

        ResponseEntity<Page<RecomendacionResponse>> response =
                controller.activas(userDetails, 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(service)
                .getRecomendacionesActivas(
                        1L, org.springframework.data.domain.PageRequest.of(0, 20));
    }

    @Test
    void refreshShouldReturnAcceptedAndDelegateToService() {
        when(userDetails.getUsername()).thenReturn("2");

        ResponseEntity<Void> response = controller.refresh(userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(service).generarRecomendacionesUsuario(2L);
    }
}
