package es.us.meerkat.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.checkout.SessionCreateParams;

import es.us.meerkat.backend.dto.PaymentUrlResponse;
import es.us.meerkat.backend.entity.*;
import es.us.meerkat.backend.repository.TransaccionPagoRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final TransaccionPagoRepository transaccionRepository;
    private final UsuarioRepository usuarioRepository;

    @Value("${stripe.success.url}")
    private String successUrl;

    @Value("${stripe.cancel.url}")
    private String cancelUrl;

    @Value("${stripe.price.premium}")
    private String pricePremium;

    private static final BigDecimal COMISION_PORCENTAJE = new BigDecimal("10");

    // -------------------------------------------------------------------------
    // Generación de sesiones de pago
    // -------------------------------------------------------------------------

    /** Verificación de tutor → TipoTransaccion.PAGO_VERIFICACION */
    /** ID del precio de verificación de tutor en Stripe (pago único) */
    private static final String PRICE_VERIFICACION_TUTOR = "price_1T8no6Iti4eEH8Y0mb6IAwMc";

    /** Verificación de tutor → TipoTransaccion.PAGO_VERIFICACION */
    @Transactional
    public PaymentUrlResponse generarPagoVerificacionTutor(Long tutorId, Long usuarioId)
            throws StripeException {

        SessionCreateParams params =
                SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setSuccessUrl(
                                successUrl + "?session_id={CHECKOUT_SESSION_ID}&tipo=verificacion")
                        .setCancelUrl(cancelUrl)
                        .addLineItem(
                                SessionCreateParams.LineItem.builder()
                                        .setQuantity(1L)
                                        .setPriceData(
                                                SessionCreateParams.LineItem.PriceData.builder()
                                                        .setCurrency("eur")
                                                        .setUnitAmount(1999L) // 19.99€
                                                        // en
                                                        // centavos
                                                        .setProductData(
                                                                SessionCreateParams.LineItem
                                                                        .PriceData.ProductData
                                                                        .builder()
                                                                        .setName(
                                                                                "Verificación de"
                                                                                        + " tutor")
                                                                        .setDescription(
                                                                                "Insignia"
                                                                                    + " 'Verificado'"
                                                                                    + " en tu"
                                                                                    + " perfil. Tu"
                                                                                    + " perfil"
                                                                                    + " aparecerá"
                                                                                    + " destacado"
                                                                                    + " en los"
                                                                                    + " listados.")
                                                                        .build())
                                                        .build())
                                        .build())
                        .putMetadata("tipo", TipoTransaccion.PAGO_VERIFICACION.name())
                        .putMetadata("usuarioId", usuarioId.toString())
                        .putMetadata("tutorId", tutorId.toString())
                        .build();

        Session session = Session.create(params);
        log.info(
                "Sesión Stripe verificación tutor creada para tutor {}: {}",
                tutorId,
                session.getId());
        return new PaymentUrlResponse(session.getUrl(), session.getId());
    }

    /** Contratación de tutor → TipoTransaccion.PAGO_TUTOR */
    @Transactional
    public PaymentUrlResponse generarPagoContratacionTutor(
            Long tutorId, Long comunidadId, BigDecimal monto, Long usuarioId)
            throws StripeException {

        Map<String, String> metadata = new HashMap<>();
        metadata.put("tipo", TipoTransaccion.PAGO_TUTOR.name());
        metadata.put("usuarioId", usuarioId.toString());
        metadata.put("tutorId", tutorId.toString());
        metadata.put("comunidadId", comunidadId.toString());

        long montoEnCentavos =
                monto.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValue();

        SessionCreateParams params =
                SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        // ← igual que verificacion: tipo en la URL para que el frontend sepa qué
                        // endpoint llamar
                        .setSuccessUrl(
                                successUrl + "?session_id={CHECKOUT_SESSION_ID}&tipo=contratacion")
                        .setCancelUrl(cancelUrl)
                        .addLineItem(
                                SessionCreateParams.LineItem.builder()
                                        .setQuantity(1L)
                                        .setPriceData(
                                                SessionCreateParams.LineItem.PriceData.builder()
                                                        .setCurrency("eur")
                                                        .setUnitAmount(montoEnCentavos)
                                                        .setProductData(
                                                                SessionCreateParams.LineItem
                                                                        .PriceData.ProductData
                                                                        .builder()
                                                                        .setName(
                                                                                "Contratación de"
                                                                                        + " tutor")
                                                                        .build())
                                                        .build())
                                        .build())
                        .putAllMetadata(metadata)
                        .build();

        Session session = Session.create(params);
        log.info("Sesión Stripe contratación tutor creada: {}", session.getId());
        return new PaymentUrlResponse(session.getUrl(), session.getId());
    }

    /** ID del precio Premium mensual en Stripe */
    private static final String PRICE_PREMIUM_MENSUAL = "price_1T9SPXIti4eEH8Y0ElUN2cxt";

    /** ID del precio Premium anual en Stripe */
    private static final String PRICE_PREMIUM_ANUAL = "price_1T9SQ4Iti4eEH8Y0vT8h39gU";

    public PaymentUrlResponse generarPagoSuscripcion(Usuario usuario, TipoPlan plan, String periodo)
            throws StripeException {

        String priceId =
                periodo.equalsIgnoreCase("anual") ? PRICE_PREMIUM_ANUAL : PRICE_PREMIUM_MENSUAL;

        SessionCreateParams params =
                SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                        .setSuccessUrl(
                                successUrl + "?session_id={CHECKOUT_SESSION_ID}&tipo=suscripcion")
                        .setCancelUrl(cancelUrl)
                        .setCustomerEmail(usuario.getEmail())
                        .addLineItem(
                                SessionCreateParams.LineItem.builder()
                                        .setPrice(priceId)
                                        .setQuantity(1L)
                                        .build())
                        .putMetadata("usuarioId", usuario.getId().toString())
                        .putMetadata("plan", plan.name())
                        .putMetadata("periodo", periodo)
                        .build();

        Session session = Session.create(params);
        return new PaymentUrlResponse(session.getUrl(), session.getId());
    }

    /**
     * Upgrade de comunidad → TipoTransaccion.COMISION (no hay un tipo específico, se registra como
     * comisión de plataforma)
     */
    @Transactional
    public PaymentUrlResponse generarPagoUpgradeComunidad(Long comunidadId, BigDecimal monto)
            throws StripeException {

        Map<String, String> metadata = new HashMap<>();
        metadata.put("tipo", TipoTransaccion.COMISION.name());
        metadata.put("comunidadId", comunidadId.toString());

        Session session = crearSesionPagoUnico("Upgrade comunidad a premium", monto, metadata);
        log.info("Sesión Stripe upgrade comunidad creada: {}", session.getId());
        return new PaymentUrlResponse(session.getUrl(), session.getId());
    }

    // Institucional Básico
    private static final String PRICE_INST_BASICO_MENSUAL = "price_1T8ifYIti4eEH8Y0uN8Ng7pW";
    private static final String PRICE_INST_BASICO_ANUAL = "price_1T8ih9Iti4eEH8Y0UyiUb3xX";

    // Institucional Estándar
    private static final String PRICE_INST_ESTANDAR_MENSUAL = "price_1T8iiKIti4eEH8Y0zPrrFGOg";
    private static final String PRICE_INST_ESTANDAR_ANUAL = "price_1T8iibIti4eEH8Y0CLxEtyR4";

    // Institucional Premium
    private static final String PRICE_INST_PREMIUM_MENSUAL = "price_1T8ijJIti4eEH8Y01fl4z9hj";
    private static final String PRICE_INST_PREMIUM_ANUAL = "price_1T8ijaIti4eEH8Y0Isb1PqIF";

    @Transactional
    public PaymentUrlResponse generarPagoPlanCorporativo(
            Long institucionId,
            TipoPlanCorporativo tipoPlan,
            BigDecimal monto,
            String periodo,
            String emailContacto)
            throws StripeException {

        // Seleccionar price ID según plan y periodo
        boolean esAnual =
                periodo != null && periodo.equalsIgnoreCase("anual")
                        || (monto != null && monto.compareTo(new BigDecimal("100")) > 0);

        String priceId =
                switch (tipoPlan) {
                    case BASICO -> esAnual ? PRICE_INST_BASICO_ANUAL : PRICE_INST_BASICO_MENSUAL;
                    case ESTANDAR ->
                            esAnual ? PRICE_INST_ESTANDAR_ANUAL : PRICE_INST_ESTANDAR_MENSUAL;
                    case PREMIUM -> esAnual ? PRICE_INST_PREMIUM_ANUAL : PRICE_INST_PREMIUM_MENSUAL;
                    default -> PRICE_INST_BASICO_MENSUAL;
                };

        SessionCreateParams.Builder paramsBuilder =
                SessionCreateParams.builder()
                        .setMode(
                                SessionCreateParams.Mode
                                        .SUBSCRIPTION) // En generarPagoPlanCorporativo
                        .setSuccessUrl(
                                successUrl + "?session_id={CHECKOUT_SESSION_ID}&tipo=institucional")
                        .setCancelUrl(cancelUrl)
                        .addLineItem(
                                SessionCreateParams.LineItem.builder()
                                        .setPrice(priceId)
                                        .setQuantity(1L)
                                        .build())
                        .putMetadata("tipo", TipoTransaccion.COMISION.name())
                        .putMetadata("institucionId", institucionId.toString())
                        .putMetadata("tipoPlanCorporativo", tipoPlan.name())
                        .putMetadata("periodo", esAnual ? "anual" : "mensual")
                        .putMetadata("duracionMeses", esAnual ? "12" : "1"); // ← añadir esto

        // Prerellenar email si está disponible
        if (emailContacto != null && !emailContacto.isBlank()) {
            paramsBuilder.setCustomerEmail(emailContacto);
        }

        Session session = Session.create(paramsBuilder.build());

        log.info(
                "Sesión Stripe plan corporativo {} ({}) creada para institución {}: {}",
                tipoPlan,
                esAnual ? "anual" : "mensual",
                institucionId,
                session.getId());

        return new PaymentUrlResponse(session.getUrl(), session.getId());
    }

    // -------------------------------------------------------------------------
    // Métodos de procesado (sin cambios)
    // -------------------------------------------------------------------------

    @Transactional
    public TransaccionPago procesarPagoExitoso(
            Long usuarioId,
            TipoTransaccion tipo,
            BigDecimal monto,
            String descripcion,
            Tutor tutor) {

        Usuario usuario =
                usuarioRepository
                        .findById(usuarioId)
                        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        BigDecimal comision = calcularComision(monto);

        TransaccionPago transaccion =
                TransaccionPago.builder()
                        .tipo(tipo)
                        .monto(monto)
                        .moneda("EUR")
                        .comision(comision)
                        .estado(EstadoTransaccion.COMPLETADA)
                        .usuario(usuario)
                        .tutor(tutor)
                        .iniciadoAt(LocalDateTime.now())
                        .completadoAt(LocalDateTime.now())
                        .build();

        return transaccionRepository.save(transaccion);
    }

    @Transactional
    public TransaccionPago procesarPagoFallido(
            Long usuarioId, TipoTransaccion tipo, BigDecimal monto, String razon) {

        Usuario usuario =
                usuarioRepository
                        .findById(usuarioId)
                        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        TransaccionPago transaccion =
                TransaccionPago.builder()
                        .tipo(tipo)
                        .monto(monto)
                        .moneda("EUR")
                        .comision(BigDecimal.ZERO)
                        .estado(EstadoTransaccion.FALLIDA)
                        .usuario(usuario)
                        .iniciadoAt(LocalDateTime.now())
                        .completadoAt(LocalDateTime.now())
                        .build();

        return transaccionRepository.save(transaccion);
    }

    public Page<TransaccionPago> obtenerHistorialPagos(Long usuarioId, Pageable pageable) {
        return transaccionRepository.findByUsuarioIdOrderByIniciadoAtDesc(usuarioId, pageable);
    }

    public Optional<TransaccionPago> obtenerTransaccion(Long transactionId, Long usuarioId) {
        return transaccionRepository.findByIdAndUsuarioId(transactionId, usuarioId);
    }

    public BigDecimal calcularComision(BigDecimal monto) {
        return monto.multiply(COMISION_PORCENTAJE)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal calcularMontoNeto(BigDecimal monto) {
        return monto.subtract(calcularComision(monto));
    }

    // -------------------------------------------------------------------------
    // Helper privado
    // -------------------------------------------------------------------------

    private Session crearSesionPagoUnico(
            String nombreProducto, BigDecimal monto, Map<String, String> metadata)
            throws StripeException {

        long montoEnCentavos =
                monto.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValue();

        SessionCreateParams params =
                SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                        .setCancelUrl(cancelUrl)
                        .addLineItem(
                                SessionCreateParams.LineItem.builder()
                                        .setQuantity(1L)
                                        .setPriceData(
                                                SessionCreateParams.LineItem.PriceData.builder()
                                                        .setCurrency("eur")
                                                        .setUnitAmount(montoEnCentavos)
                                                        .setProductData(
                                                                SessionCreateParams.LineItem
                                                                        .PriceData.ProductData
                                                                        .builder()
                                                                        .setName(nombreProducto)
                                                                        .build())
                                                        .build())
                                        .build())
                        .putAllMetadata(metadata)
                        .build();

        return Session.create(params);
    }

    // -------------------------------------------------------------------------
    // PaymentIntent para Stripe Elements (pago embebido en la app)
    // -------------------------------------------------------------------------

    /**
     * Crea un PaymentIntent para suscripcion Premium usando Stripe Elements. Devuelve clientSecret
     * para inicializar el formulario de pago embebido.
     */
    public Map<String, String> crearPaymentIntentSuscripcion(
            Usuario usuario, TipoPlan plan, String periodo) throws StripeException {

        long amount;
        String description;
        if ("anual".equalsIgnoreCase(periodo)) {
            amount = 2599L; // 25.99 EUR en centimos
            description = "Suscripcion Premium Anual - MeerKatters";
        } else {
            amount = 299L; // 2.99 EUR en centimos
            description = "Suscripcion Premium Mensual - MeerKatters";
        }

        PaymentIntentCreateParams params =
                PaymentIntentCreateParams.builder()
                        .setAmount(amount)
                        .setCurrency("eur")
                        .setDescription(description)
                        .setReceiptEmail(usuario.getEmail())
                        .putMetadata("usuarioId", usuario.getId().toString())
                        .putMetadata("plan", plan.name())
                        .putMetadata("periodo", periodo)
                        .putMetadata("tipo", TipoTransaccion.SUSCRIPCION.name())
                        .addPaymentMethodType("card")
                        .build();

        PaymentIntent intent = PaymentIntent.create(params);
        log.info("PaymentIntent creado para usuario {}: {}", usuario.getId(), intent.getId());

        Map<String, String> result = new HashMap<>();
        result.put("clientSecret", intent.getClientSecret());
        result.put("paymentIntentId", intent.getId());
        return result;
    }
}
