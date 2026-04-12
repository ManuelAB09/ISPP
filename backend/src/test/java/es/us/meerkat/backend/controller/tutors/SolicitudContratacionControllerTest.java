package es.us.meerkat.backend.controller.tutors;

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

import es.us.meerkat.backend.dto.tutors.SolicitudContratacionRequest;
import es.us.meerkat.backend.dto.tutors.SolicitudContratacionResponse;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.service.emails.EmailService;
import es.us.meerkat.backend.service.subscriptions.PaymentService;
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

    @Test
    void reservarDirectaShouldReturnCreatedOnSuccess() {
        Usuario usuario = buildUsuario(1L);
        SolicitudContratacionRequest request = new SolicitudContratacionRequest();
        SolicitudContratacionResponse mockResponse = new SolicitudContratacionResponse();

        when(solicitudService.reservarDirecta(eq(1L), eq(10L), any())).thenReturn(mockResponse);

        ResponseEntity<?> response = controller.reservarDirecta(usuario, 10L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void reservarDirectaShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> response = controller.reservarDirecta(null, 10L, null);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void reservarDirectaShouldReturnBadRequestOnError() {
        Usuario usuario = buildUsuario(1L);
        when(solicitudService.reservarDirecta(eq(1L), eq(10L), any()))
                .thenThrow(new IllegalArgumentException("No disponible"));

        ResponseEntity<?> response =
                controller.reservarDirecta(usuario, 10L, new SolicitudContratacionRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void obtenerSolicitudesDelTutorShouldReturnListOnSuccess() {
        Usuario usuario = buildUsuario(2L);
        when(solicitudService.obtenerSolicitudesDelTutor(2L)).thenReturn(List.of());

        ResponseEntity<?> response = controller.obtenerSolicitudesDelTutor(usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void obtenerSolicitudesDelTutorShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> response = controller.obtenerSolicitudesDelTutor(null);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void obtenerSolicitudesDelAlumnoShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> response = controller.obtenerSolicitudesDelAlumno(null);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void cancelarSolicitudShouldReturnOkOnSuccess() {
        Usuario usuario = buildUsuario(2L);
        SolicitudContratacionResponse mockResponse = new SolicitudContratacionResponse();
        when(solicitudService.cancelarSolicitud(100L, 2L, null)).thenReturn(mockResponse);

        ResponseEntity<?> response = controller.cancelarSolicitud(usuario, 100L, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void reprogramarSolicitudShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> response =
                controller.reprogramarSolicitud(
                        null, 100L, Map.of("dia", "2025-12-25", "horaInicio", "10:00"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void reprogramarSolicitudShouldReturnOkOnSuccess() {
        Usuario usuario = buildUsuario(2L);
        SolicitudContratacionResponse mockResponse = new SolicitudContratacionResponse();
        when(solicitudService.reprogramarSolicitud(eq(100L), eq(2L), any(), any(), any()))
                .thenReturn(mockResponse);

        ResponseEntity<?> response =
                controller.reprogramarSolicitud(
                        usuario,
                        100L,
                        Map.of("dia", "2025-12-25", "horaInicio", "10:00", "horaFin", "11:00"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void marcarComoPagadaShouldReturnBadRequestOnError() {
        Usuario usuario = buildUsuario(1L);
        when(solicitudService.marcarComoPagada(100L, 1L, null))
                .thenThrow(new IllegalArgumentException("Solicitud no válida"));

        ResponseEntity<?> response = controller.marcarComoPagada(usuario, 100L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void cancelarPorAlumnoShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> response = controller.cancelarPorAlumno(null, 100L, null);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void cancelarPorAlumnoShouldReturnOkOnSuccess() {
        Usuario usuario = buildUsuario(1L);
        when(solicitudService.cancelarPorAlumno(100L, 1L, null))
                .thenReturn(new SolicitudContratacionResponse());

        ResponseEntity<?> response = controller.cancelarPorAlumno(usuario, 100L, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void calificarSolicitudShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> response = controller.calificarSolicitud(null, 100L, Map.of());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void calificarSolicitudShouldReturnOkOnSuccess() {
        Usuario usuario = buildUsuario(1L);
        when(solicitudService.calificarSolicitud(100L, 1L, 5, "Excelente"))
                .thenReturn(new SolicitudContratacionResponse());

        ResponseEntity<?> response =
                controller.calificarSolicitud(
                        usuario, 100L, Map.of("calificacion", 5, "comentario", "Excelente"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void aprobarReprogramacionShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> response = controller.aprobarReprogramacion(null, 100L);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void aprobarReprogramacionShouldReturnOkOnSuccess() {
        Usuario usuario = buildUsuario(1L);
        when(solicitudService.aprobarReprogramacion(100L, 1L))
                .thenReturn(new SolicitudContratacionResponse());

        ResponseEntity<?> response = controller.aprobarReprogramacion(usuario, 100L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void rechazarReprogramacionShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> response = controller.rechazarReprogramacion(null, 100L);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rechazarReprogramacionShouldReturnOkOnSuccess() {
        Usuario usuario = buildUsuario(1L);
        when(solicitudService.rechazarReprogramacion(100L, 1L))
                .thenReturn(new SolicitudContratacionResponse());

        ResponseEntity<?> response = controller.rechazarReprogramacion(usuario, 100L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
