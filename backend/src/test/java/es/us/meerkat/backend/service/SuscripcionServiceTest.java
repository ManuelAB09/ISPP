package es.us.meerkat.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import es.us.meerkat.backend.entity.Suscripcion;
import es.us.meerkat.backend.entity.TipoPlan;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.SuscripcionRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;

/**
 * Tests unitarios para SuscripcionService.
 *
 * <p>Valida la lógica de negocio relacionada con la gestión de suscripciones de usuarios,
 * incluyendo obtención de planes, suscripción, cancelación y renovación.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests del servicio de suscripciones")
class SuscripcionServiceTest {

    @Mock private SuscripcionRepository suscripcionRepository;

    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks private SuscripcionService suscripcionService;

    private Usuario usuario;
    private Suscripcion suscripcion;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("test@example.com");

        suscripcion =
                Suscripcion.builder()
                        .id(1L)
                        .usuario(usuario)
                        .plan(TipoPlan.PREMIUM)
                        .fechaInicio(LocalDate.now())
                        .fechaFin(LocalDate.now().plusMonths(1))
                        .activa(true)
                        .autoRenovar(true)
                        .build();
    }

    @Test
    @DisplayName("Debe devolver todos los planes disponibles")
    void testObtenerPlanesDisponibles() {
        TipoPlan[] planes = suscripcionService.obtenerPlanesDisponibles();

        assertNotNull(planes);
        assertEquals(2, planes.length);
        assertTrue(contains(planes, TipoPlan.FREE));
        assertTrue(contains(planes, TipoPlan.PREMIUM));
    }

    @Test
    @DisplayName("Debe obtener la suscripción activa de un usuario")
    void testObtenerMiSuscripcion_SuscripcionExiste() {
        when(suscripcionRepository.findByUsuarioIdAndActiva(1L, true))
                .thenReturn(Optional.of(suscripcion));

        Optional<Suscripcion> resultado = suscripcionService.obtenerMiSuscripcion(1L);

        assertTrue(resultado.isPresent());
        assertEquals(suscripcion.getId(), resultado.get().getId());
        assertEquals(TipoPlan.PREMIUM, resultado.get().getPlan());
        verify(suscripcionRepository).findByUsuarioIdAndActiva(1L, true);
    }

    @Test
    @DisplayName("Debe devolver Optional vacío si no hay suscripción activa")
    void testObtenerMiSuscripcion_SuscripcionNoExiste() {
        when(suscripcionRepository.findByUsuarioIdAndActiva(1L, true)).thenReturn(Optional.empty());

        Optional<Suscripcion> resultado = suscripcionService.obtenerMiSuscripcion(1L);

        assertFalse(resultado.isPresent());
        verify(suscripcionRepository).findByUsuarioIdAndActiva(1L, true);
    }

    @Test
    @DisplayName("Debe suscribir a un usuario a plan Premium exitosamente")
    void testSuscribirse_Exito() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(suscripcionRepository.findByUsuarioIdAndActiva(1L, true)).thenReturn(Optional.empty());
        when(suscripcionRepository.save(any(Suscripcion.class))).thenReturn(suscripcion);

        Suscripcion resultado = suscripcionService.suscribirse(1L);

        assertNotNull(resultado);
        assertEquals(TipoPlan.PREMIUM, resultado.getPlan());
        assertTrue(resultado.getActiva());
        verify(usuarioRepository).findById(1L);
        verify(suscripcionRepository).findByUsuarioIdAndActiva(1L, true);
        verify(suscripcionRepository).save(any(Suscripcion.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción si el usuario no existe al suscribirse")
    void testSuscribirse_UsuarioNoExiste() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class, () -> suscripcionService.suscribirse(1L));

        assertEquals("Usuario no encontrado", exception.getMessage());
        verify(usuarioRepository).findById(1L);
        verify(suscripcionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar excepción si el usuario ya tiene suscripción activa")
    void testSuscribirse_YaTieneSuscripcionActiva() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(suscripcionRepository.findByUsuarioIdAndActiva(1L, true))
                .thenReturn(Optional.of(suscripcion));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class, () -> suscripcionService.suscribirse(1L));

        assertEquals("Ya tienes una suscripción activa", exception.getMessage());
        verify(usuarioRepository).findById(1L);
        verify(suscripcionRepository).findByUsuarioIdAndActiva(1L, true);
        verify(suscripcionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe cancelar la suscripción exitosamente")
    void testCancelarSuscripcion_Exito() {
        when(suscripcionRepository.findByUsuarioIdAndActiva(1L, true))
                .thenReturn(Optional.of(suscripcion));
        when(suscripcionRepository.save(any(Suscripcion.class))).thenReturn(suscripcion);

        Suscripcion resultado = suscripcionService.cancelarSuscripcion(1L);

        assertNotNull(resultado);
        verify(suscripcionRepository).findByUsuarioIdAndActiva(1L, true);
        verify(suscripcionRepository).save(suscripcion);
    }

    @Test
    @DisplayName("Debe lanzar excepción si no hay suscripción activa al cancelar")
    void testCancelarSuscripcion_NoTieneSuscripcionActiva() {
        when(suscripcionRepository.findByUsuarioIdAndActiva(1L, true)).thenReturn(Optional.empty());

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> suscripcionService.cancelarSuscripcion(1L));

        assertEquals("No tienes una suscripción activa", exception.getMessage());
        verify(suscripcionRepository).findByUsuarioIdAndActiva(1L, true);
        verify(suscripcionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe renovar la suscripción exitosamente")
    void testRenovarSuscripcion_Exito() {
        when(suscripcionRepository.findByUsuarioId(1L)).thenReturn(Optional.of(suscripcion));
        when(suscripcionRepository.save(any(Suscripcion.class))).thenReturn(suscripcion);

        Suscripcion resultado = suscripcionService.renovarSuscripcion(1L);

        assertNotNull(resultado);
        verify(suscripcionRepository).findByUsuarioId(1L);
        verify(suscripcionRepository).save(suscripcion);
    }

    @Test
    @DisplayName("Debe lanzar excepción si el usuario no tiene suscripción al renovar")
    void testRenovarSuscripcion_NoTieneSuscripcion() {
        when(suscripcionRepository.findByUsuarioId(1L)).thenReturn(Optional.empty());

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> suscripcionService.renovarSuscripcion(1L));

        assertEquals("El usuario no tiene suscripción", exception.getMessage());
        verify(suscripcionRepository).findByUsuarioId(1L);
        verify(suscripcionRepository, never()).save(any());
    }

    /** Método auxiliar para verificar si un array contiene un elemento. */
    private boolean contains(TipoPlan[] array, TipoPlan element) {
        for (TipoPlan item : array) {
            if (item == element) {
                return true;
            }
        }
        return false;
    }
}
