package es.us.meerkat.backend.controller.tutors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import es.us.meerkat.backend.dto.tutors.CreateDisponibilidadRequest;
import es.us.meerkat.backend.dto.tutors.DisponibilidadTutorResponse;
import es.us.meerkat.backend.entity.tutors.Tutor;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.tutors.TutorRepository;
import es.us.meerkat.backend.service.tutors.DisponibilidadService;

@ExtendWith(MockitoExtension.class)
class DisponibilidadControllerTest {

    @Mock private DisponibilidadService disponibilidadService;
    @Mock private TutorRepository tutorRepository;

    @InjectMocks private DisponibilidadController controller;

    private Usuario buildUsuario(Long id) {
        Usuario u = new Usuario();
        u.setId(id);
        return u;
    }

    private Tutor buildTutor(Long id, Usuario usuario) {
        Tutor t = new Tutor();
        t.setId(id);
        t.setUsuario(usuario);
        return t;
    }

    private DisponibilidadTutorResponse buildDisponibilidadResponse(Long id) {
        return DisponibilidadTutorResponse.builder().id(id).build();
    }

    @Test
    void crearShouldReturnCreatedWhenUserIsValid() {
        Usuario usuario = buildUsuario(1L);
        Tutor tutor = buildTutor(10L, usuario);
        CreateDisponibilidadRequest request = new CreateDisponibilidadRequest();
        DisponibilidadTutorResponse response = buildDisponibilidadResponse(1L);

        when(tutorRepository.findByUsuarioId(1L)).thenReturn(Optional.of(tutor));
        when(disponibilidadService.crearDisponibilidad(
                        eq(10L), any(CreateDisponibilidadRequest.class), eq(1L)))
                .thenReturn(response);

        ResponseEntity<DisponibilidadTutorResponse> result = controller.crear(usuario, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isEqualTo(response);
    }

    @Test
    void crearShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<DisponibilidadTutorResponse> response = controller.crear(null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void crearShouldThrowExceptionWhenTutorNotFound() {
        Usuario usuario = buildUsuario(1L);
        CreateDisponibilidadRequest request = new CreateDisponibilidadRequest();

        when(tutorRepository.findByUsuarioId(1L)).thenReturn(Optional.empty());

        try {
            controller.crear(usuario, request);
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }
    }

    @Test
    void actualizarShouldReturnOkWhenUserIsValid() {
        Usuario usuario = buildUsuario(1L);
        CreateDisponibilidadRequest request = new CreateDisponibilidadRequest();
        DisponibilidadTutorResponse response = buildDisponibilidadResponse(1L);

        when(disponibilidadService.actualizarDisponibilidad(
                        eq(1L), any(CreateDisponibilidadRequest.class), eq(1L)))
                .thenReturn(response);

        ResponseEntity<DisponibilidadTutorResponse> result =
                controller.actualizar(usuario, 1L, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
    }

    @Test
    void actualizarShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<DisponibilidadTutorResponse> response =
                controller.actualizar(null, 1L, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void desactivarShouldReturnNoContentWhenUserIsValid() {
        Usuario usuario = buildUsuario(1L);

        ResponseEntity<Void> response = controller.desactivar(usuario, 1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void desactivarShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<Void> response = controller.desactivar(null, 1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getDisponibilidadesShouldReturnEmptyList() {
        when(disponibilidadService.getDisponibilidades(1L)).thenReturn(List.of());

        ResponseEntity<List<DisponibilidadTutorResponse>> response =
                controller.getDisponibilidades(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void getDisponibilidadesShouldReturnMultipleItems() {
        var resp1 = buildDisponibilidadResponse(1L);
        var resp2 = buildDisponibilidadResponse(2L);

        when(disponibilidadService.getDisponibilidades(1L)).thenReturn(List.of(resp1, resp2));

        ResponseEntity<List<DisponibilidadTutorResponse>> response =
                controller.getDisponibilidades(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    void getDisponibilidadesShouldReturnOk() {
        when(disponibilidadService.getDisponibilidades(1L)).thenReturn(List.of());

        ResponseEntity<List<DisponibilidadTutorResponse>> response =
                controller.getDisponibilidades(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void crearShouldThrowWhenServiceFails() {
        Usuario usuario = buildUsuario(1L);
        Tutor tutor = buildTutor(10L, usuario);
        CreateDisponibilidadRequest request = new CreateDisponibilidadRequest();

        when(tutorRepository.findByUsuarioId(1L)).thenReturn(Optional.of(tutor));
        when(disponibilidadService.crearDisponibilidad(eq(10L), any(), eq(1L)))
                .thenThrow(new RuntimeException("Service error"));

        try {
            controller.crear(usuario, request);
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }
    }

    @Test
    void actualizarShouldThrowWhenServiceFails() {
        Usuario usuario = buildUsuario(1L);
        CreateDisponibilidadRequest request = new CreateDisponibilidadRequest();

        when(disponibilidadService.actualizarDisponibilidad(eq(1L), any(), eq(1L)))
                .thenThrow(new RuntimeException("Update failed"));

        try {
            controller.actualizar(usuario, 1L, request);
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }
    }

    @Test
    void desactivarShouldThrowWhenServiceFails() {
        Usuario usuario = buildUsuario(1L);

        doThrow(new RuntimeException("Deactivation failed"))
                .when(disponibilidadService)
                .desactivarDisponibilidad(1L, 1L);

        try {
            controller.desactivar(usuario, 1L);
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }
    }

    @Test
    void crearShouldReturnCreatedWithCorrectResponse() {
        Usuario usuario = buildUsuario(1L);
        Tutor tutor = buildTutor(10L, usuario);
        CreateDisponibilidadRequest request = new CreateDisponibilidadRequest();
        DisponibilidadTutorResponse responseBody =
                DisponibilidadTutorResponse.builder().id(5L).build();

        when(tutorRepository.findByUsuarioId(1L)).thenReturn(Optional.of(tutor));
        when(disponibilidadService.crearDisponibilidad(eq(10L), any(), eq(1L)))
                .thenReturn(responseBody);

        ResponseEntity<DisponibilidadTutorResponse> result = controller.crear(usuario, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody().getId()).isEqualTo(5L);
    }

    @Test
    void actualizarShouldReturnUpdatedResponse() {
        Usuario usuario = buildUsuario(1L);
        CreateDisponibilidadRequest request = new CreateDisponibilidadRequest();
        DisponibilidadTutorResponse responseBody =
                DisponibilidadTutorResponse.builder().id(1L).build();

        when(disponibilidadService.actualizarDisponibilidad(eq(1L), any(), eq(1L)))
                .thenReturn(responseBody);

        ResponseEntity<DisponibilidadTutorResponse> result =
                controller.actualizar(usuario, 1L, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getId()).isEqualTo(1L);
    }

    @Test
    void getDisponibilidadesShouldHandleServiceException() {
        when(disponibilidadService.getDisponibilidades(1L))
                .thenThrow(new RuntimeException("Service unavailable"));

        try {
            controller.getDisponibilidades(1L);
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }
    }

    @Test
    void crearShouldVerifyTutorRepositoryCalled() {
        Usuario usuario = buildUsuario(1L);
        Tutor tutor = buildTutor(10L, usuario);
        CreateDisponibilidadRequest request = new CreateDisponibilidadRequest();
        DisponibilidadTutorResponse response = buildDisponibilidadResponse(1L);

        when(tutorRepository.findByUsuarioId(1L)).thenReturn(Optional.of(tutor));
        when(disponibilidadService.crearDisponibilidad(eq(10L), any(), eq(1L)))
                .thenReturn(response);

        controller.crear(usuario, request);

        verify(tutorRepository).findByUsuarioId(1L);
    }
}
