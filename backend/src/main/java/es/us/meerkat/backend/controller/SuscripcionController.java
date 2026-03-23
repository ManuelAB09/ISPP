package es.us.meerkat.backend.controller;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;

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
import lombok.extern.slf4j.Slf4j;

@Slf4j // ← añadir esto
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
        SubscriptionResponse response =
                suscripcionService.obtenerMiSuscripcionCompleta(usuario.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * Inicia el proceso de suscripción a un plan Premium.
     *
     * @param usuario Usuario autenticado
     * @param request Datos de subscripción
     * @return URL de pago para completar la suscripción
     */
    @PostMapping("/me")
    public ResponseEntity<?> suscribirse(
            @AuthenticationPrincipal final Usuario usuario,
            @Valid @RequestBody SubscribeRequest request) {

        // Verificar si tiene plan institucional activo
        if (suscripcionService.tienePlanInstitucionalActivo(usuario)) {
            return ResponseEntity.badRequest()
                    .body(
                            Map.of(
                                    "error",
                                    "No puedes suscribirte a un plan individual mientras tengas un"
                                            + " plan institucional activo"));
        }

        try {
                        TipoPlan planSolicitado = resolvePlan(request.getPlanId());
            PaymentUrlResponse paymentUrl =
                    paymentService.generarPagoSuscripcion(
                            usuario, planSolicitado, request.getPeriodo());
            return ResponseEntity.status(HttpStatus.CREATED).body(paymentUrl);
                } catch (IllegalArgumentException e) {
                        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (StripeException e) {

            return ResponseEntity.internalServerError()
                    .body(
                            Map.of(
                                    "error",
                                    "Error al conectar con la pasarela de pago",
                                    "detalle",
                                    e.getMessage()));
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

    @PostMapping("/me/verify-session")
    public ResponseEntity<?> verificarSesion(
            @AuthenticationPrincipal final Usuario usuario, @RequestBody Map<String, String> body) {

        String sessionId = body.get("sessionId");
        log.info(
                "=== VERIFY SESSION START === sessionId: {}, usuarioId: {}",
                sessionId,
                usuario.getId());

        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "sessionId requerido"));
        }

        try {
            Session session = Session.retrieve(sessionId);
            log.info(
                    "Session recuperada - status: {}, amountTotal: {}, metadata: {}",
                    session.getStatus(),
                    session.getAmountTotal(),
                    session.getMetadata());

            if (!"complete".equals(session.getStatus())) {
                log.warn("Session no completada, status: {}", session.getStatus());
                return ResponseEntity.badRequest()
                        .body(
                                Map.of(
                                        "error",
                                        "El pago no esta completado: " + session.getStatus()));
            }

            String usuarioIdEnSession = session.getMetadata().get("usuarioId");
            log.info(
                    "usuarioId en metadata: {} vs actual: {}", usuarioIdEnSession, usuario.getId());

            if (!usuario.getId().toString().equals(usuarioIdEnSession)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Sesion no valida para este usuario"));
            }

            BigDecimal monto =
                    session.getAmountTotal() != null
                            ? BigDecimal.valueOf(session.getAmountTotal())
                                    .divide(BigDecimal.valueOf(100))
                            : BigDecimal.valueOf(2.99);

            TipoPlan planSolicitado =
                    resolvePlan(session.getMetadata().getOrDefault("plan", TipoPlan.PREMIUM.name()));
            String periodo = session.getMetadata().getOrDefault("periodo", "mensual");

            log.info("Llamando activarSuscripcionTrasStripe con monto: {}", monto);
            suscripcionService.activarSuscripcionTrasStripe(
                    usuario.getId(), monto, periodo, planSolicitado);
            log.info("=== SUSCRIPCION ACTIVADA CORRECTAMENTE ===");

            return ResponseEntity.ok(Map.of("mensaje", "Suscripcion activada correctamente"));

        } catch (StripeException e) {
            log.error("StripeException: code={}, message={}", e.getCode(), e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error Stripe: " + e.getMessage()));

        } catch (Exception e) {
            log.error(
                    "ERROR INESPERADO: clase={}, mensaje={}",
                    e.getClass().getSimpleName(),
                    e.getMessage(),
                    e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getClass().getSimpleName() + ": " + e.getMessage()));
        }
    }

    /** Confirma la suscripcion tras pago exitoso con Stripe Elements (PaymentIntent). */
    @PostMapping("/me/confirm-embedded-payment")
    @Operation(
            summary = "Confirmar pago embebido con Stripe Elements",
            description = "Verifica el PaymentIntent y activa la suscripcion Premium")
    public ResponseEntity<?> confirmarPagoEmbebido(
            @AuthenticationPrincipal final Usuario usuario, @RequestBody Map<String, String> body) {

        String paymentIntentId = body.get("paymentIntentId");
        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "paymentIntentId requerido"));
        }

        try {
            com.stripe.model.PaymentIntent intent =
                    com.stripe.model.PaymentIntent.retrieve(paymentIntentId);

            if (!"succeeded".equals(intent.getStatus())) {
                return ResponseEntity.badRequest()
                        .body(
                                Map.of(
                                        "error",
                                        "El pago no se ha completado: " + intent.getStatus()));
            }

            String usuarioIdMeta = intent.getMetadata().get("usuarioId");
            if (!usuario.getId().toString().equals(usuarioIdMeta)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "PaymentIntent no valido para este usuario"));
            }

            BigDecimal monto =
                    BigDecimal.valueOf(intent.getAmount()).divide(BigDecimal.valueOf(100));
            String periodo = intent.getMetadata().get("periodo");
            TipoPlan planSolicitado = resolvePlan(intent.getMetadata().get("plan"));
            suscripcionService.activarSuscripcionTrasStripe(
                    usuario.getId(), monto, periodo, planSolicitado);

            return ResponseEntity.ok(Map.of("mensaje", "Suscripcion activada correctamente"));

        } catch (StripeException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error Stripe: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/me/create-payment-intent")
    @Operation(
            summary = "Crear PaymentIntent para suscripcion",
            description = "Devuelve el clientSecret para usar con Stripe Elements")
    public ResponseEntity<?> crearPaymentIntent(
            @AuthenticationPrincipal final Usuario usuario,
            @Valid @RequestBody SubscribeRequest request) {

        // Verificar si tiene plan institucional activo
        if (suscripcionService.tienePlanInstitucionalActivo(usuario)) {
            return ResponseEntity.badRequest()
                    .body(
                            Map.of(
                                    "error",
                                    "No puedes suscribirte a un plan individual mientras tengas un"
                                            + " plan institucional activo"));
        }

        try {
                        TipoPlan planSolicitado = resolvePlan(request.getPlanId());
            Map<String, String> result =
                                        paymentService.crearPaymentIntentSuscripcion(
                                                        usuario, planSolicitado, request.getPeriodo());
            return ResponseEntity.ok(result);
                } catch (IllegalArgumentException e) {
                        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (StripeException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error al crear el intent de pago: " + e.getMessage()));
        }
    }

        private TipoPlan resolvePlan(String planId) {
                String normalized = (planId == null || planId.isBlank()) ? "PREMIUM" : planId.trim().toUpperCase();
                try {
                        TipoPlan plan = TipoPlan.valueOf(normalized);
                        if (plan == TipoPlan.FREE) {
                                throw new IllegalArgumentException("El plan FREE no es contratable");
                        }
                        return plan;
                } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Plan no válido: " + planId + ". Usa PREMIUM o PRO");
                }
        }
}
