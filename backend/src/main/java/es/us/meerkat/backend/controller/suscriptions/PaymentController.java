package es.us.meerkat.backend.controller.suscriptions;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Invoice;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;

import es.us.meerkat.backend.dto.suscriptions.TransactionListResponse;
import es.us.meerkat.backend.dto.suscriptions.TransactionResponse;
import es.us.meerkat.backend.dto.users.PageInfo;
import es.us.meerkat.backend.entity.TipoTransaccion;
import es.us.meerkat.backend.entity.TransaccionPago;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.service.communities.InstitutionService;
import es.us.meerkat.backend.service.suscriptions.PaymentService;
import es.us.meerkat.backend.service.suscriptions.SuscripcionService;
import es.us.meerkat.backend.service.tutors.TutorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Pagos", description = "Gestión de pagos y transacciones")
public class PaymentController {

    private final PaymentService paymentService;
    private final SuscripcionService suscripcionService;
    private final InstitutionService institutionService;
    private final TutorService tutorService;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    // -------------------------------------------------------------------------
    // Endpoints existentes sin cambios
    // -------------------------------------------------------------------------

    @GetMapping("/history")
    @Operation(summary = "Historial de pagos", description = "Devuelve el historial")
    public ResponseEntity<TransactionListResponse> getPaymentHistory(
            @AuthenticationPrincipal final Usuario usuario,
            @Parameter(description = "Filtrar por tipo") @RequestParam(required = false)
                    String tipo,
            @Parameter(description = "Fecha desde") @RequestParam(required = false) LocalDate desde,
            @Parameter(description = "Fecha hasta") @RequestParam(required = false) LocalDate hasta,
            @Parameter(description = "Número de página") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Elementos por página") @RequestParam(defaultValue = "20")
                    int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<TransaccionPago> payments =
                paymentService.obtenerHistorialPagos(usuario.getId(), pageable);

        var content =
                payments.getContent().stream()
                        .map(this::toTransactionResponse)
                        .collect(Collectors.toList());

        var pageInfo =
                PageInfo.builder()
                        .number(page)
                        .size(size)
                        .totalElements(payments.getTotalElements())
                        .totalPages(payments.getTotalPages())
                        .first(payments.isFirst())
                        .last(payments.isLast())
                        .build();

        return ResponseEntity.ok(
                TransactionListResponse.builder().content(content).page(pageInfo).build());
    }

    @GetMapping("/{transactionId}")
    @Operation(summary = "Obtener detalle de transacción", description = "Devuelve el detalle")
    public ResponseEntity<TransactionResponse> getTransaction(
            @Parameter(description = "ID de la transacción") @PathVariable Long transactionId,
            @AuthenticationPrincipal final Usuario usuario) {

        return paymentService
                .obtenerTransaccion(transactionId, usuario.getId())
                .map(t -> ResponseEntity.ok(toTransactionResponse(t)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // -------------------------------------------------------------------------
    // Webhook real de Stripe
    // -------------------------------------------------------------------------

    /**
     * Recibe y procesa eventos del webhook de Stripe.
     *
     * <p>Eventos manejados: - checkout.session.completed → activa suscripción PREMIUM o registra
     * pago único - checkout.session.expired → sesión expirada sin pagar - invoice.payment_succeeded
     * → renueva suscripción PREMIUM (cobro recurrente exitoso) - invoice.payment_failed → registra
     * fallo de cobro recurrente
     *
     * <p>IMPORTANTE: añadir en SecurityConfig: .csrf(csrf ->
     * csrf.ignoringRequestMatchers("/api/v1/payments/webhook")) .authorizeHttpRequests(auth ->
     * auth.requestMatchers("/api/v1/payments/webhook").permitAll())
     */
    @PostMapping("/webhook")
    @Operation(
            summary = "Webhook de Stripe",
            description = "Recibe notificaciones de eventos de Stripe")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {

        // 1. Verificar firma — si falla, Stripe reintentará
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Webhook con firma inválida: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Firma inválida");
        }

        log.info("Webhook Stripe recibido: tipo={}, id={}", event.getType(), event.getId());

        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();

        switch (event.getType()) {

                // Pago completado: puede ser suscripción o pago único
            case "checkout.session.completed" -> {
                deserializer
                        .getObject()
                        .ifPresentOrElse(
                                obj -> procesarSessionCompletada((Session) obj),
                                () ->
                                        log.warn(
                                                "No se pudo deserializar Session. Evento: {}",
                                                event.getId()));
            }

                // Sesión expirada sin que el usuario pagara
            case "checkout.session.expired" -> {
                deserializer
                        .getObject()
                        .ifPresent(
                                obj -> {
                                    Session session = (Session) obj;
                                    log.info("Sesión expirada sin pago: {}", session.getId());
                                });
            }

                // Cobro recurrente exitoso → renovar suscripción PREMIUM
            case "invoice.payment_succeeded" -> {
                deserializer
                        .getObject()
                        .ifPresent(
                                obj -> {
                                    Invoice invoice = (Invoice) obj;
                                    // Solo procesar facturas de suscripción (no la
                                    // factura del alta
                                    // inicial,
                                    // que ya la maneja checkout.session.completed)
                                    if (invoice.getBillingReason() != null
                                            && invoice.getBillingReason()
                                                    .equals("subscription_cycle")) {
                                        procesarRenovacionFactura(invoice);
                                    }
                                });
            }

                // Cobro recurrente fallido → registrar fallo
            case "invoice.payment_failed" -> {
                deserializer
                        .getObject()
                        .ifPresent(
                                obj -> {
                                    Invoice invoice = (Invoice) obj;
                                    BigDecimal monto =
                                            BigDecimal.valueOf(invoice.getAmountDue())
                                                    .divide(
                                                            new BigDecimal("100"),
                                                            2,
                                                            RoundingMode.HALF_UP);
                                    log.warn(
                                            "Cobro recurrente fallido. Customer: {}, Monto: {}€",
                                            invoice.getCustomer(),
                                            monto);
                                    // Aquí podrías cancelar la suscripción o
                                    // notificar al usuario
                                });
            }

            default -> log.debug("Evento Stripe no manejado: {}", event.getType());
        }

        // Siempre 200 para que Stripe no reintente innecesariamente
        return ResponseEntity.ok("Evento recibido");
    }

    // -------------------------------------------------------------------------
    // Helpers privados del webhook
    // -------------------------------------------------------------------------

    /**
     * Procesa un checkout.session.completed. Si es SUSCRIPCION activa la suscripción; si es otro
     * tipo registra el pago.
     */
    private void procesarSessionCompletada(Session session) {
        try {
            String tipoStr = session.getMetadata().get("tipo");
            String usuarioIdStr = session.getMetadata().get("usuarioId");

            if (tipoStr == null || usuarioIdStr == null) {
                log.warn("Session {} sin metadata suficiente", session.getId());
                return;
            }

            Long usuarioId = Long.parseLong(usuarioIdStr);
            BigDecimal monto =
                    BigDecimal.valueOf(session.getAmountTotal())
                            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            TipoTransaccion tipo = TipoTransaccion.valueOf(tipoStr);

            switch (tipo) {
                case SUSCRIPCION -> {
                    // Activa suscripción + crea transacción + actualiza plan usuario
                    String periodo = session.getMetadata().get("periodo");
                    String planMeta = session.getMetadata().get("plan");
                    es.us.meerkat.backend.entity.TipoPlan plan =
                            "PRO".equalsIgnoreCase(planMeta)
                                    ? es.us.meerkat.backend.entity.TipoPlan.PRO
                                    : es.us.meerkat.backend.entity.TipoPlan.PREMIUM;
                    suscripcionService.activarSuscripcionTrasStripe(
                            usuarioId, monto, periodo, plan);
                }

                case PAGO_VERIFICACION -> {
                    // Activa la insignia verificado en el perfil del tutor
                    String tutorIdStr = session.getMetadata().get("tutorId");
                    if (tutorIdStr == null) {
                        log.warn(
                                "Session {} de verificación sin tutorId en metadata",
                                session.getId());
                        return;
                    }
                    Long tutorId = Long.parseLong(tutorIdStr);
                    tutorService.activarVerificacion(tutorId);

                    // Registrar transacción completada
                    paymentService.procesarPagoExitoso(
                            usuarioId,
                            TipoTransaccion.PAGO_VERIFICACION,
                            monto,
                            "Verificación de tutor completada vía Stripe",
                            null);
                }

                case PAGO_TUTOR, COMISION -> {
                    // Si es un plan corporativo institucional, activarlo
                    String institucionIdStr = session.getMetadata().get("institucionId");
                    if (tipo == TipoTransaccion.COMISION && institucionIdStr != null) {
                        Long institucionId = Long.parseLong(institucionIdStr);
                        String duracionStr = session.getMetadata().get("duracionMeses");
                        String emailContacto = session.getMetadata().get("emailContacto");
                        String tipoPlanCorporativoStr =
                                session.getMetadata().get("tipoPlanCorporativo");
                        Integer duracionMeses =
                                duracionStr != null ? Integer.parseInt(duracionStr) : 12;
                        es.us.meerkat.backend.entity.TipoPlanCorporativo tipoPlanCorporativo =
                                tipoPlanCorporativoStr != null
                                        ? es.us.meerkat.backend.entity.TipoPlanCorporativo.valueOf(
                                                tipoPlanCorporativoStr)
                                        : null;
                        institutionService.activarPlanCorporativo(
                                institucionId, duracionMeses, emailContacto, tipoPlanCorporativo);
                    }
                    // Registrar transacción
                    paymentService.procesarPagoExitoso(
                            usuarioId, tipo, monto, "Pago completado vía Stripe", null);
                }

                default -> log.warn("Tipo de transacción no manejado en webhook: {}", tipo);
            }

            log.info(
                    "Session completada procesada. Usuario: {}, Tipo: {}, Monto: {}€",
                    usuarioId,
                    tipo,
                    monto);

        } catch (Exception e) {
            log.error(
                    "Error procesando checkout.session.completed [{}]: {}",
                    session.getId(),
                    e.getMessage(),
                    e);
        }
    }

    /**
     * Procesa un invoice.payment_succeeded de tipo subscription_cycle (renovación). Necesita que el
     * invoice tenga metadata con usuarioId, o que lo resuelvas desde el customerId de Stripe
     * mapeado en tu BD.
     */
    private void procesarRenovacionFactura(Invoice invoice) {
        try {
            // Las invoices de suscripción recurrente no tienen metadata propia,
            // pero la suscripción de Stripe sí. Aquí usamos la metadata de la invoice
            // si la propagaste, o bien buscas el usuario por stripeCustomerId.
            String usuarioIdStr =
                    invoice.getMetadata() != null ? invoice.getMetadata().get("usuarioId") : null;

            if (usuarioIdStr == null) {
                log.warn(
                        "Invoice {} sin usuarioId en metadata. Considera guardar "
                                + "stripeCustomerId en Usuario para resolver este caso.",
                        invoice.getId());
                return;
            }

            Long usuarioId = Long.parseLong(usuarioIdStr);
            BigDecimal monto =
                    BigDecimal.valueOf(invoice.getAmountPaid())
                            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            String planMeta =
                    invoice.getMetadata() != null ? invoice.getMetadata().get("plan") : null;
            es.us.meerkat.backend.entity.TipoPlan plan =
                    "PRO".equalsIgnoreCase(planMeta)
                            ? es.us.meerkat.backend.entity.TipoPlan.PRO
                            : es.us.meerkat.backend.entity.TipoPlan.PREMIUM;

            suscripcionService.renovarSuscripcionTrasStripe(usuarioId, monto, plan);

            log.info("Renovación PREMIUM procesada. Usuario: {}, Monto: {}€", usuarioId, monto);

        } catch (Exception e) {
            log.error(
                    "Error procesando renovación de factura [{}]: {}",
                    invoice.getId(),
                    e.getMessage(),
                    e);
        }
    }

    // -------------------------------------------------------------------------
    // Helper DTO
    // -------------------------------------------------------------------------

    private TransactionResponse toTransactionResponse(TransaccionPago transaccion) {
        return TransactionResponse.builder()
                .id(transaccion.getId())
                .tipo(transaccion.getTipo())
                .monto(transaccion.getMonto())
                .moneda(transaccion.getMoneda())
                .comision(transaccion.getComision())
                .montoNeto(
                        transaccion
                                .getMonto()
                                .subtract(
                                        transaccion.getComision() != null
                                                ? transaccion.getComision()
                                                : BigDecimal.ZERO))
                .estado(transaccion.getEstado())
                .iniciadoAt(transaccion.getIniciadoAt())
                .completadoAt(transaccion.getCompletadoAt())
                .build();
    }
}
