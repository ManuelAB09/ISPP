package es.us.meerkat.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import es.us.meerkat.backend.service.forms.CuestionarioService;

@ExtendWith(MockitoExtension.class)
class CuestionarioControllerTest {

    @Mock private CuestionarioService cuestionarioService;

    @InjectMocks private CuestionarioController controller;

    @Test
    void listMineShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseEntity<?> response = controller.listMine(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getByIdShouldReturnNotFoundWhenCuestionarioDoesNotExist() {
        when(cuestionarioService.findById(99L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.getById(99L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
