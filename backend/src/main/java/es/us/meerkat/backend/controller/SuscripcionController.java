package es.us.meerkat.backend.controller;

import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import es.us.meerkat.backend.dto.PaymentUrlResponse;
import es.us.meerkat.backend.dto.SubscribeRequest;
import es.us.meerkat.backend.dto.SubscriptionResponse;
import es.us.meerkat.backend.entity.Suscripcion;
import es.us.meerkat.backend.entity.TipoPlan;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.service.PaymentService;
import es.us.meerkat.backend.service.SuscripcionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Controlador REST para gestionar suscripciones de usuarios. */
@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
@Tag(name = "suscripciones-controller", description = "Gestión de suscripciones a planes")
public class SuscripcionController {

    private final SuscripcionService suscripcionService;
    private final PaymentService paymentService;

    /**
     * Obtiene todos los planes de suscripción disponibles.
     *
     * @return Tipos de planes disponibles
     */
    @GetMapping("/plans")
    @Operation(
            summary = "Ver planes disponibles",
            description = " lista de planes de suscripción disponibles")
    public ResponseEntity<TipoPlan[]> obtenerPlanes() {
        TipoPlan[] planes = suscripcionService.obtenerPlanesDisponibles();
        return ResponseEntity.ok(planes);
    }

    /**
     * Obtiene la suscripción actual del usuario autenticado.
     *
     * @return Suscripción del usuario o plan FREE por defecto
     */
    @GetMapping("/me")
    @Operation(summary = "Obtener mi suscripción", description = "Devuelve la suscripción actual")
    public ResponseEntity<SubscriptionResponse> obtenerMiSuscripcion(
            @AuthenticationPrincipal final Usuario usuario) {
        Optional<Suscripcion> suscripcion =
                suscripcionService.obtenerMiSuscripcion(usuario.getId());

        if (suscripcion.isPresent()) {
            return ResponseEntity.ok(suscripcion.get().toDTO());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Inicia el proceso de suscripción a un plan Premium.
     *
     * @param usuario Usuario autenticado
     * @param request Datos de subscripción
     * @return URL de pago para completar la suscripción
     */
    @PostMapping("/me")
    @Operation(
            summary = "Suscribirse a plan Premium",
            description = "Inicia el proceso de suscripción")
    public ResponseEntity<?> suscribirse(
            @AuthenticationPrincipal final Usuario usuario,
            @Valid @RequestBody SubscribeRequest request) {
        try {
            Optional<Suscripcion> suscripcionActiva =
                    suscripcionService.obtenerMiSuscripcion(usuario.getId());
            if (suscripcionActiva.isPresent()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Ya tienes una suscripción activa"));
            }

            PaymentUrlResponse paymentUrl =
                    paymentService.generarPagoSuscripcion(usuario, TipoPlan.PREMIUM);
            return ResponseEntity.status(HttpStatus.CREATED).body(paymentUrl);

        } catch (com.stripe.exception.StripeException e) {

            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error al conectar con la pasarela de pago"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Simula la completación del pago de suscripción. En producción, Stripe webhook haría esto.
     *
     * @param usuario Usuario autenticado
     * @return Suscripción creada
     */
    @PostMapping("/me/confirm-payment")
    @Operation(
            summary = "Confirmar pago de suscripción",
            description = "confirma el pago de la suscripción ")
    public ResponseEntity<SubscriptionResponse> confirmarPagoSuscripcion(
            @AuthenticationPrincipal final Usuario usuario) {
        try {
            Suscripcion suscripcion = suscripcionService.suscribirse(usuario.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(suscripcion.toDTO());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Cancela la suscripción Premium del usuario autenticado.
     *
     * @return Confirmación de cancelación
     */
    @DeleteMapping("/me")
    @Operation(
            summary = "Cancelar suscripción",
            description =
                    "Cancela la suscripción del usuario.\r\n"
                            + //
                            "        El acceso Premium se mantiene hasta la fecha de fin del"
                            + " período actual.")
    public ResponseEntity<SubscriptionResponse> cancelarSuscripcion(
            @AuthenticationPrincipal final Usuario usuario) {
        try {
            Suscripcion cancelada = suscripcionService.cancelarSuscripcion(usuario.getId());
            return ResponseEntity.ok(cancelada.toDTO());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
