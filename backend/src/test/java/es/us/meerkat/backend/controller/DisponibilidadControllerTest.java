package es.us.meerkat.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import es.us.meerkat.backend.dto.DisponibilidadTutorResponse;
import es.us.meerkat.backend.repository.TutorRepository;
import es.us.meerkat.backend.service.tutors.DisponibilidadService;

@ExtendWith(MockitoExtension.class)
class DisponibilidadControllerTest {

    @Mock private DisponibilidadService disponibilidadService;
    @Mock private TutorRepository tutorRepository;

    @InjectMocks private DisponibilidadController controller;

    @Test
    void getDisponibilidadesShouldReturnOk() {
        when(disponibilidadService.getDisponibilidades(1L)).thenReturn(List.of());

        ResponseEntity<List<DisponibilidadTutorResponse>> response =
                controller.getDisponibilidades(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void crearShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<DisponibilidadTutorResponse> response = controller.crear(null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
