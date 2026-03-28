package es.us.meerkat.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import es.us.meerkat.backend.controller.tutors.SolicitudContratacionController;
import es.us.meerkat.backend.dto.tutors.SolicitudContratacionRequest;
import es.us.meerkat.backend.dto.tutors.SolicitudContratacionResponse;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.service.emails.EmailService;
import es.us.meerkat.backend.service.suscriptions.PaymentService;
import es.us.meerkat.backend.service.tutors.SolicitudContratacionService;
import es.us.meerkat.backend.service.tutors.TutorService;

@ExtendWith(MockitoExtension.class)
class SolicitudContratacionControllerTest {

    @Mock private SolicitudContratacionService solicitudService;
    @Mock private PaymentService paymentService;
    @Mock private TutorService tutorService;
    @Mock private EmailService emailService;

    @InjectMocks private SolicitudContratacionController controller;

    private Usuario buildUsuario(Long id) {
        Usuario u = new Usuario();
        u.setId(id);
        return u;
    }

    @Test
    void crearSolicitudShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> response =
                controller.crearSolicitud(null, 10L, new SolicitudContratacionRequest());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void crearSolicitudShouldReturnCreatedOnSuccess() {
        Usuario usuario = buildUsuario(1L);
        SolicitudContratacionRequest request = new SolicitudContratacionRequest();
        SolicitudContratacionResponse mockResponse = new SolicitudContratacionResponse();

        when(solicitudService.crearSolicitud(eq(1L), eq(10L), any())).thenReturn(mockResponse);

        ResponseEntity<?> response = controller.crearSolicitud(usuario, 10L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(mockResponse);
    }

    @Test
    void crearSolicitudShouldReturnBadRequestOnError() {
        Usuario usuario = buildUsuario(1L);
        when(solicitudService.crearSolicitud(eq(1L), eq(10L), any()))
                .thenThrow(new IllegalArgumentException("Error de validación"));

        ResponseEntity<?> response =
                controller.crearSolicitud(usuario, 10L, new SolicitudContratacionRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void aceptarSolicitudShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> response = controller.aceptarSolicitud(null, 100L);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void aceptarSolicitudShouldReturnOkOnSuccess() {
        Usuario usuario = buildUsuario(2L);
        SolicitudContratacionResponse mockResponse = new SolicitudContratacionResponse();
        when(solicitudService.aceptarSolicitud(100L, 2L)).thenReturn(mockResponse);

        ResponseEntity<?> response = controller.aceptarSolicitud(usuario, 100L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void rechazarSolicitudShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> response = controller.rechazarSolicitud(null, 100L, null);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rechazarSolicitudShouldReturnOkWithReason() {
        Usuario usuario = buildUsuario(2L);
        SolicitudContratacionResponse mockResponse = new SolicitudContratacionResponse();
        when(solicitudService.rechazarSolicitud(100L, 2L, "No disponible"))
                .thenReturn(mockResponse);

        ResponseEntity<?> response =
                controller.rechazarSolicitud(usuario, 100L, Map.of("motivo", "No disponible"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void marcarComoPagadaShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> response = controller.marcarComoPagada(null, 100L);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void marcarComoPagadaShouldReturnOkOnSuccess() {
        Usuario usuario = buildUsuario(1L);
        SolicitudContratacionResponse mockResponse = new SolicitudContratacionResponse();
        when(solicitudService.marcarComoPagada(100L, 1L, null)).thenReturn(mockResponse);

        ResponseEntity<?> response = controller.marcarComoPagada(usuario, 100L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void obtenerPendientesDelTutorShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> response = controller.obtenerPendientesDelTutor(null);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void obtenerPendientesDelTutorShouldReturnListOnSuccess() {
        Usuario usuario = buildUsuario(2L);
        when(solicitudService.obtenerSolicitudesPendientesDelTutor(2L)).thenReturn(List.of());

        ResponseEntity<?> response = controller.obtenerPendientesDelTutor(usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void obtenerSolicitudesDelAlumnoShouldReturnList() {
        Usuario usuario = buildUsuario(1L);
        when(solicitudService.obtenerSolicitudesDelAlumno(1L)).thenReturn(List.of());

        ResponseEntity<?> response = controller.obtenerSolicitudesDelAlumno(usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void cancelarSolicitudShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> response = controller.cancelarSolicitud(null, 100L, null);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
