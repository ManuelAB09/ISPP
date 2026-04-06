package es.us.meerkat.backend.controller.recommendations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import es.us.meerkat.backend.service.recommendations.ValoracionService;

@ExtendWith(MockitoExtension.class)
class ValoracionControllerTest {

    @Mock private ValoracionService valoracionService;

    @InjectMocks private ValoracionController controller;

    @Test
    void checkAlreadyRatedShouldReturnRatedFlag() {
        when(valoracionService.isAlreadyRated(10L, 20L)).thenReturn(true);

        ResponseEntity<Map<String, Boolean>> response = controller.checkAlreadyRated(10L, 20L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("rated", true);
    }

    @Test
    void obtenerEstadisticasShouldMapNullValuesToZero() {
        when(valoracionService.obtenerMediaPorProfesor(5L)).thenReturn(null);
        when(valoracionService.contarValoracionesPorProfesor(5L)).thenReturn(null);
        when(valoracionService.calcularNivel(5L)).thenReturn("NOVATO");

        ResponseEntity<Map<String, Object>> response = controller.obtenerEstadisticas(5L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("media", 0.0).containsEntry("total", 0L);
    }

    @Test
    void checkAlreadyRatedShouldReturnFalseWhenNotRated() {
        when(valoracionService.isAlreadyRated(11L, 21L)).thenReturn(false);

        ResponseEntity<Map<String, Boolean>> response = controller.checkAlreadyRated(11L, 21L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("rated", false);
    }

    @Test
    void obtenerEstadisticasShouldReturnProperStatsWithValues() {
        when(valoracionService.obtenerMediaPorProfesor(7L)).thenReturn(4.5);
        when(valoracionService.contarValoracionesPorProfesor(7L)).thenReturn(10L);
        when(valoracionService.calcularNivel(7L)).thenReturn("EXPERTO");

        ResponseEntity<Map<String, Object>> response = controller.obtenerEstadisticas(7L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("media")).isEqualTo(4.5);
        assertThat(response.getBody().get("total")).isEqualTo(10L);
        assertThat(response.getBody().get("nivel")).isEqualTo("EXPERTO");
    }

    @Test
    void checkAlreadyRatedShouldReturnOkStatus() {
        when(valoracionService.isAlreadyRated(12L, 22L)).thenReturn(true);

        ResponseEntity<Map<String, Boolean>> response = controller.checkAlreadyRated(12L, 22L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }
}
