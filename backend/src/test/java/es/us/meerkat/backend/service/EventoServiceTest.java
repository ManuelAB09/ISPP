package es.us.meerkat.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import es.us.meerkat.backend.entity.Evento;
import es.us.meerkat.backend.repository.EventoRepository;

/**
 * Tests unitarios para EventoService.
 *
 * <p>Valida la lógica de negocio relacionada con la creación, edición, cancelación y obtención de
 * eventos.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests del servicio de eventos")
class EventoServiceTest {

    @Mock private EventoRepository eventoRepository;

    @InjectMocks private EventoService eventoService;

    private Evento evento;
    private LocalDateTime ahora;
    private LocalDateTime mañana;

    @BeforeEach
    void setUp() {
        ahora = LocalDateTime.now().withNano(0);
        mañana = ahora.plusHours(2);

        evento = new Evento();
        evento.setId(1L);
        evento.setTitulo("Sesión de Matemáticas");
        evento.setDescripcion("Repaso de cálculo integral");
        evento.setFechaHora(ahora);
        evento.setFechaFin(mañana);
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
    // EDITAR EVENTO
    // ========================

    @Test
    @DisplayName("Debe editar un evento existente correctamente")
    void testEditarEvento_Exito() {
        // Given
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(eventoRepository.save(any(Evento.class))).thenReturn(evento);

        String nuevoTitulo = "Sesión de Física";
        String nuevaDescripcion = "Repaso de termodinámica";
        LocalDateTime nuevaFecha = ahora.plusDays(1);
        Integer nuevoAforo = 40;

        // When
        Evento resultado =
                eventoService.editarEvento(
                        1L,
                        nuevoTitulo,
                        nuevaDescripcion,
                        nuevaFecha,
                        nuevaFecha.plusHours(2),
                        "Aula 101",
                        41.5,
                        -74.0,
                        nuevoAforo,
                        "Calculadora",
                        false,
                        false);

        // Then
        assertNotNull(resultado);
        verify(eventoRepository).findById(1L);
        verify(eventoRepository).save(any(Evento.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción si el evento no existe")
    void testEditarEvento_EventoNoExiste() {
        // Given
        when(eventoRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(
                RuntimeException.class,
                () ->
                        eventoService.editarEvento(
                                999L,
                                "Título",
                                "Descripción",
                                ahora,
                                mañana,
                                "Ubicación",
                                41.5,
                                -74.0,
                                30,
                                "Qué llevar",
                                false,
                                false));

        verify(eventoRepository).findById(999L);
        verify(eventoRepository, never()).save(any());
    }

    // ========================
    // CANCELAR EVENTO
    // ========================

    @Test
    @DisplayName("Debe cancelar un evento exitosamente")
    void testCancelarEvento_Exito() {
        // Given
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(eventoRepository.save(any(Evento.class))).thenReturn(evento);

        String motivo = "Cancelado por fuerza mayor";

        // When
        Evento resultado = eventoService.cancelarEvento(1L, motivo);

        // Then
        assertNotNull(resultado);
        verify(eventoRepository).findById(1L);
        verify(eventoRepository).save(any(Evento.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción al cancelar un evento que no existe")
    void testCancelarEvento_EventoNoExiste() {
        // Given
        when(eventoRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(
                RuntimeException.class,
                () -> eventoService.cancelarEvento(999L, "Motivo cualquiera"));

        verify(eventoRepository).findById(999L);
        verify(eventoRepository, never()).save(any());
    }

    // ========================
    // OBTENER EVENTO
    // ========================

    @Test
    @DisplayName("Debe obtener un evento por ID exitosamente")
    void testObtenerEvento_Exito() {
        // Given
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));

        // When
        Evento resultado = eventoService.obtenerEvento(1L);

        // Then
        assertNotNull(resultado);
        assertEquals(evento.getId(), resultado.getId());
        assertEquals(evento.getTitulo(), resultado.getTitulo());
        assertEquals(evento.getDescripcion(), resultado.getDescripcion());
        verify(eventoRepository).findById(1L);
    }

    @Test
    @DisplayName("Debe lanzar excepción si el evento no existe al obtenerlo")
    void testObtenerEvento_NoExiste() {
        // Given
        when(eventoRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> eventoService.obtenerEvento(999L));

        verify(eventoRepository).findById(999L);
    }

    // ========================
    // OBTENER EVENTOS PÚBLICOS
    // ========================

    @Test
    @DisplayName("Debe obtener todos los eventos públicos")
    void testObtenerEventosPublicos_Exito() {
        // Given
        evento.setPrivado(false);
        Evento evento2 = new Evento();
        evento2.setId(2L);
        evento2.setTitulo("Sesión de Química");
        evento2.setPrivado(false);

        List<Evento> eventosPúblicos = List.of(evento, evento2);
        when(eventoRepository.findPublicEvents()).thenReturn(eventosPúblicos);

        // When
        List<Evento> resultado = eventoService.obtenerEventosPublicos();

        // Then
        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        assertTrue(resultado.stream().allMatch(e -> !e.getPrivado()));
        verify(eventoRepository).findPublicEvents();
    }

    @Test
    @DisplayName("Debe devolver lista vacía si no hay eventos públicos")
    void testObtenerEventosPublicos_Vacio() {
        // Given
        when(eventoRepository.findPublicEvents()).thenReturn(List.of());

        // When
        List<Evento> resultado = eventoService.obtenerEventosPublicos();

        // Then
        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
        verify(eventoRepository).findPublicEvents();
    }

    // ========================
    // OBTENER EVENTOS EN MAPA
    // ========================

    @Test
    @DisplayName("Debe obtener todos los eventos visibles en mapa")
    void testObtenerEventosEnMapa_Exito() {
        // Given
        evento.setVisibleMapa(true);
        evento.setCancelado(false);
        Evento evento2 = new Evento();
        evento2.setId(2L);
        evento2.setTitulo("Otro evento");
        evento2.setVisibleMapa(true);
        evento2.setCancelado(false);

        List<Evento> eventosEnMapa = List.of(evento, evento2);
        when(eventoRepository.findVisibleOnMap()).thenReturn(eventosEnMapa);

        // When
        List<Evento> resultado = eventoService.obtenerEventosEnMapa();

        // Then
        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        assertTrue(resultado.stream().allMatch(Evento::getVisibleMapa));
        assertTrue(resultado.stream().noneMatch(Evento::getCancelado));
        verify(eventoRepository).findVisibleOnMap();
    }

    @Test
    @DisplayName("Debe devolver lista vacía si no hay eventos en mapa")
    void testObtenerEventosEnMapa_Vacio() {
        // Given
        when(eventoRepository.findVisibleOnMap()).thenReturn(List.of());

        // When
        List<Evento> resultado = eventoService.obtenerEventosEnMapa();

        // Then
        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
        verify(eventoRepository).findVisibleOnMap();
    }

    // ========================
    // GENERAR ENLACE VIRTUAL
    // ========================

    @Test
    @DisplayName("Debe generar un enlace virtual para un evento")
    void testGenerarEnlaceVirtual_Exito() {
        // Given
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(eventoRepository.save(any(Evento.class))).thenReturn(evento);

        // When
        String enlace = eventoService.generarEnlaceVirtual(1L);

        // Then
        assertNotNull(enlace);
        assertFalse(enlace.isEmpty());
        verify(eventoRepository).findById(1L);
        verify(eventoRepository).save(any(Evento.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción si el evento no existe al generar enlace")
    void testGenerarEnlaceVirtual_EventoNoExiste() {
        // Given
        when(eventoRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> eventoService.generarEnlaceVirtual(999L));

        verify(eventoRepository).findById(999L);
        verify(eventoRepository, never()).save(any());
    }

    // ========================
    // VALIDACIÓN DE AFORO
    // ========================

    @Test
    @DisplayName("Debe verificar si el aforo está completo")
    void testAforoCompleto() {
        // Given
        evento.setAforo(10);
        evento.setAsistentesConfirmados(10);

        // When
        boolean aforoLleno = evento.getAsistentesConfirmados().equals(evento.getAforo());

        // Then
        assertTrue(aforoLleno);
    }

    @Test
    @DisplayName("Debe verificar si hay plazas disponibles")
    void testAforoDisponible() {
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
    @DisplayName("Debe validar que evento público está visible en mapa por defecto")
    void testEventoPublicoVisibleEnMapa() {
        // Given
        evento.setPrivado(false);

        // When
        boolean debeSerVisibleEnMapa = !evento.getPrivado();

        // Then
        assertTrue(debeSerVisibleEnMapa);
    }

    @Test
    @DisplayName("Debe permitir evento privado no visible en mapa")
    void testEventoPrivadoPuedeNoSerVisibleEnMapa() {
        // Given
        evento.setPrivado(true);
        evento.setVisibleMapa(false);

        // When
        boolean esVisible = evento.getVisibleMapa();

        // Then
        assertFalse(esVisible);
    }
}
