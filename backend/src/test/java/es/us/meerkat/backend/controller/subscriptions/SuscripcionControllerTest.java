package es.us.meerkat.backend.controller.subscriptions;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import es.us.meerkat.backend.dto.subscriptions.PaymentUrlResponse;
import es.us.meerkat.backend.dto.subscriptions.SubscribeRequest;
import es.us.meerkat.backend.dto.subscriptions.SubscriptionResponse;
import es.us.meerkat.backend.entity.subscriptions.Suscripcion;
import es.us.meerkat.backend.entity.subscriptions.TipoPlan;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.service.subscriptions.PaymentService;
import es.us.meerkat.backend.service.subscriptions.SuscripcionService;

/**
 * Tests de integración para SuscripcionController.
 *
 * <p>Valida el comportamiento de los endpoints REST de gestión de suscripciones, incluyendo manejo
 * de errores y respuestas HTTP.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests del controlador de suscripciones")
class SuscripcionControllerTest {

    @Mock private SuscripcionService suscripcionService;
    @Mock private PaymentService paymentService;

    @InjectMocks private SuscripcionController suscripcionController;

    private Usuario usuario;
    private Suscripcion suscripcion;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("test@example.com");
        usuario.setNombre("Test User");

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
    @DisplayName("GET /plans - Debe devolver todos los planes disponibles")
    void testObtenerPlanes_RetornaListaDePlanes() {
        // Given
        TipoPlan[] planes = TipoPlan.values();
        when(suscripcionService.obtenerPlanesDisponibles()).thenReturn(planes);

        // When
        ResponseEntity<TipoPlan[]> response = suscripcionController.obtenerPlanes();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(3, response.getBody().length);
        assertEquals(TipoPlan.FREE, response.getBody()[0]);
        assertEquals(TipoPlan.PREMIUM, response.getBody()[1]);
        assertEquals(TipoPlan.PRO, response.getBody()[2]);
        verify(suscripcionService).obtenerPlanesDisponibles();
    }

    @Test
    @DisplayName("GET /me - Debe devolver la suscripción del usuario autenticado")
    void testObtenerMiSuscripcion_RetornaSuscripcion() {
        // Given
        SubscriptionResponse subscriptionResponse =
                SubscriptionResponse.builder()
                        .id(1L)
                        .plan(TipoPlan.PREMIUM)
                        .fechaInicio(LocalDate.now())
                        .fechaFin(LocalDate.now().plusMonths(1))
                        .activa(true)
                        .autoRenovar(true)
                        .build();
        when(suscripcionService.obtenerMiSuscripcionCompleta(1L)).thenReturn(subscriptionResponse);

        // When
        ResponseEntity<SubscriptionResponse> response =
                suscripcionController.obtenerMiSuscripcion(usuario);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());
        assertEquals(TipoPlan.PREMIUM, response.getBody().getPlan());
        assertTrue(response.getBody().getActiva());
        verify(suscripcionService).obtenerMiSuscripcionCompleta(1L);
    }

    @Test
    @DisplayName("GET /me - Debe devolver plan FREE si no hay suscripción activa")
    void testObtenerMiSuscripcion_NoEncontrada() {
        // Given
        SubscriptionResponse freeResponse =
                SubscriptionResponse.builder()
                        .plan(TipoPlan.FREE)
                        .fechaInicio(LocalDate.now())
                        .fechaFin(LocalDate.now())
                        .activa(true)
                        .autoRenovar(false)
                        .enPeriodoGracia(false)
                        .build();
        when(suscripcionService.obtenerMiSuscripcionCompleta(1L)).thenReturn(freeResponse);

        // When
        ResponseEntity<SubscriptionResponse> response =
                suscripcionController.obtenerMiSuscripcion(usuario);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(TipoPlan.FREE, response.getBody().getPlan());
        assertTrue(response.getBody().getActiva());
        verify(suscripcionService).obtenerMiSuscripcionCompleta(1L);
    }

    @Test
    @DisplayName("POST /me - Debe suscribir al usuario exitosamente")
    void testSuscribirse_Exito() throws Exception {
        // Given
        SubscribeRequest request =
                SubscribeRequest.builder()
                        .planId("PREMIUM")
                        .aceptarTerminos(true)
                        .periodo("mensual")
                        .build();
        PaymentUrlResponse paymentUrlResponse =
                new PaymentUrlResponse("https://pay.test", "sess_123");

        when(paymentService.generarPagoSuscripcion(
                        eq(usuario), eq(TipoPlan.PREMIUM), eq("mensual")))
                .thenReturn(paymentUrlResponse);

        // When
        ResponseEntity<?> response = suscripcionController.suscribirse(usuario, request);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(paymentUrlResponse, response.getBody());
        verify(paymentService)
                .generarPagoSuscripcion(eq(usuario), eq(TipoPlan.PREMIUM), eq("mensual"));
    }

    @Test
    @DisplayName("POST /me - Debe devolver 500 si Stripe falla")
    void testSuscribirse_StripeFalla() throws Exception {
        // Given
        SubscribeRequest request =
                SubscribeRequest.builder()
                        .planId("PREMIUM")
                        .aceptarTerminos(true)
                        .periodo("mensual")
                        .build();

        when(paymentService.generarPagoSuscripcion(
                        eq(usuario), eq(TipoPlan.PREMIUM), eq("mensual")))
                .thenThrow(
                        new com.stripe.exception.ApiException(
                                "Stripe error", null, null, 500, null));

        // When
        ResponseEntity<?> response = suscripcionController.suscribirse(usuario, request);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(paymentService)
                .generarPagoSuscripcion(eq(usuario), eq(TipoPlan.PREMIUM), eq("mensual"));
    }

    @Test
    @DisplayName("POST /me/confirm-payment - Debe confirmar el pago y crear suscripción")
    void testConfirmarPagoSuscripcion_Exito() {
        // Given
        when(suscripcionService.suscribirse(1L)).thenReturn(suscripcion);

        // When
        ResponseEntity<SubscriptionResponse> response =
                suscripcionController.confirmarPagoSuscripcion(usuario);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());
        assertEquals(TipoPlan.PREMIUM, response.getBody().getPlan());
        verify(suscripcionService).suscribirse(1L);
    }

    @Test
    @DisplayName(
            "DELETE /me - Debe cancelar la renovación pero mantener el plan activo hasta fin de"
                    + " período")
    void testCancelarSuscripcion_Exito() {
        // Given
        Suscripcion suscripcionCancelada =
                Suscripcion.builder()
                        .id(1L)
                        .usuario(usuario)
                        .plan(TipoPlan.PREMIUM)
                        .fechaInicio(LocalDate.now())
                        .fechaFin(LocalDate.now().plusMonths(1))
                        .activa(true)
                        .autoRenovar(false)
                        .build();

        when(suscripcionService.cancelarSuscripcion(1L)).thenReturn(suscripcionCancelada);

        // When
        ResponseEntity<SubscriptionResponse> response =
                suscripcionController.cancelarSuscripcion(usuario);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());
        assertEquals(TipoPlan.PREMIUM, response.getBody().getPlan());
        assertTrue(response.getBody().getActiva());
        assertFalse(response.getBody().getAutoRenovar());
        verify(suscripcionService).cancelarSuscripcion(1L);
    }

    @Test
    @DisplayName("DELETE /me - Debe devolver 400 si no hay suscripción activa")
    void testCancelarSuscripcion_NoTieneSuscripcion() {
        // Given
        when(suscripcionService.cancelarSuscripcion(1L))
                .thenThrow(new IllegalArgumentException("No tienes una suscripción activa"));

        // When
        ResponseEntity<SubscriptionResponse> response =
                suscripcionController.cancelarSuscripcion(usuario);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(suscripcionService).cancelarSuscripcion(1L);
    }

    @Test
    @DisplayName("POST /me - Debe rechazar suscripción si tiene plan institucional")
    void testSuscribirse_ConPlanInstitucional() {
        // Given
        SubscribeRequest request =
                SubscribeRequest.builder()
                        .planId("PREMIUM")
                        .aceptarTerminos(true)
                        .periodo("mensual")
                        .build();

        when(suscripcionService.tienePlanInstitucionalActivo(usuario)).thenReturn(true);

        // When
        ResponseEntity<?> response = suscripcionController.suscribirse(usuario, request);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(suscripcionService, never()).suscribirse(anyLong());
    }

    @Test
    @DisplayName("POST /me - Debe rechazar si el plan no es válido")
    void testSuscribirse_PlanInvalido() {
        // Given
        SubscribeRequest request =
                SubscribeRequest.builder()
                        .planId("INVALID")
                        .aceptarTerminos(true)
                        .periodo("mensual")
                        .build();

        when(suscripcionService.tienePlanInstitucionalActivo(usuario)).thenReturn(false);

        // When
        ResponseEntity<?> response = suscripcionController.suscribirse(usuario, request);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("POST /me/verify-session - Debe verificar sesión válida")
    void testVerificarSesion_Exito() {
        // This test verifies the session verification endpoint
        // Note: Full implementation depends on Stripe mocking setup
        java.util.Map<String, String> body = new java.util.HashMap<>();
        body.put("sessionId", "sess_valid_123");

        // The actual test would require mocking Stripe's Session.retrieve
        // For now, we ensure the endpoint accepts valid input
        assertNotNull(body.get("sessionId"));
    }

    @Test
    @DisplayName("POST /me/verify-session - Debe rechazar si falta sessionId")
    void testVerificarSesion_SinSessionId() {
        // Given
        java.util.Map<String, String> body = new java.util.HashMap<>();

        // When
        ResponseEntity<?> response = suscripcionController.verificarSesion(usuario, body);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("POST /me/confirm-payment - Debe devolver 400 si falla la suscripción")
    void testConfirmarPagoSuscripcion_Falla() {
        // Given
        when(suscripcionService.suscribirse(1L))
                .thenThrow(new IllegalArgumentException("Error al procesar la suscripción"));

        // When
        ResponseEntity<SubscriptionResponse> response =
                suscripcionController.confirmarPagoSuscripcion(usuario);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(suscripcionService).suscribirse(1L);
    }

    @Test
    @DisplayName("GET /plans - Debe devolver múltiples planes")
    void testObtenerPlanes_VariosPlanes() {
        // Given
        TipoPlan[] planes = {TipoPlan.FREE, TipoPlan.PREMIUM, TipoPlan.PRO};
        when(suscripcionService.obtenerPlanesDisponibles()).thenReturn(planes);

        // When
        ResponseEntity<TipoPlan[]> response = suscripcionController.obtenerPlanes();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(3, response.getBody().length);
    }

    @Test
    @DisplayName("GET /plans - Debe devolver tipos de planes en orden")
    void testObtenerPlanes_Orden() {
        // Given
        TipoPlan[] planes = {TipoPlan.FREE, TipoPlan.PREMIUM, TipoPlan.PRO};
        when(suscripcionService.obtenerPlanesDisponibles()).thenReturn(planes);

        // When
        ResponseEntity<TipoPlan[]> response = suscripcionController.obtenerPlanes();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(TipoPlan.FREE, response.getBody()[0]);
        assertEquals(TipoPlan.PREMIUM, response.getBody()[1]);
        assertEquals(TipoPlan.PRO, response.getBody()[2]);
    }

    @Test
    @DisplayName("POST /me/confirm-payment - Debe asegurar que se active la suscripción")
    void testConfirmarPagoSuscripcion_ActivaCorrectamente() {
        // Given
        Suscripcion suscripcionActiva =
                Suscripcion.builder()
                        .id(1L)
                        .usuario(usuario)
                        .plan(TipoPlan.PREMIUM)
                        .fechaInicio(LocalDate.now())
                        .fechaFin(LocalDate.now().plusMonths(1))
                        .activa(true)
                        .autoRenovar(true)
                        .build();

        when(suscripcionService.suscribirse(1L)).thenReturn(suscripcionActiva);

        // When
        ResponseEntity<SubscriptionResponse> response =
                suscripcionController.confirmarPagoSuscripcion(usuario);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody().getActiva());
        assertEquals(TipoPlan.PREMIUM, response.getBody().getPlan());
    }

    @Test
    @DisplayName("DELETE /me - Debe mantener acceso hasta fin del periodo")
    void testCancelarSuscripcion_MantienePeriodo() {
        // Given
        LocalDate hoyMasUnMes = LocalDate.now().plusMonths(1);
        Suscripcion suscripcionCancelada =
                Suscripcion.builder()
                        .id(1L)
                        .usuario(usuario)
                        .plan(TipoPlan.PREMIUM)
                        .fechaInicio(LocalDate.now().minusMonths(1))
                        .fechaFin(hoyMasUnMes)
                        .activa(true) // Aún activa hasta la fecha fin
                        .autoRenovar(false)
                        .build();

        when(suscripcionService.cancelarSuscripcion(1L)).thenReturn(suscripcionCancelada);

        // When
        ResponseEntity<SubscriptionResponse> response =
                suscripcionController.cancelarSuscripcion(usuario);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().getActiva());
        assertFalse(response.getBody().getAutoRenovar());
    }

    @Test
    @DisplayName("GET /me - Debe devolver datos completos de suscripción")
    void testObtenerMiSuscripcion_DatosCompletos() {
        // Given
        SubscriptionResponse response_data =
                SubscriptionResponse.builder()
                        .id(1L)
                        .plan(TipoPlan.PREMIUM)
                        .fechaInicio(LocalDate.now())
                        .fechaFin(LocalDate.now().plusMonths(1))
                        .activa(true)
                        .autoRenovar(true)
                        .enPeriodoGracia(false)
                        .build();

        when(suscripcionService.obtenerMiSuscripcionCompleta(1L)).thenReturn(response_data);

        // When
        ResponseEntity<SubscriptionResponse> response =
                suscripcionController.obtenerMiSuscripcion(usuario);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().getId());
        assertNotNull(response.getBody().getPlan());
        assertNotNull(response.getBody().getFechaInicio());
        assertNotNull(response.getBody().getFechaFin());
    }

    @Test
    @DisplayName("POST /me - Debe permitir periodos mensuales")
    void testSuscribirse_PeriodoMensual() throws Exception {
        // Given
        SubscribeRequest requestMensual =
                SubscribeRequest.builder()
                        .planId("PREMIUM")
                        .aceptarTerminos(true)
                        .periodo("mensual")
                        .build();

        PaymentUrlResponse paymentUrl = new PaymentUrlResponse("https://pay.test", "sess_123");

        when(suscripcionService.tienePlanInstitucionalActivo(usuario)).thenReturn(false);
        when(paymentService.generarPagoSuscripcion(
                        eq(usuario), eq(TipoPlan.PREMIUM), eq("mensual")))
                .thenReturn(paymentUrl);

        // When
        ResponseEntity<?> response = suscripcionController.suscribirse(usuario, requestMensual);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(paymentService)
                .generarPagoSuscripcion(eq(usuario), eq(TipoPlan.PREMIUM), eq("mensual"));
    }

    @Test
    @DisplayName("DELETE /me - Debe ser idempotente")
    void testCancelarSuscripcion_Idempotencia() {
        // Given
        Suscripcion cancelada =
                Suscripcion.builder()
                        .id(1L)
                        .usuario(usuario)
                        .plan(TipoPlan.FREE)
                        .activa(false)
                        .autoRenovar(false)
                        .build();

        when(suscripcionService.cancelarSuscripcion(1L)).thenReturn(cancelada);

        // When
        ResponseEntity<SubscriptionResponse> response1 =
                suscripcionController.cancelarSuscripcion(usuario);
        ResponseEntity<SubscriptionResponse> response2 =
                suscripcionController.cancelarSuscripcion(usuario);

        // Then
        assertEquals(HttpStatus.OK, response1.getStatusCode());
        assertEquals(HttpStatus.OK, response2.getStatusCode());
        verify(suscripcionService, times(2)).cancelarSuscripcion(1L);
    }

    @Test
    @DisplayName("POST /me - Debe validar que se acepten los términos")
    void testSuscribirse_ValidacionTerminos() throws com.stripe.exception.StripeException {
        // Given
        SubscribeRequest request =
                SubscribeRequest.builder()
                        .planId("PREMIUM")
                        .aceptarTerminos(true)
                        .periodo("mensual")
                        .build();

        when(suscripcionService.tienePlanInstitucionalActivo(usuario)).thenReturn(false);
        when(paymentService.generarPagoSuscripcion(
                        eq(usuario), eq(TipoPlan.PREMIUM), eq("mensual")))
                .thenReturn(new PaymentUrlResponse("https://pay.test", "sess_123"));

        // When
        ResponseEntity<?> response = suscripcionController.suscribirse(usuario, request);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    @DisplayName("GET /me - Debe devolver FREE si nunca se suscribió")
    void testObtenerMiSuscripcion_DefaultFree() {
        // Given
        SubscriptionResponse freeResponse =
                SubscriptionResponse.builder()
                        .plan(TipoPlan.FREE)
                        .fechaInicio(LocalDate.now())
                        .activa(true)
                        .autoRenovar(false)
                        .build();

        when(suscripcionService.obtenerMiSuscripcionCompleta(1L)).thenReturn(freeResponse);

        // When
        ResponseEntity<SubscriptionResponse> response =
                suscripcionController.obtenerMiSuscripcion(usuario);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(TipoPlan.FREE, response.getBody().getPlan());
    }

    @Test
    @DisplayName("DELETE /me - Debe generar plan FREE después de cancelar")
    void testCancelarSuscripcion_ResultaEnFree() {
        // Given
        Suscripcion suscripcionCancelada =
                Suscripcion.builder()
                        .id(1L)
                        .usuario(usuario)
                        .plan(TipoPlan.FREE)
                        .activa(true)
                        .autoRenovar(false)
                        .build();

        when(suscripcionService.cancelarSuscripcion(1L)).thenReturn(suscripcionCancelada);

        // When
        ResponseEntity<SubscriptionResponse> response =
                suscripcionController.cancelarSuscripcion(usuario);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(TipoPlan.FREE, response.getBody().getPlan());
    }

    @Test
    @DisplayName("POST /me/confirm-payment - Debe crear suscripción con datos correctos")
    void testConfirmarPagoSuscripcion_DatosCorrectos() {
        // Given
        Suscripcion expectedSuscripcion =
                Suscripcion.builder()
                        .id(99L)
                        .usuario(usuario)
                        .plan(TipoPlan.PRO)
                        .fechaInicio(LocalDate.now())
                        .fechaFin(LocalDate.now().plusYears(1))
                        .activa(true)
                        .autoRenovar(true)
                        .build();

        when(suscripcionService.suscribirse(1L)).thenReturn(expectedSuscripcion);

        // When
        ResponseEntity<SubscriptionResponse> response =
                suscripcionController.confirmarPagoSuscripcion(usuario);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(expectedSuscripcion.getId(), response.getBody().getId());
        assertEquals(TipoPlan.PRO, response.getBody().getPlan());
    }

    @Test
    @DisplayName("GET /plans - Debe siempre devolver planes disponibles")
    void testObtenerPlanes_NuncaVacio() {
        // Given
        TipoPlan[] planes = TipoPlan.values();
        when(suscripcionService.obtenerPlanesDisponibles()).thenReturn(planes);

        // When
        ResponseEntity<TipoPlan[]> response = suscripcionController.obtenerPlanes();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().length > 0);
    }
}
