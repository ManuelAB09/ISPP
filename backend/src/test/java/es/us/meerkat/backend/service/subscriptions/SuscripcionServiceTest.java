package es.us.meerkat.backend.service.subscriptions;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import es.us.meerkat.backend.entity.communities.Comunidad;
import es.us.meerkat.backend.entity.communities.Institution;
import es.us.meerkat.backend.entity.subscriptions.Suscripcion;
import es.us.meerkat.backend.entity.subscriptions.TipoPlan;
import es.us.meerkat.backend.entity.subscriptions.TipoPlanComunidad;
import es.us.meerkat.backend.entity.subscriptions.TipoTransaccion;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.communities.ComunidadRepository;
import es.us.meerkat.backend.repository.communities.InstitutionRepository;
import es.us.meerkat.backend.repository.subscriptions.SuscripcionRepository;
import es.us.meerkat.backend.repository.users.UsuarioRepository;

/**
 * Tests unitarios para SuscripcionService.
 *
 * <p>Valida la lógica de negocio relacionada con la gestión de suscripciones de usuarios,
 * incluyendo obtención de planes, suscripción, cancelación y renovación.
 */
@DisplayName("Tests del servicio de suscripciones")
@ExtendWith(MockitoExtension.class)
class SuscripcionServiceTest {

    @Mock private SuscripcionRepository suscripcionRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private InstitutionRepository institutionRepository;
    @Mock private PaymentService paymentService;
    @Mock private ComunidadRepository comunidadRepository;

    @InjectMocks private SuscripcionService suscripcionService;

    private Usuario usuario;
    private Suscripcion suscripcion;

    @BeforeEach
    void setUp() {
        suscripcionService =
                new SuscripcionService(
                        suscripcionRepository,
                        usuarioRepository,
                        institutionRepository,
                        paymentService,
                        comunidadRepository);

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
        assertEquals(3, planes.length);
        assertTrue(contains(planes, TipoPlan.FREE));
        assertTrue(contains(planes, TipoPlan.PREMIUM));
        assertTrue(contains(planes, TipoPlan.PRO));
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

    @Test
    void obtenerPlanesDisponiblesShouldReturnAllPlans() {
        TipoPlan[] planes = suscripcionService.obtenerPlanesDisponibles();

        assertThat(planes).isNotNull().isNotEmpty().contains(TipoPlan.FREE, TipoPlan.PREMIUM);
    }

    @Test
    void obtenerMiSuscripcionShouldReturnActiveSuscripcion() {
        Long usuarioId = 1L;
        Suscripcion suscripcion = buildSuscripcion(1L, usuarioId, true);

        when(suscripcionRepository.findByUsuarioIdAndActiva(usuarioId, true))
                .thenReturn(Optional.of(suscripcion));

        Optional<Suscripcion> result = suscripcionService.obtenerMiSuscripcion(usuarioId);

        assertThat(result).isPresent().contains(suscripcion);
    }

    @Test
    void obtenerMiSuscripcionShouldReturnEmptyWhenNoActiveSuscripcion() {
        Long usuarioId = 1L;

        when(suscripcionRepository.findByUsuarioIdAndActiva(usuarioId, true))
                .thenReturn(Optional.empty());

        Optional<Suscripcion> result = suscripcionService.obtenerMiSuscripcion(usuarioId);

        assertThat(result).isEmpty();
    }

    @Test
    void suscribirseShouldCreateActiveSuscripcion() {
        Long usuarioId = 1L;
        Usuario usuario = buildUsuario(usuarioId);
        Suscripcion.suscribir();

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(suscripcionRepository.findByUsuarioIdAndActiva(usuarioId, true))
                .thenReturn(Optional.empty());
        when(suscripcionRepository.save(any(Suscripcion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Suscripcion result = suscripcionService.suscribirse(usuarioId);

        ArgumentCaptor<Suscripcion> captor = ArgumentCaptor.forClass(Suscripcion.class);
        verify(suscripcionRepository).save(captor.capture());

        assertThat(result).isNotNull();
        assertThat(captor.getValue().getUsuario()).isEqualTo(usuario);
    }

    @Test
    void suscribirseShouldFailWhenUsuarioNotFound() {
        Long usuarioId = 999L;

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> suscripcionService.suscribirse(usuarioId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Usuario no encontrado");
    }

    @Test
    void suscribirseShouldFailWhenUsuarioAlreadyHasActiveSuscripcion() {
        Long usuarioId = 1L;
        Usuario usuario = buildUsuario(usuarioId);
        Suscripcion existenteSuscripcion = buildSuscripcion(1L, usuarioId, true);

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(suscripcionRepository.findByUsuarioIdAndActiva(usuarioId, true))
                .thenReturn(Optional.of(existenteSuscripcion));

        assertThatThrownBy(() -> suscripcionService.suscribirse(usuarioId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya tienes una suscripción activa");
    }

    @Test
    void cancelarSuscripcionShouldDisableAutoRenovarButKeepActiveUntilPeriodEnd() {
        Long usuarioId = 1L;
        Suscripcion suscripcion = buildSuscripcion(1L, usuarioId, true);

        when(suscripcionRepository.findByUsuarioIdAndActiva(usuarioId, true))
                .thenReturn(Optional.of(suscripcion));
        when(suscripcionRepository.save(any(Suscripcion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        suscripcionService.cancelarSuscripcion(usuarioId);

        ArgumentCaptor<Suscripcion> captor = ArgumentCaptor.forClass(Suscripcion.class);
        verify(suscripcionRepository).save(captor.capture());

        Suscripcion saved = captor.getValue();
        assertThat(saved.getAutoRenovar()).isFalse();
        assertThat(saved.getActiva()).isTrue();
        assertThat(saved.getPlan()).isEqualTo(TipoPlan.PREMIUM);
    }

    @Test
    void cancelarSuscripcionShouldFailWhenNoActiveSuscripcion() {
        Long usuarioId = 1L;

        when(suscripcionRepository.findByUsuarioIdAndActiva(usuarioId, true))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> suscripcionService.cancelarSuscripcion(usuarioId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No tienes una suscripción activa");
    }

    @Test
    void cancelarSuscripcionShouldDowngradePremiumCommunitiesToFree() {
        Long usuarioId = 1L;
        Suscripcion suscripcion = buildSuscripcion(1L, usuarioId, true);
        Comunidad comunidadPremium =
                Comunidad.builder()
                        .id(10L)
                        .tipoPlan(TipoPlanComunidad.PREMIUM)
                        .maxMiembros(75)
                        .build();

        when(suscripcionRepository.findByUsuarioIdAndActiva(usuarioId, true))
                .thenReturn(Optional.of(suscripcion));
        when(suscripcionRepository.save(any(Suscripcion.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(comunidadRepository.findByCreadorId(usuarioId)).thenReturn(List.of(comunidadPremium));

        suscripcionService.cancelarSuscripcion(usuarioId);

        assertThat(comunidadPremium.getTipoPlan()).isEqualTo(TipoPlanComunidad.FREE);
        assertThat(comunidadPremium.getMaxMiembros()).isEqualTo(30);
        verify(comunidadRepository).save(comunidadPremium);
    }

    @Test
    void cancelarSuscripcionShouldNotTouchFreeOrUnlimitedCommunities() {
        Long usuarioId = 1L;
        Suscripcion suscripcion = buildSuscripcion(1L, usuarioId, true);
        Comunidad comunidadFree =
                Comunidad.builder()
                        .id(11L)
                        .tipoPlan(TipoPlanComunidad.FREE)
                        .maxMiembros(30)
                        .build();

        when(suscripcionRepository.findByUsuarioIdAndActiva(usuarioId, true))
                .thenReturn(Optional.of(suscripcion));
        when(suscripcionRepository.save(any(Suscripcion.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(comunidadRepository.findByCreadorId(usuarioId)).thenReturn(List.of(comunidadFree));

        suscripcionService.cancelarSuscripcion(usuarioId);

        assertThat(comunidadFree.getTipoPlan()).isEqualTo(TipoPlanComunidad.FREE);
        verify(comunidadRepository, never()).save(comunidadFree);
    }

    @Test
    void renovarSuscripcionShouldRenewSuscripcion() {
        Long usuarioId = 1L;
        Suscripcion suscripcion = buildSuscripcion(1L, usuarioId, false);

        when(suscripcionRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(suscripcion));
        when(suscripcionRepository.save(any(Suscripcion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Suscripcion result = suscripcionService.renovarSuscripcion(usuarioId);

        verify(suscripcionRepository).save(any(Suscripcion.class));
        assertThat(result).isNotNull();
    }

    @Test
    void renovarSuscripcionShouldFailWhenNoSuscripcion() {
        Long usuarioId = 999L;

        when(suscripcionRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> suscripcionService.renovarSuscripcion(usuarioId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("El usuario no tiene suscripción");
    }

    @Test
    void activarSuscripcionTrasStripeShouldCreateNewSuscripcion() {
        Long usuarioId = 1L;
        Usuario usuario = buildUsuario(usuarioId);
        BigDecimal monto = new BigDecimal("9.99");

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(suscripcionRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.empty());
        when(suscripcionRepository.save(any(Suscripcion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        suscripcionService.activarSuscripcionTrasStripe(usuarioId, monto);

        ArgumentCaptor<Suscripcion> captor = ArgumentCaptor.forClass(Suscripcion.class);
        verify(suscripcionRepository).save(captor.capture());

        assertThat(captor.getValue().getUsuario()).isEqualTo(usuario);
    }

    @Test
    void activarSuscripcionTrasStripeShouldRenewExistingSuscripcion() {
        Long usuarioId = 1L;
        Usuario usuario = buildUsuario(usuarioId);
        Suscripcion existente = buildSuscripcion(1L, usuarioId, false);
        BigDecimal monto = new BigDecimal("9.99");

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(suscripcionRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(existente));
        when(suscripcionRepository.save(any(Suscripcion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        suscripcionService.activarSuscripcionTrasStripe(usuarioId, monto);

        verify(suscripcionRepository).save(any(Suscripcion.class));
    }

    @Test
    void activarSuscripcionTrasStripeShouldRecordPaymentTransaction() {
        Long usuarioId = 1L;
        Usuario usuario = buildUsuario(usuarioId);
        BigDecimal monto = new BigDecimal("9.99");

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(suscripcionRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.empty());
        when(suscripcionRepository.save(any(Suscripcion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        suscripcionService.activarSuscripcionTrasStripe(usuarioId, monto);

        verify(paymentService)
                .procesarPagoExitoso(
                        usuarioId,
                        TipoTransaccion.SUSCRIPCION,
                        monto,
                        "Suscripción PREMIUM activada vía Stripe",
                        null);
    }

    @Test
    void activarSuscripcionTrasStripeShouldUpdateUserPlan() {
        Long usuarioId = 1L;
        Usuario usuario = buildUsuario(usuarioId);
        usuario.setPlan(TipoPlan.FREE);
        BigDecimal monto = new BigDecimal("9.99");

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(suscripcionRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.empty());
        when(suscripcionRepository.save(any(Suscripcion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        suscripcionService.activarSuscripcionTrasStripe(usuarioId, monto);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());

        assertThat(captor.getValue().getPlan()).isEqualTo(TipoPlan.PREMIUM);
    }

    @Test
    void renovarSuscripcionTrasStripeShouldRenewSuscripcion() {
        Long usuarioId = 1L;
        Usuario usuario = buildUsuario(usuarioId);
        Suscripcion suscripcion = buildSuscripcion(1L, usuarioId, true);
        BigDecimal monto = new BigDecimal("9.99");

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(suscripcionRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(suscripcion));
        when(suscripcionRepository.save(any(Suscripcion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        suscripcionService.renovarSuscripcionTrasStripe(usuarioId, monto);

        verify(suscripcionRepository).save(any(Suscripcion.class));
    }

    @Test
    void renovarSuscripcionTrasStripeShouldRecordPaymentTransaction() {
        Long usuarioId = 1L;
        Usuario usuario = buildUsuario(usuarioId);
        Suscripcion suscripcion = buildSuscripcion(1L, usuarioId, true);
        BigDecimal monto = new BigDecimal("9.99");

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(suscripcionRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(suscripcion));
        when(suscripcionRepository.save(any(Suscripcion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        suscripcionService.renovarSuscripcionTrasStripe(usuarioId, monto);

        verify(paymentService)
                .procesarPagoExitoso(
                        usuarioId,
                        TipoTransaccion.SUSCRIPCION,
                        monto,
                        "Renovación PREMIUM vía Stripe",
                        null);
    }

    @Test
    void renovarSuscripcionTrasStripeShouldFailWhenSuscripcionNotFound() {
        Long usuarioId = 999L;
        BigDecimal monto = new BigDecimal("9.99");

        when(suscripcionRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> suscripcionService.renovarSuscripcionTrasStripe(usuarioId, monto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No se encontró suscripción para renovar");
    }

    // Helper methods
    private Usuario buildUsuario(Long id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNombre("Test User");
        usuario.setEmail("test@example.com");
        usuario.setPassword("password");
        usuario.setPlan(TipoPlan.FREE);
        return usuario;
    }

    private Suscripcion buildSuscripcion(Long id, Long usuarioId, boolean activa) {
        Usuario usuario = buildUsuario(usuarioId);
        Suscripcion suscripcion = new Suscripcion();
        suscripcion.setId(id);
        suscripcion.setUsuario(usuario);
        suscripcion.setActiva(activa);
        suscripcion.setFechaInicio(LocalDate.now());
        suscripcion.setFechaFin(LocalDate.now().plusMonths(1));
        return suscripcion;
    }

    // ================================================================
    // obtenerMiSuscripcionCompleta
    // ================================================================

    @Test
    void obtenerMiSuscripcionCompletaShouldReturnFreePlanWhenNoSuscripcion() {
        when(suscripcionRepository.findByUsuarioIdAndActiva(1L, true)).thenReturn(Optional.empty());
        when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());

        var response = suscripcionService.obtenerMiSuscripcionCompleta(1L);

        assertThat(response.getPlan()).isEqualTo(TipoPlan.FREE);
        assertThat(response.getActiva()).isTrue();
    }

    @Test
    void obtenerMiSuscripcionCompletaShouldReturnExistingSuscripcion() {
        Suscripcion sub = buildSuscripcion(1L, 1L, true);
        sub.setPlan(TipoPlan.PREMIUM);
        sub.setPeriodo("MENSUAL");
        when(suscripcionRepository.findByUsuarioIdAndActiva(1L, true)).thenReturn(Optional.of(sub));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());

        var response = suscripcionService.obtenerMiSuscripcionCompleta(1L);

        assertThat(response.getPlan()).isEqualTo(TipoPlan.PREMIUM);
    }

    @Test
    void obtenerMiSuscripcionCompletaShouldEnrichWithInstitutionData() {
        when(suscripcionRepository.findByUsuarioIdAndActiva(1L, true)).thenReturn(Optional.empty());

        Usuario usuario = buildUsuario(1L);
        Institution institution =
                Institution.builder()
                        .id(100L)
                        .nombre("MIT")
                        .planActivo(true)
                        .planCorporativo(
                                es.us.meerkat.backend.entity.subscriptions.TipoPlanCorporativo
                                        .PREMIUM)
                        .fechaFinPlan(java.time.LocalDateTime.now().plusDays(30))
                        .build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(institutionRepository
                        .findFirstByUsuarioAdminIdAndPlanActivoTrueOrderByFechaFinPlanDesc(1L))
                .thenReturn(Optional.of(institution));

        var response = suscripcionService.obtenerMiSuscripcionCompleta(1L);

        assertThat(response.getInstitutionNombre()).isEqualTo("MIT");
        assertThat(response.getInstitutionId()).isEqualTo(100L);
        assertThat(response.getPlanCorporativoActivo()).isTrue();
    }

    @Test
    void obtenerMiSuscripcionCompletaShouldSetCorporatePlanInactiveWhenExpired() {
        when(suscripcionRepository.findByUsuarioIdAndActiva(1L, true)).thenReturn(Optional.empty());

        Usuario usuario = buildUsuario(1L);
        Institution institution =
                Institution.builder()
                        .id(100L)
                        .nombre("OldUni")
                        .planActivo(true)
                        .planCorporativo(
                                es.us.meerkat.backend.entity.subscriptions.TipoPlanCorporativo
                                        .BASICO)
                        .fechaFinPlan(java.time.LocalDateTime.now().minusDays(1))
                        .build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(institutionRepository
                        .findFirstByUsuarioAdminIdAndPlanActivoTrueOrderByFechaFinPlanDesc(1L))
                .thenReturn(Optional.of(institution));

        var response = suscripcionService.obtenerMiSuscripcionCompleta(1L);

        assertThat(response.getPlanCorporativoActivo()).isFalse();
    }

    @Test
    void obtenerMiSuscripcionCompletaShouldResolveInstitutionByLinkedUser() {
        when(suscripcionRepository.findByUsuarioIdAndActiva(1L, true)).thenReturn(Optional.empty());

        Usuario usuario = buildUsuario(1L);
        Institution institution =
                Institution.builder()
                        .id(200L)
                        .nombre("LinkedUni")
                        .planActivo(true)
                        .planCorporativo(
                                es.us.meerkat.backend.entity.subscriptions.TipoPlanCorporativo
                                        .ESTANDAR)
                        .fechaFinPlan(java.time.LocalDateTime.now().plusDays(30))
                        .build();
        usuario.setInstitution(institution);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(institutionRepository
                        .findFirstByUsuarioAdminIdAndPlanActivoTrueOrderByFechaFinPlanDesc(1L))
                .thenReturn(Optional.empty());

        var response = suscripcionService.obtenerMiSuscripcionCompleta(1L);

        assertThat(response.getInstitutionNombre()).isEqualTo("LinkedUni");
    }

    @Test
    void obtenerMiSuscripcionCompletaShouldResolveInstitutionByContactEmail() {
        when(suscripcionRepository.findByUsuarioIdAndActiva(1L, true)).thenReturn(Optional.empty());

        Usuario usuario = buildUsuario(1L);
        usuario.setInstitution(null);
        Institution institution =
                Institution.builder()
                        .id(300L)
                        .nombre("EmailUni")
                        .planActivo(true)
                        .planCorporativo(
                                es.us.meerkat.backend.entity.subscriptions.TipoPlanCorporativo
                                        .PREMIUM)
                        .fechaFinPlan(java.time.LocalDateTime.now().plusDays(30))
                        .build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(institutionRepository
                        .findFirstByUsuarioAdminIdAndPlanActivoTrueOrderByFechaFinPlanDesc(1L))
                .thenReturn(Optional.empty());
        when(institutionRepository
                        .findFirstByEmailContactoIgnoreCaseAndPlanActivoTrueOrderByFechaFinPlanDesc(
                                usuario.getEmail()))
                .thenReturn(Optional.of(institution));

        var response = suscripcionService.obtenerMiSuscripcionCompleta(1L);

        assertThat(response.getInstitutionNombre()).isEqualTo("EmailUni");
    }

    // ================================================================
    // tienePlanInstitucionalActivo
    // ================================================================

    @Test
    void tienePlanInstitucionalActivoShouldReturnFalseWhenNullUser() {
        assertThat(suscripcionService.tienePlanInstitucionalActivo(null)).isFalse();
    }

    @Test
    void tienePlanInstitucionalActivoShouldReturnTrueWhenActiveInstitution() {
        Usuario usuario = buildUsuario(1L);
        Institution institution =
                Institution.builder()
                        .id(100L)
                        .planActivo(true)
                        .fechaFinPlan(java.time.LocalDateTime.now().plusDays(30))
                        .build();

        when(institutionRepository
                        .findFirstByUsuarioAdminIdAndPlanActivoTrueOrderByFechaFinPlanDesc(1L))
                .thenReturn(Optional.of(institution));

        assertThat(suscripcionService.tienePlanInstitucionalActivo(usuario)).isTrue();
    }

    @Test
    void tienePlanInstitucionalActivoShouldReturnFalseWhenExpired() {
        Usuario usuario = buildUsuario(1L);
        Institution institution =
                Institution.builder()
                        .id(100L)
                        .planActivo(true)
                        .fechaFinPlan(java.time.LocalDateTime.now().minusDays(1))
                        .build();

        when(institutionRepository
                        .findFirstByUsuarioAdminIdAndPlanActivoTrueOrderByFechaFinPlanDesc(1L))
                .thenReturn(Optional.of(institution));

        assertThat(suscripcionService.tienePlanInstitucionalActivo(usuario)).isFalse();
    }

    @Test
    void tienePlanInstitucionalActivoShouldReturnTrueWhenNullEndDate() {
        Usuario usuario = buildUsuario(1L);
        Institution institution =
                Institution.builder().id(100L).planActivo(true).fechaFinPlan(null).build();

        when(institutionRepository
                        .findFirstByUsuarioAdminIdAndPlanActivoTrueOrderByFechaFinPlanDesc(1L))
                .thenReturn(Optional.of(institution));

        assertThat(suscripcionService.tienePlanInstitucionalActivo(usuario)).isTrue();
    }

    // ================================================================
    // activarSuscripcionTrasStripe – with TipoPlan parameter
    // ================================================================

    @Test
    void activarSuscripcionTrasStripeShouldDefaultToPremiumWhenFreeProvided() {
        Usuario usuario = buildUsuario(1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(institutionRepository
                        .findFirstByUsuarioAdminIdAndPlanActivoTrueOrderByFechaFinPlanDesc(1L))
                .thenReturn(Optional.empty());
        when(suscripcionRepository.findByUsuarioId(1L)).thenReturn(Optional.empty());
        when(suscripcionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        suscripcionService.activarSuscripcionTrasStripe(
                1L, BigDecimal.TEN, "MENSUAL", TipoPlan.FREE);

        ArgumentCaptor<Suscripcion> captor = ArgumentCaptor.forClass(Suscripcion.class);
        verify(suscripcionRepository).save(captor.capture());
        assertThat(captor.getValue().getPlan()).isEqualTo(TipoPlan.PREMIUM);
    }

    @Test
    void activarSuscripcionTrasStripeShouldThrowWhenInstitutionalPlanActive() {
        Usuario usuario = buildUsuario(1L);
        Institution inst =
                Institution.builder()
                        .id(1L)
                        .planActivo(true)
                        .fechaFinPlan(java.time.LocalDateTime.now().plusDays(30))
                        .build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(institutionRepository
                        .findFirstByUsuarioAdminIdAndPlanActivoTrueOrderByFechaFinPlanDesc(1L))
                .thenReturn(Optional.of(inst));

        assertThatThrownBy(
                        () ->
                                suscripcionService.activarSuscripcionTrasStripe(
                                        1L, BigDecimal.TEN, "MENSUAL", TipoPlan.PRO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("institucional");
    }

    @Test
    void suscribirseShouldThrowWhenInstitutionalPlanActive() {
        Usuario usuario = buildUsuario(1L);
        Institution inst =
                Institution.builder()
                        .id(1L)
                        .planActivo(true)
                        .fechaFinPlan(java.time.LocalDateTime.now().plusDays(30))
                        .build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(institutionRepository
                        .findFirstByUsuarioAdminIdAndPlanActivoTrueOrderByFechaFinPlanDesc(1L))
                .thenReturn(Optional.of(inst));

        assertThatThrownBy(() -> suscripcionService.suscribirse(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("institucional");
    }

    // ── renovarSuscripcionTrasStripe ──────────────────────────────────

    @Test
    void renovarSuscripcionTrasStripeShouldRenewAndSave() {
        Suscripcion suscripcion = buildSuscripcion(1L, 1L, true);
        Usuario usuario = buildUsuario(1L);

        when(suscripcionRepository.findByUsuarioId(1L)).thenReturn(Optional.of(suscripcion));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        suscripcionService.renovarSuscripcionTrasStripe(1L, BigDecimal.TEN, TipoPlan.PREMIUM);

        verify(suscripcionRepository).save(suscripcion);
        verify(usuarioRepository).save(usuario);
        assertThat(usuario.getPlan()).isEqualTo(TipoPlan.PREMIUM);
    }

    @Test
    void renovarSuscripcionTrasStripeShouldDefaultToPremiuWhenPlanNull() {
        Suscripcion suscripcion = buildSuscripcion(1L, 1L, true);
        Usuario usuario = buildUsuario(1L);

        when(suscripcionRepository.findByUsuarioId(1L)).thenReturn(Optional.of(suscripcion));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        suscripcionService.renovarSuscripcionTrasStripe(1L, BigDecimal.TEN, null);

        assertThat(usuario.getPlan()).isEqualTo(TipoPlan.PREMIUM);
    }

    @Test
    void renovarSuscripcionTrasStripeShouldDefaultToPremiuWhenPlanFree() {
        Suscripcion suscripcion = buildSuscripcion(1L, 1L, true);
        Usuario usuario = buildUsuario(1L);

        when(suscripcionRepository.findByUsuarioId(1L)).thenReturn(Optional.of(suscripcion));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        suscripcionService.renovarSuscripcionTrasStripe(1L, BigDecimal.TEN, TipoPlan.FREE);

        assertThat(usuario.getPlan()).isEqualTo(TipoPlan.PREMIUM);
    }

    @Test
    void renovarSuscripcionTrasStripeShouldThrowWhenNoSuscripcion() {
        when(suscripcionRepository.findByUsuarioId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                suscripcionService.renovarSuscripcionTrasStripe(
                                        99L, BigDecimal.TEN, TipoPlan.PREMIUM))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No se encontró suscripción");
    }

    @Test
    void renovarSuscripcionTrasStripeShouldThrowWhenUserNotFound() {
        Suscripcion suscripcion = buildSuscripcion(1L, 1L, true);

        when(suscripcionRepository.findByUsuarioId(1L)).thenReturn(Optional.of(suscripcion));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                suscripcionService.renovarSuscripcionTrasStripe(
                                        1L, BigDecimal.TEN, TipoPlan.PREMIUM))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Usuario no encontrado");
    }
}
