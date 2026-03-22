package es.us.meerkat.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import es.us.meerkat.backend.dto.RecomendacionListResponse;
import es.us.meerkat.backend.entity.RecomendacionComunidad;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.service.RecomendacionComunidadService;

@ExtendWith(MockitoExtension.class)
class RecomendacionControllerTest {

    @Mock private RecomendacionComunidadService recomendacionService;

    @InjectMocks private RecomendacionController controller;

    private Usuario buildUsuario(Long id) {
        Usuario u = new Usuario();
        u.setId(id);
        return u;
    }

    @Test
    void getRecomendacionesShouldReturnOk() {
        Usuario usuario = buildUsuario(1L);
        RecomendacionComunidad rec = new RecomendacionComunidad();

        when(recomendacionService.getRecomendacionesUsuario(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of(rec)));

        ResponseEntity<RecomendacionListResponse> response =
                controller.getRecomendaciones(0, 20, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().recomendaciones()).hasSize(1);
    }

    @Test
    void getRecomendacionesNoVistasShouldReturnOk() {
        Usuario usuario = buildUsuario(1L);
        when(recomendacionService.getRecomendacionesNoVistas(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of()));

        ResponseEntity<RecomendacionListResponse> response =
                controller.getRecomendacionesNoVistas(0, 20, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().recomendaciones()).isEmpty();
    }

    @Test
    void marcarComoVistaShouldReturnOk() {
        Usuario usuario = buildUsuario(1L);

        ResponseEntity<?> response = controller.marcarComoVista(100L, usuario);

        verify(recomendacionService).marcarComoVista(100L);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void descartarRecomendacionShouldReturnNoContent() {
        Usuario usuario = buildUsuario(1L);

        ResponseEntity<Void> response = controller.descartarRecomendacion(100L, usuario);

        verify(recomendacionService).eliminarRecomendacion(100L);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
}
