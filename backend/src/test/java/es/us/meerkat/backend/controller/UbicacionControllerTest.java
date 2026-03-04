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

import es.us.meerkat.backend.dto.UbicacionRequest;
import es.us.meerkat.backend.dto.UbicacionResponse;
import es.us.meerkat.backend.entity.Ubicacion;
import es.us.meerkat.backend.service.UbicacionService;

@ExtendWith(MockitoExtension.class)
class UbicacionControllerTest {

    @Mock private UbicacionService ubicacionService;

    @InjectMocks private UbicacionController ubicacionController;

    @Test
    void crearUbicacionShouldReturnCreated() {
        UbicacionRequest request = new UbicacionRequest();
        request.setNombre("Biblioteca Central");
        request.setDireccion("Calle Real 1");
        request.setLatitud(37.38);
        request.setLongitud(-5.99);

        UbicacionResponse created =
                UbicacionResponse.builder().id(1L).nombre("Biblioteca Central").build();
        when(ubicacionService.crearUbicacion(request)).thenReturn(created);

        ResponseEntity<UbicacionResponse> response = ubicacionController.crearUbicacion(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(created);
        verify(ubicacionService).crearUbicacion(request);
    }

    @Test
    void editarUbicacionShouldReturnOk() {
        UbicacionRequest request = new UbicacionRequest();
        request.setNombre("Coworking Nuevo");

        UbicacionResponse updated =
                UbicacionResponse.builder().id(2L).nombre("Coworking Nuevo").build();
        when(ubicacionService.editarUbicacion(2L, request)).thenReturn(updated);

        ResponseEntity<UbicacionResponse> response =
                ubicacionController.editarUbicacion(2L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(updated);
    }

    @Test
    void buscarSitiosEstudioShouldReturnRecommendations() {
        List<UbicacionResponse> recomendaciones =
                List.of(
                        UbicacionResponse.builder().nombre("Biblioteca Pública").build(),
                        UbicacionResponse.builder().nombre("Espacio Coworking").build());
        when(ubicacionService.buscarSitiosEstudio(37.38, -5.99, 1500)).thenReturn(recomendaciones);

        ResponseEntity<List<UbicacionResponse>> response =
                ubicacionController.buscarSitiosEstudio(37.38, -5.99, 1500);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    void listarTodasShouldReturnAllStoredLocations() {
        List<Ubicacion> ubicaciones =
                List.of(Ubicacion.builder().id(1L).nombre("Biblioteca").build());
        when(ubicacionService.obtenerTodas()).thenReturn(ubicaciones);

        List<Ubicacion> response = ubicacionController.listarTodas();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getNombre()).isEqualTo("Biblioteca");
    }
}
