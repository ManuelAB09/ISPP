package es.us.meerkat.backend.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import es.us.meerkat.backend.dto.PaymentUrlResponse;
import es.us.meerkat.backend.dto.SubscribeRequest;
import es.us.meerkat.backend.dto.SubscriptionResponse;
import es.us.meerkat.backend.entity.Suscripcion;
import es.us.meerkat.backend.entity.TipoPlan;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.service.PaymentService;
import es.us.meerkat.backend.service.SuscripcionService;

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
        assertEquals(2, response.getBody().length);
        assertEquals(TipoPlan.FREE, response.getBody()[0]);
        assertEquals(TipoPlan.PREMIUM, response.getBody()[1]);
        verify(suscripcionService).obtenerPlanesDisponibles();
    }

    @Test
    @DisplayName("GET /me - Debe devolver la suscripción del usuario autenticado")
    void testObtenerMiSuscripcion_RetornaSuscripcion() {
        // Given
        when(suscripcionService.obtenerMiSuscripcion(1L)).thenReturn(Optional.of(suscripcion));

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
        verify(suscripcionService).obtenerMiSuscripcion(1L);
    }

    @Test
    @DisplayName("GET /me - Debe devolver 404 si no hay suscripción activa")
    void testObtenerMiSuscripcion_NoEncontrada() {
        // Given
        when(suscripcionService.obtenerMiSuscripcion(1L)).thenReturn(Optional.empty());

        // When
        ResponseEntity<SubscriptionResponse> response =
                suscripcionController.obtenerMiSuscripcion(usuario);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(TipoPlan.FREE, response.getBody().getPlan());
        assertTrue(response.getBody().getActiva());
        verify(suscripcionService).obtenerMiSuscripcion(1L);
    }

    @Test
    @DisplayName("POST /me - Debe suscribir al usuario exitosamente")
    void testSuscribirse_Exito() throws Exception {
        // Given
        SubscribeRequest request =
                SubscribeRequest.builder().planId("PREMIUM").aceptarTerminos(true).build();
        PaymentUrlResponse paymentUrlResponse =
                new PaymentUrlResponse("https://pay.test", "sess_123");
        when(suscripcionService.obtenerMiSuscripcion(1L)).thenReturn(Optional.empty());
        when(paymentService.generarPagoSuscripcion(usuario, TipoPlan.PREMIUM))
                .thenReturn(paymentUrlResponse);

        // When
        ResponseEntity<?> response = suscripcionController.suscribirse(usuario, request);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(paymentUrlResponse, response.getBody());
        verify(suscripcionService).obtenerMiSuscripcion(1L);
        verify(paymentService).generarPagoSuscripcion(usuario, TipoPlan.PREMIUM);
    }

    @Test
    @DisplayName("POST /me - Debe devolver 400 si el usuario ya tiene suscripción")
    void testSuscribirse_YaTieneSuscripcion() {
        // Given
        SubscribeRequest request =
                SubscribeRequest.builder().planId("PREMIUM").aceptarTerminos(true).build();
        when(suscripcionService.obtenerMiSuscripcion(1L)).thenReturn(Optional.of(suscripcion));

        // When
        ResponseEntity<?> response = suscripcionController.suscribirse(usuario, request);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertEquals("Ya tienes una suscripción activa", body.get("error"));
        verify(suscripcionService).obtenerMiSuscripcion(1L);
        verifyNoInteractions(paymentService);
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
