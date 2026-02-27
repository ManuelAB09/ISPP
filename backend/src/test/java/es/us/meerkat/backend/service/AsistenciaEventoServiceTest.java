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

import es.us.meerkat.backend.entity.AsistenciaEvento;
import es.us.meerkat.backend.entity.Evento;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.AsistenciaEventoRepository;
import es.us.meerkat.backend.repository.EventoRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;

/**
 * Tests unitarios para AsistenciaEventoService.
 *
 * <p>Valida la lógica de negocio relacionada con la confirmación, cancelación y obtención de
 * asistencias a eventos.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests del servicio de asistencia a eventos")
class AsistenciaEventoServiceTest {

    @Mock private AsistenciaEventoRepository asistenciaRepository;

    @Mock private EventoRepository eventoRepository;

    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks private AsistenciaEventoService asistenciaService;

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
        evento.setDescripcion("Repaso de cálculo");
        evento.setFechaHora(LocalDateTime.now().plusHours(1));
        evento.setFechaFin(LocalDateTime.now().plusHours(3));
        evento.setAforo(30);
        evento.setAsistentesConfirmados(5);
        evento.setCancelado(false);
        evento.setPrivado(false);

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
    @DisplayName("Debe confirmar la asistencia de un usuario a un evento")
    void testConfirmarAsistencia_Exito() {
        // Given
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(asistenciaRepository.findByEventoAndUsuario(1L, 1L)).thenReturn(Optional.empty());
        when(asistenciaRepository.save(any(AsistenciaEvento.class))).thenReturn(asistencia);

        // When
        AsistenciaEvento resultado = asistenciaService.confirmarAsistencia(1L, 1L);

        // Then
        assertNotNull(resultado);
        assertEquals(usuario.getId(), resultado.getUsuario().getId());
        assertEquals(evento.getId(), resultado.getEvento().getId());
        verify(eventoRepository).findById(1L);
        verify(usuarioRepository).findById(1L);
        verify(asistenciaRepository).save(any(AsistenciaEvento.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción si el evento no existe al confirmar asistencia")
    void testConfirmarAsistencia_EventoNoExiste() {
        // Given
        when(eventoRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> asistenciaService.confirmarAsistencia(999L, 1L));

        verify(eventoRepository).findById(999L);
        verify(usuarioRepository, never()).findById(anyLong());
        verify(asistenciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar excepción si el usuario no existe al confirmar asistencia")
    void testConfirmarAsistencia_UsuarioNoExiste() {
        // Given
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> asistenciaService.confirmarAsistencia(1L, 999L));

        verify(eventoRepository).findById(1L);
        verify(usuarioRepository).findById(999L);
        verify(asistenciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar excepción si el evento alcanzó el aforo máximo")
    void testConfirmarAsistencia_AforoLleno() {
        // Given
        evento.setAforo(5);
        evento.setAsistentesConfirmados(5);

        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        // When & Then
        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> asistenciaService.confirmarAsistencia(1L, 1L));

        assertEquals("El evento ha alcanzado su aforo máximo", exception.getMessage());
        verify(eventoRepository).findById(1L);
        verify(asistenciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe confirmar asistencia existente nuevamente")
    void testConfirmarAsistencia_YaExistente() {
        // Given
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(asistenciaRepository.findByEventoAndUsuario(1L, 1L))
                .thenReturn(Optional.of(asistencia));
        when(asistenciaRepository.save(any(AsistenciaEvento.class))).thenReturn(asistencia);

        // When
        AsistenciaEvento resultado = asistenciaService.confirmarAsistencia(1L, 1L);

        // Then
        assertNotNull(resultado);
        verify(eventoRepository).findById(1L);
        verify(asistenciaRepository).save(any(AsistenciaEvento.class));
    }

    // ========================
    // CANCELAR ASISTENCIA
    // ========================

    @Test
    @DisplayName("Debe cancelar la asistencia de un usuario")
    void testCancelarAsistencia_Exito() {
        // Given
        when(asistenciaRepository.findByEventoAndUsuario(1L, 1L))
                .thenReturn(Optional.of(asistencia));
        when(asistenciaRepository.save(any(AsistenciaEvento.class))).thenReturn(asistencia);

        // When
        asistenciaService.cancelarAsistencia(1L, 1L);

        // Then
        verify(asistenciaRepository).findByEventoAndUsuario(1L, 1L);
        verify(asistenciaRepository).save(any(AsistenciaEvento.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción si la asistencia no existe al cancelar")
    void testCancelarAsistencia_NoExiste() {
        // Given
        when(asistenciaRepository.findByEventoAndUsuario(1L, 1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> asistenciaService.cancelarAsistencia(1L, 1L));

        verify(asistenciaRepository).findByEventoAndUsuario(1L, 1L);
        verify(asistenciaRepository, never()).save(any());
    }

    // ========================
    // OBTENER ASISTENCIA
    // ========================

    @Test
    @DisplayName("Debe obtener la asistencia de un usuario a un evento")
    void testObtenerAsistencia_Exito() {
        // Given
        when(asistenciaRepository.findByEventoAndUsuario(1L, 1L))
                .thenReturn(Optional.of(asistencia));

        // When
        AsistenciaEvento resultado = asistenciaService.obtenerAsistencia(1L, 1L);

        // Then
        assertNotNull(resultado);
        assertEquals(asistencia.getId(), resultado.getId());
        verify(asistenciaRepository).findByEventoAndUsuario(1L, 1L);
    }

    @Test
    @DisplayName("Debe lanzar excepción si la asistencia no existe")
    void testObtenerAsistencia_NoExiste() {
        // Given
        when(asistenciaRepository.findByEventoAndUsuario(1L, 999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> asistenciaService.obtenerAsistencia(1L, 999L));

        verify(asistenciaRepository).findByEventoAndUsuario(1L, 999L);
    }

    // ========================
    // OBTENER ASISTENTES CONFIRMADOS
    // ========================

    @Test
    @DisplayName("Debe obtener la lista de asistentes confirmados a un evento")
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
        when(asistenciaRepository.findConfirmedAttendanceByEvent(1L)).thenReturn(asistentes);

        // When
        List<AsistenciaEvento> resultado = asistenciaService.obtenerAsistentesConfirmados(1L);

        // Then
        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        verify(asistenciaRepository).findConfirmedAttendanceByEvent(1L);
    }

    @Test
    @DisplayName("Debe devolver lista vacía si no hay asistentes confirmados")
    void testObtenerAsistentesConfirmados_Vacio() {
        // Given
        when(asistenciaRepository.findConfirmedAttendanceByEvent(1L)).thenReturn(List.of());

        // When
        List<AsistenciaEvento> resultado = asistenciaService.obtenerAsistentesConfirmados(1L);

        // Then
        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
        verify(asistenciaRepository).findConfirmedAttendanceByEvent(1L);
    }

    // ========================
    // OBTENER ASISTENCIAS DEL EVENTO
    // ========================

    @Test
    @DisplayName("Debe obtener todas las asistencias de un evento")
    void testObtenerAsistenciasEvento_Exito() {
        // Given
        List<AsistenciaEvento> asistencias = List.of(asistencia);
        when(asistenciaRepository.findByEventoId(1L)).thenReturn(asistencias);

        // When
        List<AsistenciaEvento> resultado = asistenciaService.obtenerAsistenciasEvento(1L);

        // Then
        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        verify(asistenciaRepository).findByEventoId(1L);
    }

    // ========================
    // CONTAR ASISTENTES CONFIRMADOS
    // ========================

    @Test
    @DisplayName("Debe contar el número de asistentes confirmados")
    void testContarAsistentesConfirmados_Exito() {
        // Given
        when(asistenciaRepository.countConfirmedByEvent(1L)).thenReturn(10L);

        // When
        long resultado = asistenciaService.contarAsistentesConfirmados(1L);

        // Then
        assertEquals(10L, resultado);
        verify(asistenciaRepository).countConfirmedByEvent(1L);
    }

    @Test
    @DisplayName("Debe retornar cero si no hay asistentes confirmados")
    void testContarAsistentesConfirmados_Cero() {
        // Given
        when(asistenciaRepository.countConfirmedByEvent(1L)).thenReturn(0L);

        // When
        long resultado = asistenciaService.contarAsistentesConfirmados(1L);

        // Then
        assertEquals(0L, resultado);
        verify(asistenciaRepository).countConfirmedByEvent(1L);
    }
}
