package es.us.meerkat.backend.service.maps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import es.us.meerkat.backend.service.maps.UbicacionService;

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
}
