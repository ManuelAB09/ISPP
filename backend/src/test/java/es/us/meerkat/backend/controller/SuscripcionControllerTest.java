package es.us.meerkat.backend.controller;

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

import es.us.meerkat.backend.controller.suscriptions.SuscripcionController;
import es.us.meerkat.backend.dto.suscriptions.PaymentUrlResponse;
import es.us.meerkat.backend.dto.suscriptions.SubscribeRequest;
import es.us.meerkat.backend.dto.suscriptions.SubscriptionResponse;
import es.us.meerkat.backend.entity.suscriptions.Suscripcion;
import es.us.meerkat.backend.entity.suscriptions.TipoPlan;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.service.suscriptions.PaymentService;
import es.us.meerkat.backend.service.suscriptions.SuscripcionService;

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
    @DisplayName("DELETE /me - Debe cancelar la suscripción exitosamente")
    void testCancelarSuscripcion_Exito() {
        // Given
        Suscripcion suscripcionCancelada =
                Suscripcion.builder()
                        .id(1L)
                        .usuario(usuario)
                        .plan(TipoPlan.FREE)
                        .fechaInicio(null)
                        .fechaFin(null)
                        .activa(false)
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
        assertEquals(TipoPlan.FREE, response.getBody().getPlan());
        assertFalse(response.getBody().getActiva());
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
}
