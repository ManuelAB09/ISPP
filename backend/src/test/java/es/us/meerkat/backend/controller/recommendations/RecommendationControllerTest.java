package es.us.meerkat.backend.controller.recommendations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import es.us.meerkat.backend.dto.recommendations.FeedbackRecomendacionRequest;
import es.us.meerkat.backend.dto.recommendations.RecomendacionResponse;
import es.us.meerkat.backend.dto.recommendations.RecomendacionesPageResponse;
import es.us.meerkat.backend.dto.recommendations.RegistrarActividadRequest;
import es.us.meerkat.backend.dto.recommendations.ValoracionTutorRequest;
import es.us.meerkat.backend.service.recommendations.RecommendationService;

@ExtendWith(MockitoExtension.class)
class RecommendationControllerTest {

    @Mock private RecommendationService service;

    @Mock private UserDetails userDetails;

    @InjectMocks private RecommendationController controller;

    private UserDetails buildUserDetails(String userId) {
        return User.builder()
                .username(userId)
                .password("password")
                .authorities(List.of(() -> "ROLE_USER"))
                .build();
    }

    private RecomendacionResponse buildRecomendacion(Long id) {
        RecomendacionResponse response = new RecomendacionResponse();
        return response;
    }

    @Test
    void pagedRecommendationsShouldReturnOk() {
        UserDetails user = buildUserDetails("1");
        RecomendacionesPageResponse pageResponse = new RecomendacionesPageResponse();

        when(service.getRecomendacionesPage(1L)).thenReturn(pageResponse);

        ResponseEntity<RecomendacionesPageResponse> response = controller.page(user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(pageResponse);
    }

    @Test
    void profesoresRecommendationsShouldReturnOk() {
        UserDetails user = buildUserDetails("1");
        RecomendacionResponse rec1 = buildRecomendacion(1L);
        RecomendacionResponse rec2 = buildRecomendacion(2L);
        Page<RecomendacionResponse> page =
                new PageImpl<>(List.of(rec1, rec2), PageRequest.of(0, 6), 2);

        when(service.getRecomendacionesProfesores(1L, PageRequest.of(0, 6))).thenReturn(page);

        ResponseEntity<Page<RecomendacionResponse>> response = controller.profesores(user, 0, 6);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getContent()).hasSize(2);
    }

    @Test
    void contenidoRecommendationsShouldReturnOk() {
        UserDetails user = buildUserDetails("1");
        Page<RecomendacionResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 8), 0);

        when(service.getRecomendacionesContenido(1L, PageRequest.of(0, 8))).thenReturn(page);

        ResponseEntity<Page<RecomendacionResponse>> response = controller.contenido(user, 0, 8);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getContent()).isEmpty();
    }

    @Test
    void cuestionariosRecommendationsShouldReturnOk() {
        UserDetails user = buildUserDetails("1");
        RecomendacionResponse rec = buildRecomendacion(1L);
        Page<RecomendacionResponse> page = new PageImpl<>(List.of(rec), PageRequest.of(0, 6), 1);

        when(service.getRecomendacionesCuestionarios(1L, PageRequest.of(0, 6))).thenReturn(page);

        ResponseEntity<Page<RecomendacionResponse>> response = controller.cuestionarios(user, 0, 6);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getContent()).hasSize(1);
    }

    @Test
    void comunidadesRecommendationsShouldReturnOk() {
        UserDetails user = buildUserDetails("1");
        Page<RecomendacionResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 4), 0);

        when(service.getRecomendacionesComunidades(1L, PageRequest.of(0, 4))).thenReturn(page);

        ResponseEntity<Page<RecomendacionResponse>> response = controller.comunidades(user, 0, 4);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void activasRecommendationsShouldReturnOk() {
        UserDetails user = buildUserDetails("1");
        RecomendacionResponse rec = buildRecomendacion(1L);
        Page<RecomendacionResponse> page = new PageImpl<>(List.of(rec), PageRequest.of(0, 20), 1);

        when(service.getRecomendacionesActivas(1L, PageRequest.of(0, 20))).thenReturn(page);

        ResponseEntity<Page<RecomendacionResponse>> response = controller.activas(user, 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void noVistasRecommendationsShouldReturnOk() {
        UserDetails user = buildUserDetails("1");
        RecomendacionResponse rec = buildRecomendacion(1L);
        Page<RecomendacionResponse> page = new PageImpl<>(List.of(rec), PageRequest.of(0, 10), 1);

        when(service.getRecomendacionesNoVistas(1L, PageRequest.of(0, 10))).thenReturn(page);

        ResponseEntity<Page<RecomendacionResponse>> response = controller.noVistas(user, 0, 10);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void valorarTutorShouldReturnOk() {
        UserDetails user = buildUserDetails("1");
        ValoracionTutorRequest request = new ValoracionTutorRequest();
        request.setPuntuacion(5);
        request.setComentario("Excelente tutor");

        doNothing().when(service).valorarTutor(2L, 1L, request);

        ResponseEntity<Void> response = controller.valorarTutor(2L, request, user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void feedbackShouldReturnOk() {
        UserDetails user = buildUserDetails("1");
        FeedbackRecomendacionRequest request = new FeedbackRecomendacionRequest();
        request.setEsUtil(true);

        doNothing().when(service).darFeedbackRecomendacion(1L, request, 1L);

        ResponseEntity<Void> response = controller.feedback(1L, request, user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void marcarVistaShouldReturnOk() {
        UserDetails user = buildUserDetails("1");

        doNothing().when(service).marcarComoVista(1L, 1L);

        ResponseEntity<Void> response = controller.marcarVista(1L, user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void eliminarShouldReturnNoContent() {
        UserDetails user = buildUserDetails("1");

        doNothing().when(service).eliminarRecomendacion(1L, 1L);

        ResponseEntity<Void> response = controller.eliminar(1L, user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void actividadShouldReturnAccepted() {
        UserDetails user = buildUserDetails("1");
        RegistrarActividadRequest request = new RegistrarActividadRequest();
        request.setTipoActividad("BUSQUEDA");

        doNothing().when(service).registrarActividad(1L, request);

        ResponseEntity<Void> response = controller.actividad(request, user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

    @Test
    void refreshShouldReturnAccepted() {
        UserDetails user = buildUserDetails("1");

        doNothing().when(service).generarRecomendacionesUsuario(1L);

        ResponseEntity<Void> response = controller.refresh(user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

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
