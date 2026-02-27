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

import es.us.meerkat.backend.entity.AsistenciaEvento;
import es.us.meerkat.backend.entity.Evento;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.service.AsistenciaEventoService;

/**
 * Tests del controlador de asistencia a eventos.
 *
 * <p>Valida el comportamiento de los endpoints REST de gestión de asistencia.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests del controlador de asistencia a eventos")
class AsistenciaEventoControllerTest {

    @Mock private AsistenciaEventoService asistenciaService;

    @InjectMocks private AsistenciaEventoController asistenciaController;

    private Usuario usuario;
    private Evento evento;
    private AsistenciaEvento asistencia;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("estudiante@example.com");
        usuario.setNombre("Juan Pérez");

        evento = new Evento();
        evento.setId(1L);
        evento.setTitulo("Sesión de Matemáticas");
        evento.setAforo(30);
        evento.setAsistentesConfirmados(5);

        asistencia = new AsistenciaEvento();
        asistencia.setId(1L);
        asistencia.setEvento(evento);
        asistencia.setUsuario(usuario);
        asistencia.setCreatedAt(LocalDateTime.now());
    }

    // ========================
    // CONFIRMAR ASISTENCIA
    // ========================

    @Test
    @DisplayName("POST /me - Debe confirmar asistencia exitosamente")
    void testConfirmarAsistencia_Exito() {
        // Given
        when(asistenciaService.confirmarAsistencia(1L, 1L)).thenReturn(asistencia);

        // When
        ResponseEntity<AsistenciaEvento> response =
                asistenciaController.confirmarAsistencia(1L, 1L);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(asistencia.getId(), response.getBody().getId());
        assertEquals(usuario.getId(), response.getBody().getUsuario().getId());
        verify(asistenciaService).confirmarAsistencia(1L, 1L);
    }

    @Test
    @DisplayName("POST /me - Debe retornar error si el evento no existe")
    void testConfirmarAsistencia_EventoNoExiste() {
        // Given
        when(asistenciaService.confirmarAsistencia(999L, 1L))
                .thenThrow(new RuntimeException("Evento no encontrado"));

        // When & Then
        assertThrows(
                RuntimeException.class, () -> asistenciaController.confirmarAsistencia(999L, 1L));

        verify(asistenciaService).confirmarAsistencia(999L, 1L);
    }

    @Test
    @DisplayName("POST /me - Debe retornar error si el aforo está lleno")
    void testConfirmarAsistencia_AforoLleno() {
        // Given
        when(asistenciaService.confirmarAsistencia(1L, 1L))
                .thenThrow(new RuntimeException("El evento ha alcanzado su aforo máximo"));

        // When & Then
        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> asistenciaController.confirmarAsistencia(1L, 1L));

        assertEquals("El evento ha alcanzado su aforo máximo", exception.getMessage());
        verify(asistenciaService).confirmarAsistencia(1L, 1L);
    }

    // ========================
    // OBTENER ASISTENCIA PROPIA
    // ========================

    @Test
    @DisplayName("GET /me - Debe obtener asistencia propia del usuario")
    void testObtenerAsistenciaPropia_Exito() {
        // Given
        when(asistenciaService.obtenerAsistencia(1L, 1L)).thenReturn(asistencia);

        // When
        ResponseEntity<AsistenciaEvento> response =
                asistenciaController.obtenerAsistenciaPropia(1L, 1L);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(asistencia.getId(), response.getBody().getId());
        verify(asistenciaService).obtenerAsistencia(1L, 1L);
    }

    @Test
    @DisplayName("GET /me - Debe retornar error si no existe asistencia")
    void testObtenerAsistenciaPropia_NoExiste() {
        // Given
        when(asistenciaService.obtenerAsistencia(1L, 999L))
                .thenThrow(new RuntimeException("Asistencia no encontrada"));

        // When & Then
        assertThrows(
                RuntimeException.class,
                () -> asistenciaController.obtenerAsistenciaPropia(1L, 999L));

        verify(asistenciaService).obtenerAsistencia(1L, 999L);
    }

    // ========================
    // CANCELAR ASISTENCIA PROPIA
    // ========================

    @Test
    @DisplayName("DELETE /me - Debe cancelar asistencia propia")
    void testCancelarAsistenciaPropia_Exito() {
        // Given
        doNothing().when(asistenciaService).cancelarAsistencia(1L, 1L);

        // When
        ResponseEntity<Void> response = asistenciaController.cancelarAsistenciaPropia(1L, 1L);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(asistenciaService).cancelarAsistencia(1L, 1L);
    }

    @Test
    @DisplayName("DELETE /me - Debe retornar error si no existe asistencia para cancelar")
    void testCancelarAsistenciaPropia_NoExiste() {
        // Given
        doThrow(new RuntimeException("Asistencia no encontrada"))
                .when(asistenciaService)
                .cancelarAsistencia(1L, 999L);

        // When & Then
        assertThrows(
                RuntimeException.class,
                () -> asistenciaController.cancelarAsistenciaPropia(1L, 999L));

        verify(asistenciaService).cancelarAsistencia(1L, 999L);
    }

    // ========================
    // OBTENER ASISTENTES CONFIRMADOS
    // ========================

    @Test
    @DisplayName("Debe obtener lista de asistentes confirmados para un evento")
    void testObtenerAsistentesConfirmados_Exito() {
        // Given
        Usuario usuario2 = new Usuario();
        usuario2.setId(2L);
        usuario2.setNombre("María García");

        AsistenciaEvento asistencia2 = new AsistenciaEvento();
        asistencia2.setId(2L);
        asistencia2.setEvento(evento);
        asistencia2.setUsuario(usuario2);

        List<AsistenciaEvento> asistentes = List.of(asistencia, asistencia2);
        when(asistenciaService.obtenerAsistentesConfirmados(1L)).thenReturn(asistentes);

        // When
        ResponseEntity<List<AsistenciaEvento>> response =
                asistenciaController.obtenerAsistentesConfirmados(1L);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        verify(asistenciaService).obtenerAsistentesConfirmados(1L);
    }

    @Test
    @DisplayName("Debe devolver lista vacía si no hay asistentes confirmados")
    void testObtenerAsistentesConfirmados_Vacio() {
        // Given
        when(asistenciaService.obtenerAsistentesConfirmados(1L)).thenReturn(List.of());

        // When
        ResponseEntity<List<AsistenciaEvento>> response =
                asistenciaController.obtenerAsistentesConfirmados(1L);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
        verify(asistenciaService).obtenerAsistentesConfirmados(1L);
    }

    // ========================
    // CONTAR ASISTENTES
    // ========================

    @Test
    @DisplayName("Debe contar asistentes confirmados de un evento")
    void testContarAsistentes_Exito() {
        // Given
        when(asistenciaService.contarAsistentesConfirmados(1L)).thenReturn(5L);

        // When
        ResponseEntity<Long> response = asistenciaController.contarAsistentes(1L);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(5L, response.getBody());
        verify(asistenciaService).contarAsistentesConfirmados(1L);
    }

    // ========================
    // VALIDACIÓN DE CAPACIDAD
    // ========================

    @Test
    @DisplayName("Debe validar que hay plazas disponibles en evento")
    void testValidarPlazasDisponibles() {
        // Given
        evento.setAforo(30);
        evento.setAsistentesConfirmados(10);

        // When
        boolean hayPlazas = evento.getAsistentesConfirmados() < evento.getAforo();

        // Then
        assertTrue(hayPlazas);
    }

    @Test
    @DisplayName("Debe validar que no hay plazas cuando aforo está completo")
    void testNoHayPlazasAfroLleno() {
        // Given
        evento.setAforo(10);
        evento.setAsistentesConfirmados(10);

        // When
        boolean hayPlazas = evento.getAsistentesConfirmados() < evento.getAforo();

        // Then
        assertFalse(hayPlazas);
    }
}
