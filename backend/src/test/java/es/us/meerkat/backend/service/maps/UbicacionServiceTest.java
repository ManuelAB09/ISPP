package es.us.meerkat.backend.service.maps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import es.us.meerkat.backend.client.OverpassClient;
import es.us.meerkat.backend.dto.maps.UbicacionRequest;
import es.us.meerkat.backend.dto.maps.UbicacionResponse;
import es.us.meerkat.backend.entity.maps.Ubicacion;
import es.us.meerkat.backend.repository.maps.UbicacionRepository;

@ExtendWith(MockitoExtension.class)
class UbicacionServiceTest {

    @Mock private UbicacionRepository ubicacionRepository;

    @Mock private OverpassClient overpassClient;

    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks private UbicacionService ubicacionService;

    @Test
    void crearUbicacionShouldReturnExistingWhenCoordinatesAlreadyExist() {
        UbicacionRequest request = buildRequest();
        Ubicacion existente =
                Ubicacion.builder()
                        .id(7L)
                        .nombre("Biblioteca Central")
                        .direccion("Calle Real 1")
                        .latitud(37.38)
                        .longitud(-5.99)
                        .tipo("library")
                        .coste("GRATIS")
                        .build();

        when(ubicacionRepository.findByLatitudAndLongitud(37.38, -5.99))
                .thenReturn(Optional.of(existente));

        UbicacionResponse response = ubicacionService.crearUbicacion(request);

        assertThat(response.getId()).isEqualTo(7L);
        assertThat(response.getNombre()).isEqualTo("Biblioteca Central");
    }

    @Test
    void crearUbicacionShouldPersistNewLocationWhenNotExisting() {
        UbicacionRequest request = buildRequest();

        when(ubicacionRepository.findByLatitudAndLongitud(37.38, -5.99))
                .thenReturn(Optional.empty());
        when(ubicacionRepository.save(any(Ubicacion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UbicacionResponse response = ubicacionService.crearUbicacion(request);

        verify(ubicacionRepository).save(any(Ubicacion.class));
        assertThat(response.getNombre()).isEqualTo("Biblioteca Central");
        assertThat(response.getLatitud()).isEqualTo(37.38);
        assertThat(response.getLongitud()).isEqualTo(-5.99);
    }

    @Test
    void buscarSitiosEstudioShouldParseOverpassResponseAndClassifyCost() {
        String json =
                """
                {
                  "elements": [
                    {
                      "lat": 37.4000,
                      "lon": -5.9800,
                      "tags": {
                        "name": "Biblioteca Pública",
                        "amenity": "library",
                        "fee": "no",
                        "addr:street": "Av. Constitución",
                        "addr:housenumber": "1",
                        "addr:city": "Sevilla",
                        "addr:postcode": "41001"
                      }
                    },
                    {
                      "lat": 37.4100,
                      "lon": -5.9700,
                      "tags": {
                        "amenity": "library"
                      }
                    }
                  ]
                }
                """;

        when(overpassClient.ejecutar(any(String.class))).thenReturn(json);

        List<UbicacionResponse> result = ubicacionService.buscarSitiosEstudio(37.38, -5.99, 1200);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNombre()).isEqualTo("Biblioteca Pública");
        assertThat(result.get(0).getCoste()).isEqualTo("GRATIS");
        assertThat(result.get(0).getDireccion()).contains("Av. Constitución 1");
    }

    @Test
    void buscarSitiosEstudioShouldThrowWhenOverpassJsonIsInvalid() {
        when(overpassClient.ejecutar(any(String.class))).thenReturn("{invalid-json");

        assertThatThrownBy(() -> ubicacionService.buscarSitiosEstudio(37.38, -5.99, 1200))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error procesando respuesta Overpass");
    }

    private UbicacionRequest buildRequest() {
        UbicacionRequest request = new UbicacionRequest();
        request.setNombre("Biblioteca Central");
        request.setDireccion("Calle Real 1");
        request.setLatitud(37.38);
        request.setLongitud(-5.99);
        request.setTipo("library");
        request.setCoste("GRATIS");
        return request;
    }

    // ── editarUbicacion ──────────────────────────────────────────────────

    @Test
    void editarUbicacionShouldUpdateFieldsAndSave() {
        Ubicacion existente =
                Ubicacion.builder()
                        .id(5L)
                        .nombre("Old")
                        .direccion("Old St")
                        .latitud(0.0)
                        .longitud(0.0)
                        .tipo("park")
                        .coste("GRATIS")
                        .build();
        when(ubicacionRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(ubicacionRepository.save(any(Ubicacion.class))).thenAnswer(inv -> inv.getArgument(0));

        UbicacionRequest request = buildRequest();
        UbicacionResponse response = ubicacionService.editarUbicacion(5L, request);

        verify(ubicacionRepository).save(existente);
        assertThat(response.getNombre()).isEqualTo("Biblioteca Central");
        assertThat(response.getDireccion()).isEqualTo("Calle Real 1");
    }

    @Test
    void editarUbicacionShouldThrowWhenNotFound() {
        when(ubicacionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ubicacionService.editarUbicacion(99L, buildRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Ubicación no encontrada");
    }

    // ── obtenerUbicacion ─────────────────────────────────────────────────

    @Test
    void obtenerUbicacionShouldReturnResponse() {
        Ubicacion ubicacion =
                Ubicacion.builder()
                        .id(3L)
                        .nombre("Parque")
                        .direccion("Av. Principal")
                        .latitud(37.0)
                        .longitud(-6.0)
                        .tipo("park")
                        .coste("GRATIS")
                        .build();
        when(ubicacionRepository.findById(3L)).thenReturn(Optional.of(ubicacion));

        UbicacionResponse response = ubicacionService.obtenerUbicacion(3L);

        assertThat(response.getId()).isEqualTo(3L);
        assertThat(response.getNombre()).isEqualTo("Parque");
    }

    @Test
    void obtenerUbicacionShouldThrowWhenNotFound() {
        when(ubicacionRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ubicacionService.obtenerUbicacion(404L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Ubicación no encontrada");
    }

    // ── eliminarUbicacion ────────────────────────────────────────────────

    @Test
    void eliminarUbicacionShouldDeleteWhenExists() {
        when(ubicacionRepository.existsById(10L)).thenReturn(true);

        ubicacionService.eliminarUbicacion(10L);

        verify(ubicacionRepository).deleteById(10L);
    }

    @Test
    void eliminarUbicacionShouldThrowWhenNotFound() {
        when(ubicacionRepository.existsById(10L)).thenReturn(false);

        assertThatThrownBy(() -> ubicacionService.eliminarUbicacion(10L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Ubicación no encontrada");

        verify(ubicacionRepository, never()).deleteById(10L);
    }

    // ── obtenerTodas ─────────────────────────────────────────────────────

    @Test
    void obtenerTodasShouldReturnAllUbicaciones() {
        Ubicacion u1 = Ubicacion.builder().id(1L).nombre("A").build();
        Ubicacion u2 = Ubicacion.builder().id(2L).nombre("B").build();
        when(ubicacionRepository.findAll()).thenReturn(List.of(u1, u2));

        List<Ubicacion> result = ubicacionService.obtenerTodas();

        assertThat(result).hasSize(2);
    }
}
