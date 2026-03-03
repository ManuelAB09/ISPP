package es.us.meerkat.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import es.us.meerkat.backend.client.OverpassClient;
import es.us.meerkat.backend.dto.UbicacionRequest;
import es.us.meerkat.backend.dto.UbicacionResponse;
import es.us.meerkat.backend.entity.Ubicacion;
import es.us.meerkat.backend.repository.UbicacionRepository;

@ExtendWith(MockitoExtension.class)
class UbicacionServiceTest {

    @Mock private UbicacionRepository ubicacionRepository;

    @Mock private OverpassClient overpassClient;

    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks private UbicacionService ubicacionService;

    @Test
    void crearUbicacion_retornaExistenteSiMismasCoordenadas() {
        UbicacionRequest request = buildRequest();

        Ubicacion existente =
                Ubicacion.builder()
                        .id(10L)
                        .nombre("Biblioteca")
                        .direccion("Calle A")
                        .latitud(37.38)
                        .longitud(-5.99)
                        .tipo("library")
                        .coste("GRATIS")
                        .build();

        when(ubicacionRepository.findByLatitudAndLongitud(37.38, -5.99))
                .thenReturn(Optional.of(existente));

        UbicacionResponse response = ubicacionService.crearUbicacion(request);

        assertEquals(10L, response.getId());
        assertEquals("Biblioteca", response.getNombre());
        assertEquals("Calle A", response.getDireccion());
        assertEquals(37.38, response.getLatitud());
        assertEquals(-5.99, response.getLongitud());
        verify(ubicacionRepository, never()).save(any(Ubicacion.class));
    }

    @Test
    void crearUbicacion_creaYGuardaSiNoExiste() {
        UbicacionRequest request = buildRequest();

        when(ubicacionRepository.findByLatitudAndLongitud(37.38, -5.99))
                .thenReturn(Optional.empty());

        UbicacionResponse response = ubicacionService.crearUbicacion(request);

        assertEquals("Biblioteca", response.getNombre());
        assertEquals("Calle A", response.getDireccion());
        assertEquals(37.38, response.getLatitud());
        assertEquals(-5.99, response.getLongitud());
        verify(ubicacionRepository).save(any(Ubicacion.class));
    }

    @Test
    void editarUbicacion_actualizaYGuarda() {
        Ubicacion existente =
                Ubicacion.builder()
                        .id(5L)
                        .nombre("Antiguo")
                        .direccion("Vieja")
                        .latitud(1.0)
                        .longitud(2.0)
                        .tipo("old")
                        .coste("DESCONOCIDO")
                        .build();

        UbicacionRequest request = buildRequest();

        when(ubicacionRepository.findById(5L)).thenReturn(Optional.of(existente));

        UbicacionResponse response = ubicacionService.editarUbicacion(5L, request);

        assertEquals(5L, response.getId());
        assertEquals("Biblioteca", response.getNombre());
        assertEquals("Calle A", response.getDireccion());
        assertEquals(37.38, response.getLatitud());
        assertEquals(-5.99, response.getLongitud());
        verify(ubicacionRepository).save(existente);
    }

    @Test
    void editarUbicacion_lanzaSiNoExiste() {
        when(ubicacionRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex =
                assertThrows(
                        RuntimeException.class,
                        () -> ubicacionService.editarUbicacion(99L, buildRequest()));

        assertEquals("Ubicación no encontrada", ex.getMessage());
    }

    @Test
    void obtenerUbicacion_devuelveDto() {
        Ubicacion ubicacion =
                Ubicacion.builder()
                        .id(7L)
                        .nombre("Lugar")
                        .direccion("Dir")
                        .latitud(10.0)
                        .longitud(20.0)
                        .tipo("park")
                        .coste("GRATIS")
                        .build();

        when(ubicacionRepository.findById(7L)).thenReturn(Optional.of(ubicacion));

        UbicacionResponse response = ubicacionService.obtenerUbicacion(7L);

        assertEquals(7L, response.getId());
        assertEquals("Lugar", response.getNombre());
        assertEquals("Dir", response.getDireccion());
        assertEquals(10.0, response.getLatitud());
        assertEquals(20.0, response.getLongitud());
    }

    @Test
    void obtenerUbicacion_lanzaSiNoExiste() {
        when(ubicacionRepository.findById(8L)).thenReturn(Optional.empty());

        RuntimeException ex =
                assertThrows(RuntimeException.class, () -> ubicacionService.obtenerUbicacion(8L));

        assertEquals("Ubicación no encontrada", ex.getMessage());
    }

    @Test
    void obtenerTodas_retornaListaRepositorio() {
        List<Ubicacion> lista =
                List.of(
                        Ubicacion.builder()
                                .id(1L)
                                .nombre("A")
                                .direccion("D1")
                                .latitud(1.0)
                                .longitud(1.0)
                                .tipo("park")
                                .coste("GRATIS")
                                .build(),
                        Ubicacion.builder()
                                .id(2L)
                                .nombre("B")
                                .direccion("D2")
                                .latitud(2.0)
                                .longitud(2.0)
                                .tipo("library")
                                .coste("GRATIS")
                                .build());

        when(ubicacionRepository.findAll()).thenReturn(lista);

        List<Ubicacion> resultado = ubicacionService.obtenerTodas();

        assertEquals(2, resultado.size());
        assertEquals("A", resultado.get(0).getNombre());
        assertEquals("B", resultado.get(1).getNombre());
    }

    @Test
    void eliminarUbicacion_eliminaSiExiste() {
        when(ubicacionRepository.existsById(3L)).thenReturn(true);

        ubicacionService.eliminarUbicacion(3L);

        verify(ubicacionRepository).deleteById(3L);
    }

    @Test
    void eliminarUbicacion_lanzaSiNoExiste() {
        when(ubicacionRepository.existsById(4L)).thenReturn(false);

        RuntimeException ex =
                assertThrows(RuntimeException.class, () -> ubicacionService.eliminarUbicacion(4L));

        assertEquals("Ubicación no encontrada", ex.getMessage());
        verify(ubicacionRepository, never()).deleteById(4L);
    }

    @Test
    void buscarSitiosEstudio_parseaTodosLosCasosYConstruyeQuery() {
        String overpassJson =
                """
                {
                  "elements": [
                    {
                      "lat": 37.111111,
                      "lon": -5.111111,
                      "tags": {
                        "name": "Biblioteca Centro",
                        "amenity": "library",
                        "fee": "no",
                        "addr:street": "Calle Real",
                        "addr:housenumber": "12",
                        "addr:city": "Sevilla",
                        "addr:postcode": "41001"
                      }
                    },
                    {
                      "lat": 37.2,
                      "lon": -5.2,
                      "tags": {
                        "name": "Universidad Pública",
                        "amenity": "university",
                        "operator": "public"
                      }
                    },
                    {
                      "lat": 37.21,
                      "lon": -5.21,
                      "tags": {
                        "name": "Universidad Privada",
                        "amenity": "university",
                        "operator": "private"
                      }
                    },
                    {
                      "lat": 37.3,
                      "lon": -5.3,
                      "tags": {
                        "name": "Coworking Sur",
                        "amenity": "coworking_space"
                      }
                    },
                    {
                      "lat": 37.31,
                      "lon": -5.31,
                      "tags": {
                        "name": "Studio Pro",
                        "amenity": "studio"
                      }
                    },
                    {
                      "lat": 37.4,
                      "lon": -5.4,
                      "tags": {
                        "name": "Parque Norte",
                        "leisure": "park"
                      }
                    },
                    {
                      "lat": 37.41,
                      "lon": -5.41,
                      "tags": {
                        "name": "Playground Kids",
                        "leisure": "playground"
                      }
                    },
                    {
                      "lat": 37.5,
                      "lon": -5.5,
                      "tags": {
                        "name": "Centro Privado",
                        "amenity": "training",
                        "access": "private"
                      }
                    },
                    {
                      "lat": 37.51,
                      "lon": -5.51,
                      "tags": {
                        "name": "Biblioteca Premium",
                        "amenity": "library",
                        "fee": "yes"
                      }
                    },
                    {
                      "lat": 37.52,
                      "lon": -5.52,
                      "tags": {
                        "name": "Training Sin Datos",
                        "amenity": "training"
                      }
                    },
                    {
                      "lat": 37.53,
                      "lon": -5.53,
                      "tags": {
                        "name": "Lugar Sin Tipo"
                      }
                    },
                    {
                      "lat": 37.6,
                      "lon": -5.6,
                      "tags": {
                        "amenity": "library"
                      }
                    }
                  ]
                }
                """;

        when(overpassClient.ejecutar(any(String.class))).thenReturn(overpassJson);

        List<UbicacionResponse> resultado =
                ubicacionService.buscarSitiosEstudio(37.123456, -5.987654, 500);

        assertEquals(11, resultado.size());

        assertEquals("library", resultado.get(0).getTipo());
        assertEquals("GRATIS", resultado.get(0).getCoste());
        assertEquals("Calle Real 12, Sevilla 41001", resultado.get(0).getDireccion());

        assertEquals("PARCIALMENTE_GRATIS", resultado.get(1).getCoste());
        assertEquals("DESCONOCIDO", resultado.get(2).getCoste());
        assertEquals("PROBABLEMENTE_DE_PAGO", resultado.get(3).getCoste());
        assertEquals("PROBABLEMENTE_DE_PAGO", resultado.get(4).getCoste());
        assertEquals("GRATIS", resultado.get(5).getCoste());
        assertEquals("GRATIS", resultado.get(6).getCoste());
        assertEquals("DE_PAGO", resultado.get(7).getCoste());
        assertEquals("DE_PAGO", resultado.get(8).getCoste());
        assertEquals("DESCONOCIDO", resultado.get(9).getCoste());
        assertEquals("desconocido", resultado.get(10).getTipo());
        assertEquals("Dirección no disponible", resultado.get(10).getDireccion());

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(overpassClient).ejecutar(queryCaptor.capture());
        String query = queryCaptor.getValue();
        assertTrue(query.contains("around:500,37.123456,-5.987654"));
        assertTrue(query.contains("node[\"amenity\"=\"library\"]"));
        assertTrue(query.contains("node[\"leisure\"=\"park\"]"));
    }

    @Test
    void buscarSitiosEstudio_lanzaRuntimeSiJsonInvalido() {
        when(overpassClient.ejecutar(any(String.class))).thenReturn("{ json-invalido }");

        RuntimeException ex =
                assertThrows(
                        RuntimeException.class,
                        () -> ubicacionService.buscarSitiosEstudio(37.0, -5.0, 100));

        assertEquals("Error procesando respuesta Overpass", ex.getMessage());
        assertNotNull(ex.getCause());
    }

    private UbicacionRequest buildRequest() {
        UbicacionRequest request = new UbicacionRequest();
        request.setNombre("Biblioteca");
        request.setDireccion("Calle A");
        request.setLatitud(37.38);
        request.setLongitud(-5.99);
        request.setTipo("library");
        request.setCoste("GRATIS");
        return request;
    }
}
