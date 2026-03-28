package es.us.meerkat.backend.controller;

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

import es.us.meerkat.backend.controller.recommendations.ValoracionController;
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
}
