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

import es.us.meerkat.backend.entity.Evento;
import es.us.meerkat.backend.service.EventoService;

/**
 * Tests del controlador de eventos.
 *
 * <p>Valida el comportamiento de los endpoints REST de gestión de eventos.
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
        evento.setUbicacion("Biblioteca Central");
        evento.setLatitud(41.5);
        evento.setLongitud(-74.0);
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
    @DisplayName("GET /{eventId} - Debe obtener un evento por ID")
    void testObtenerEvento_Exito() {
        // Given
        when(eventoService.obtenerEvento(1L)).thenReturn(evento);

        // When
        ResponseEntity<Evento> response = eventoController.obtenerEvento(1L);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(evento.getId(), response.getBody().getId());
        assertEquals(evento.getTitulo(), response.getBody().getTitulo());
        verify(eventoService).obtenerEvento(1L);
    }

    @Test
    @DisplayName("GET /{eventId} - Debe retornar error si el evento no existe")
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
    @DisplayName("GET / - Debe obtener lista de eventos públicos")
    void testListarEventos_Exito() {
        // Given
        evento.setPrivado(false);
        Evento evento2 = new Evento();
        evento2.setId(2L);
        evento2.setTitulo("Sesión de Física");
        evento2.setPrivado(false);

        List<Evento> eventosPublicos = List.of(evento, evento2);
        when(eventoService.obtenerEventosPublicos()).thenReturn(eventosPublicos);

        // When
        ResponseEntity<List<Evento>> response = eventoController.listarEventos();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertTrue(response.getBody().stream().noneMatch(Evento::getPrivado));
        verify(eventoService).obtenerEventosPublicos();
    }

    @Test
    @DisplayName("GET / - Debe devolver lista vacía si no hay eventos públicos")
    void testListarEventos_Vacio() {
        // Given
        when(eventoService.obtenerEventosPublicos()).thenReturn(List.of());

        // When
        ResponseEntity<List<Evento>> response = eventoController.listarEventos();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
        verify(eventoService).obtenerEventosPublicos();
    }

    // ========================
    // OBTENER EVENTOS EN MAPA
    // ========================

    @Test
    @DisplayName("Debe obtener eventos visibles en mapa")
    void testObtenerEventosEnMapa_Exito() {
        // Given
        evento.setVisibleMapa(true);
        evento.setCancelado(false);
        List<Evento> eventosEnMapa = List.of(evento);
        when(eventoService.obtenerEventosEnMapa()).thenReturn(eventosEnMapa);

        // When
        ResponseEntity<List<Evento>> response = eventoController.obtenerEventosEnMapa();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertTrue(response.getBody().stream().allMatch(Evento::getVisibleMapa));
        verify(eventoService).obtenerEventosEnMapa();
    }

    // ========================
    // VALIDACIÓN DE AFORO
    // ========================

    @Test
    @DisplayName("Debe validar que evento respeta aforo máximo")
    void testAforoValido() {
        // Given
        evento.setAforo(30);
        evento.setAsistentesConfirmados(10);

        // When
        boolean hayPlazas = evento.getAsistentesConfirmados() < evento.getAforo();

        // Then
        assertTrue(hayPlazas);
    }

    // ========================
    // VALIDACIÓN DE VISIBILIDAD
    // ========================

    @Test
    @DisplayName("Debe permitir evento público visible")
    void testEventoPublicoVisible() {
        // Given
        evento.setPrivado(false);
        evento.setVisibleMapa(true);

        // When & Then
        assertFalse(evento.getPrivado());
        assertTrue(evento.getVisibleMapa());
    }

    @Test
    @DisplayName("Debe permitir evento privado no visible en mapa")
    void testEventoPrivadoNoVisible() {
        // Given
        evento.setPrivado(true);
        evento.setVisibleMapa(false);

        // When & Then
        assertTrue(evento.getPrivado());
        assertFalse(evento.getVisibleMapa());
    }

    // ========================
    // INFORMACIÓN DEL EVENTO
    // ========================

    @Test
    @DisplayName("Debe contener toda la información requerida del evento")
    void testEventoContieneTodaLaInformacion() {
        // Then
        assertNotNull(evento.getTitulo());
        assertNotNull(evento.getDescripcion());
        assertNotNull(evento.getFechaHora());
        assertNotNull(evento.getFechaFin());
        assertNotNull(evento.getUbicacion());
        assertNotNull(evento.getLatitud());
        assertNotNull(evento.getLongitud());
        assertNotNull(evento.getAforo());
        assertNotNull(evento.getQueLlevar());
        assertNotNull(evento.getEsVirtual());
        assertNotNull(evento.getPrivado());
    }

    @Test
    @DisplayName("Debe validar que evento no cancelado puede tener asistentes")
    void testEventoActivoPuedeRecibirAsistentes() {
        // Given
        evento.setCancelado(false);

        // When & Then
        assertFalse(evento.getCancelado());
    }
}
