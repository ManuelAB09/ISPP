package es.us.meerkat.backend.controller.tutors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import es.us.meerkat.backend.controller.tutors.TutorController;
import es.us.meerkat.backend.dto.tutors.CreateTutorRequest;
import es.us.meerkat.backend.dto.tutors.TutorResponse;
import es.us.meerkat.backend.dto.tutors.UpdateTutorRequest;
import es.us.meerkat.backend.entity.tutors.Tutor;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.service.suscriptions.PaymentService;
import es.us.meerkat.backend.service.tutors.TutorService;

@ExtendWith(MockitoExtension.class)
class TutorControllerTest {

    @Mock private TutorService tutorService;
    @Mock private PaymentService paymentService;

    @InjectMocks private TutorController tutorController;

    private Usuario buildUsuario(Long id) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setEmail("user@test.es");
        return u;
    }

    private Tutor buildTutor(Long id, Usuario usuario) {
        Tutor t = new Tutor();
        t.setId(id);
        t.setUsuario(usuario);
        t.setEspecialidades(List.of("Matemáticas"));
        t.setBio("Tutor de prueba");
        t.setVerificado(true);
        return t;
    }

    @Test
    void createTutorShouldReturnCreatedOnSuccess() {
        Usuario usuario = buildUsuario(1L);
        CreateTutorRequest request = new CreateTutorRequest();
        Tutor tutor = buildTutor(10L, usuario);

        when(tutorService.crearPerfil(eq(1L), any(CreateTutorRequest.class))).thenReturn(tutor);

        ResponseEntity<?> response = tutorController.createTutor(usuario, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void createTutorShouldReturnBadRequestOnError() {
        Usuario usuario = buildUsuario(1L);
        when(tutorService.crearPerfil(eq(1L), any(CreateTutorRequest.class)))
                .thenThrow(new RuntimeException("Ya tienes perfil"));

        ResponseEntity<?> response = tutorController.createTutor(usuario, new CreateTutorRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getMyTutorProfileShouldReturnOkWhenExists() {
        Usuario usuario = buildUsuario(1L);
        Tutor tutor = buildTutor(10L, usuario);

        when(tutorService.obtenerTutorPorUsuarioId(1L)).thenReturn(Optional.of(tutor));

        ResponseEntity<TutorResponse> response = tutorController.getMyTutorProfile(usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void getMyTutorProfileShouldReturnNotFoundWhenNotExists() {
        Usuario usuario = buildUsuario(1L);
        when(tutorService.obtenerTutorPorUsuarioId(1L)).thenReturn(Optional.empty());

        ResponseEntity<TutorResponse> response = tutorController.getMyTutorProfile(usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getTutorByIdShouldReturnOkWhenExists() {
        Usuario usuario = buildUsuario(1L);
        Tutor tutor = buildTutor(10L, usuario);

        when(tutorService.obtenerTutorPorId(10L)).thenReturn(Optional.of(tutor));

        ResponseEntity<TutorResponse> response = tutorController.getTutorById(10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getTutorByIdShouldReturnNotFoundWhenNotExists() {
        when(tutorService.obtenerTutorPorId(10L)).thenReturn(Optional.empty());

        ResponseEntity<TutorResponse> response = tutorController.getTutorById(10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateMyTutorProfileShouldReturnOkOnSuccess() {
        Usuario usuario = buildUsuario(1L);
        Tutor tutor = buildTutor(10L, usuario);
        UpdateTutorRequest request = new UpdateTutorRequest();

        when(tutorService.actualizarPerfil(eq(1L), any())).thenReturn(tutor);

        ResponseEntity<TutorResponse> response =
                tutorController.updateMyTutorProfile(usuario, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void updateMyTutorProfileShouldReturnBadRequestOnError() {
        Usuario usuario = buildUsuario(1L);
        when(tutorService.actualizarPerfil(eq(1L), any())).thenThrow(new RuntimeException("Error"));

        ResponseEntity<TutorResponse> response =
                tutorController.updateMyTutorProfile(usuario, new UpdateTutorRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
