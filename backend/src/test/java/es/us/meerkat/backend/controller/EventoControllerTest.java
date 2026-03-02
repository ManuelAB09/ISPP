package es.us.meerkat.backend.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import es.us.meerkat.backend.dto.EventDetailResponse;
import es.us.meerkat.backend.dto.EventSummaryResponse;
import es.us.meerkat.backend.entity.Evento;
import es.us.meerkat.backend.service.EventoService;

/**
 * Tests del controlador de eventos.
 *
 * <p>Valida que el controlador convierte correctamente las entidades a DTOs.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests del controlador de eventos")
class EventoControllerTest {

    @Mock private EventoService eventoService;

    @InjectMocks private EventoController eventoController;

    private Evento evento;
    private LocalDateTime ahora;

    @BeforeEach
    void setUp() {
        ahora = LocalDateTime.now().withNano(0);

        evento = new Evento();
        evento.setId(1L);
        evento.setTitulo("Sesión de Matemáticas");
        evento.setDescripcion("Repaso de cálculo integral");
        evento.setFechaHora(ahora);
        evento.setFechaFin(ahora.plusHours(2));
        evento.setAforo(30);
        evento.setAsistentesConfirmados(10);
        evento.setQueLlevar("Libreta y bolígrafo");
        evento.setEsVirtual(false);
        evento.setPrivado(false);
        evento.setCancelado(false);
        evento.setVisibleMapa(true);
    }

    // ========================
    // OBTENER EVENTO BY ID
    // ========================

    @Test
    @DisplayName("GET /{eventId} - Debe convertir a EventDetailResponse")
    void testObtenerEvento_Exito() {
        // Given
        when(eventoService.obtenerEvento(1L)).thenReturn(evento);

        // When
        ResponseEntity<EventDetailResponse> response = eventoController.obtenerEvento(1L);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());
        assertEquals("Sesión de Matemáticas", response.getBody().getTitulo());
        verify(eventoService).obtenerEvento(1L);
    }

    @Test
    @DisplayName("GET /{eventId} - Debe lanzar exception si no existe")
    void testObtenerEvento_NoExiste() {
        // Given
        when(eventoService.obtenerEvento(999L))
                .thenThrow(new RuntimeException("Evento no encontrado"));

        // When & Then
        assertThrows(RuntimeException.class, () -> eventoController.obtenerEvento(999L));
        verify(eventoService).obtenerEvento(999L);
    }

    // ========================
    // LISTAR EVENTOS PÚBLICOS
    // ========================

    @Test
    @DisplayName("GET / - Debe convertir lista a EventSummaryResponse")
    void testListarEventos_Exito() {
        // Given
        evento.setPrivado(false);
        Evento evento2 = new Evento();
        evento2.setId(2L);
        evento2.setTitulo("Sesión de Física");
        evento2.setPrivado(false);
        evento2.setFechaHora(ahora.plusDays(1));
        evento2.setFechaFin(ahora.plusDays(1).plusHours(2));

        List<Evento> eventosPublicos = List.of(evento, evento2);
        when(eventoService.obtenerEventosPublicos()).thenReturn(eventosPublicos);

        // When
        ResponseEntity<List<EventSummaryResponse>> response = eventoController.listarEventos();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals("Sesión de Matemáticas", response.getBody().get(0).getTitulo());
        assertEquals("Sesión de Física", response.getBody().get(1).getTitulo());
        verify(eventoService).obtenerEventosPublicos();
    }

    @Test
    @DisplayName("GET / - Debe devolver lista vacía")
    void testListarEventos_Vacio() {
        // Given
        when(eventoService.obtenerEventosPublicos()).thenReturn(List.of());

        // When
        ResponseEntity<List<EventSummaryResponse>> response = eventoController.listarEventos();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
        verify(eventoService).obtenerEventosPublicos();
    }

    // ========================
    // OBTENER EVENTOS EN MAPA
    // ========================

    @Test
    @DisplayName("GET /map - Debe convertir a EventSummaryResponse")
    void testObtenerEventosEnMapa_Exito() {
        // Given
        evento.setVisibleMapa(true);
        evento.setCancelado(false);
        List<Evento> eventosEnMapa = List.of(evento);
        when(eventoService.obtenerEventosEnMapa()).thenReturn(eventosEnMapa);

        // When
        ResponseEntity<List<EventSummaryResponse>> response =
                eventoController.obtenerEventosEnMapa();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verify(eventoService).obtenerEventosEnMapa();
    }

    @Test
    @DisplayName("GET /map - Debe devolver lista vacía")
    void testObtenerEventosEnMapa_Vacio() {
        // Given
        when(eventoService.obtenerEventosEnMapa()).thenReturn(List.of());

        // When
        ResponseEntity<List<EventSummaryResponse>> response =
                eventoController.obtenerEventosEnMapa();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
        verify(eventoService).obtenerEventosEnMapa();
    }

    // ========================
    // UBICACIONES RECOMENDADAS
    // ========================

    @Test
    @DisplayName("GET /recommended-locations - Debe retornar lista de ubicaciones")
    void testObtenerUbicacionesRecomendadas() {
        // Given
        List<String> ubicaciones = List.of("Biblioteca Central", "Aula 101", "Cafetería");
        when(eventoService.obtenerUbicacionesRecomendadas()).thenReturn(ubicaciones);

        // When
        ResponseEntity<List<String>> response = eventoController.obtenerUbicacionesRecomendadas();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(3, response.getBody().size());
        assertTrue(response.getBody().contains("Biblioteca Central"));
        verify(eventoService).obtenerUbicacionesRecomendadas();
    }
}
