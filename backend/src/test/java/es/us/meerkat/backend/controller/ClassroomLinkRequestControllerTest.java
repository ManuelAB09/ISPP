package es.us.meerkat.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import es.us.meerkat.backend.controller.google.ClassroomLinkRequestController;
import es.us.meerkat.backend.dto.google.ClassroomLinkRequestResponse;
import es.us.meerkat.backend.dto.google.CompleteClassroomLinkRequest;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.service.google.ClassroomLinkRequestService;

@ExtendWith(MockitoExtension.class)
class ClassroomLinkRequestControllerTest {

    @Mock private ClassroomLinkRequestService service;

    @InjectMocks private ClassroomLinkRequestController controller;

    private Usuario buildUsuario(Long id) {
        Usuario u = new Usuario();
        u.setId(id);
        return u;
    }

    @Test
    void listarSolicitudesPendientesShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> response = controller.listarSolicitudesPendientes(null);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void listarSolicitudesPendientesShouldReturnOkWithList() {
        Usuario tutor = buildUsuario(1L);
        ClassroomLinkRequestResponse item =
                new ClassroomLinkRequestResponse(1L, 10L, 1L, "PENDIENTE", null);

        when(service.listarSolicitudesPendientes(1L)).thenReturn(List.of(item));

        ResponseEntity<List<ClassroomLinkRequestResponse>> response =
                controller.listarSolicitudesPendientes(tutor);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void completarSolicitudShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> response =
                controller.completarSolicitud(
                        1L, null, new CompleteClassroomLinkRequest("c1", "Course"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void completarSolicitudShouldReturnOkOnSuccess() {
        Usuario tutor = buildUsuario(1L);
        CompleteClassroomLinkRequest request =
                new CompleteClassroomLinkRequest("c1", "Math Course");

        ResponseEntity<?> response = controller.completarSolicitud(100L, tutor, request);

        verify(service).completarSolicitud(100L, 1L, "c1", "Math Course");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
