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
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.checkout.SessionCreateParams;

import es.us.meerkat.backend.dto.PaymentUrlResponse;
import es.us.meerkat.backend.entity.*;
import es.us.meerkat.backend.repository.TransaccionPagoRepository;
import es.us.meerkat.backend.repository.TutorRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final TransaccionPagoRepository transaccionRepository;
    private final UsuarioRepository usuarioRepository;
    private final TutorRepository tutorRepository;

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
                                                        .setUnitAmount(1999L) // 19.99€ en centavos
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

        Session session = crearSesionPagoUnico("Contratación de tutor", monto, metadata);
        log.info("Sesión Stripe contratación tutor creada: {}", session.getId());
        return new PaymentUrlResponse(session.getUrl(), session.getId());
    }

    /** ID del precio Premium mensual en Stripe */
    private static final String PRICE_PREMIUM_MENSUAL = "price_1T5p9zIti4eEH8Y0Sr09PRkj";

    /** ID del precio Premium anual en Stripe */
    private static final String PRICE_PREMIUM_ANUAL = "price_1T8hTmIti4eEH8Y01iZAD8gY";

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
            amount = 3145L; // 25.99 + 21% IVA = 31.45 EUR en centimos
            description = "Suscripcion Premium Anual (IVA incluido) - MeerKatters";
        } else {
            amount = 362L; // 2.99 + 21% IVA = 3.62 EUR en centimos
            description = "Suscripcion Premium Mensual (IVA incluido) - MeerKatters";
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

    /**
     * Crea un PaymentIntent para verificación de tutor (pago único embebido). Devuelve clientSecret
     * para inicializar Stripe Elements en el frontend.
     */
    public Map<String, String> crearPaymentIntentVerificacion(
            Long tutorId, Long usuarioId, String email) throws StripeException {

        PaymentIntentCreateParams params =
                PaymentIntentCreateParams.builder()
                        .setAmount(1999L) // 19.99 EUR en céntimos
                        .setCurrency("eur")
                        .setDescription("Verificación de tutor - MeerKatters")
                        .setReceiptEmail(email)
                        .putMetadata("usuarioId", usuarioId.toString())
                        .putMetadata("tutorId", tutorId.toString())
                        .putMetadata("tipo", TipoTransaccion.PAGO_VERIFICACION.name())
                        .addPaymentMethodType("card")
                        .build();

        PaymentIntent intent = PaymentIntent.create(params);
        log.info("PaymentIntent verificación creado para tutor {}: {}", tutorId, intent.getId());

        Map<String, String> result = new HashMap<>();
        result.put("clientSecret", intent.getClientSecret());
        result.put("paymentIntentId", intent.getId());
        return result;
    }

    /**
     * Crea un PaymentIntent para plan corporativo institucional (pago embebido). Devuelve
     * clientSecret para inicializar Stripe Elements en el frontend.
     */
    public Map<String, String> crearPaymentIntentPlanCorporativo(
            Long institucionId,
            TipoPlanCorporativo tipoPlan,
            String periodo,
            String emailContacto,
            Integer duracionMeses)
            throws StripeException {

        boolean esAnual =
                periodo != null && periodo.equalsIgnoreCase("anual")
                        || (duracionMeses != null && duracionMeses >= 12);

        long amount = calcularMontoCentimos(tipoPlan, esAnual);

        String description =
                "Plan Institucional "
                        + tipoPlan.name()
                        + " ("
                        + (esAnual ? "anual" : "mensual")
                        + ") - MeerKatters";

        PaymentIntentCreateParams.Builder builder =
                PaymentIntentCreateParams.builder()
                        .setAmount(amount)
                        .setCurrency("eur")
                        .setDescription(description)
                        .putMetadata("institucionId", institucionId.toString())
                        .putMetadata("tipoPlanCorporativo", tipoPlan.name())
                        .putMetadata("periodo", esAnual ? "anual" : "mensual")
                        .putMetadata(
                                "duracionMeses",
                                String.valueOf(
                                        duracionMeses != null ? duracionMeses : (esAnual ? 12 : 1)))
                        .putMetadata("tipo", TipoTransaccion.COMISION.name())
                        .addPaymentMethodType("card");

        if (emailContacto != null && !emailContacto.isBlank()) {
            builder.setReceiptEmail(emailContacto);
            builder.putMetadata("emailContacto", emailContacto);
        }

        PaymentIntent intent = PaymentIntent.create(builder.build());
        log.info(
                "PaymentIntent plan corporativo {} creado para institución {}: {}",
                tipoPlan,
                institucionId,
                intent.getId());

        Map<String, String> result = new HashMap<>();
        result.put("clientSecret", intent.getClientSecret());
        result.put("paymentIntentId", intent.getId());
        return result;
    }

    /** Calcula el monto en céntimos para planes corporativos. */
    private long calcularMontoCentimos(TipoPlanCorporativo tipoPlan, boolean esAnual) {
        return switch (tipoPlan) {
            case BASICO -> esAnual ? 49999L : 4999L;
            case ESTANDAR -> esAnual ? 99999L : 9999L;
            case PREMIUM -> esAnual ? 199999L : 19999L;
            case REDUCIDO_PUBLICA, REDUCIDO_PRIVADA -> esAnual ? 49999L : 4999L;
        };
    }

    /**
     * Crea un PaymentIntent para contratación de tutor (pago embebido). Devuelve clientSecret para
     * inicializar Stripe Elements en el frontend.
     */
    public Map<String, String> crearPaymentIntentContratacionTutor(
            Long tutorId,
            Long comunidadId,
            java.math.BigDecimal monto,
            Long usuarioId,
            String email)
            throws StripeException {

        long montoEnCentavos =
                monto.multiply(new java.math.BigDecimal("100"))
                        .setScale(0, java.math.RoundingMode.HALF_UP)
                        .longValue();

        PaymentIntentCreateParams.Builder builder =
                PaymentIntentCreateParams.builder()
                        .setAmount(montoEnCentavos)
                        .setCurrency("eur")
                        .setDescription("Contratación de tutor - MeerKatters")
                        .putMetadata("usuarioId", usuarioId.toString())
                        .putMetadata("tutorId", tutorId.toString())
                        .putMetadata("tipo", TipoTransaccion.PAGO_TUTOR.name())
                        .addPaymentMethodType("card");

        if (comunidadId != null) {
            builder.putMetadata("comunidadId", comunidadId.toString());
        }
        if (email != null && !email.isBlank()) {
            builder.setReceiptEmail(email);
        }

        // If tutor has a connected Stripe account, route the payment to them
        Tutor tutor = tutorRepository.findById(tutorId).orElse(null);
        if (tutor != null
                && tutor.getStripeAccountId() != null
                && !tutor.getStripeAccountId().isBlank()) {
            long comisionCentavos =
                    calcularComision(monto)
                            .multiply(new java.math.BigDecimal("100"))
                            .setScale(0, java.math.RoundingMode.HALF_UP)
                            .longValue();
            builder.setApplicationFeeAmount(comisionCentavos);
            builder.setTransferData(
                    PaymentIntentCreateParams.TransferData.builder()
                            .setDestination(tutor.getStripeAccountId())
                            .build());
        }

        PaymentIntent intent = PaymentIntent.create(builder.build());
        log.info(
                "PaymentIntent contratación tutor creado para tutor {}: {}",
                tutorId,
                intent.getId());

        Map<String, String> result = new HashMap<>();
        result.put("clientSecret", intent.getClientSecret());
        result.put("paymentIntentId", intent.getId());
        return result;
    }

    // -------------------------------------------------------------------------
    // Stripe Connect (cuentas conectadas de tutores)
    // -------------------------------------------------------------------------

    /** Crea una cuenta Express de Stripe Connect para el tutor. */
    @Transactional
    public String crearCuentaConectadaTutor(Tutor tutor) throws StripeException {
        if (tutor.getStripeAccountId() != null && !tutor.getStripeAccountId().isBlank()) {
            return tutor.getStripeAccountId();
        }

        AccountCreateParams params =
                AccountCreateParams.builder()
                        .setType(AccountCreateParams.Type.EXPRESS)
                        .setEmail(tutor.getUsuario().getEmail())
                        .setCountry("ES")
                        .setBusinessType(AccountCreateParams.BusinessType.INDIVIDUAL)
                        .setCapabilities(
                                AccountCreateParams.Capabilities.builder()
                                        .setCardPayments(
                                                AccountCreateParams.Capabilities.CardPayments
                                                        .builder()
                                                        .setRequested(true)
                                                        .build())
                                        .setTransfers(
                                                AccountCreateParams.Capabilities.Transfers.builder()
                                                        .setRequested(true)
                                                        .build())
                                        .build())
                        .build();

        Account account = Account.create(params);
        tutor.setStripeAccountId(account.getId());
        tutorRepository.save(tutor);

        log.info("Cuenta Stripe Connect creada para tutor {}: {}", tutor.getId(), account.getId());
        return account.getId();
    }

    /** Genera un link de onboarding de Stripe Connect para un tutor. */
    public String generarOnboardingLink(String stripeAccountId, String returnUrl)
            throws StripeException {
        AccountLinkCreateParams params =
                AccountLinkCreateParams.builder()
                        .setAccount(stripeAccountId)
                        .setRefreshUrl(returnUrl)
                        .setReturnUrl(returnUrl)
                        .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                        .build();

        AccountLink link = AccountLink.create(params);
        return link.getUrl();
    }

    /** Comprueba si la cuenta conectada de Stripe está operativa. */
    public boolean cuentaConectadaActiva(String stripeAccountId) throws StripeException {
        Account account = Account.retrieve(stripeAccountId);
        return Boolean.TRUE.equals(account.getChargesEnabled())
                && Boolean.TRUE.equals(account.getPayoutsEnabled());
    }

    // -------------------------------------------------------------------------
    // Ganancias del tutor
    // -------------------------------------------------------------------------

    /** Obtiene las transacciones de pago completadas para un tutor (paginado). */
    public Page<TransaccionPago> obtenerGananciasTutor(Long tutorId, Pageable pageable) {
        return transaccionRepository.findByTutorIdAndTipoAndEstadoOrderByCompletadoAtDesc(
                tutorId, TipoTransaccion.PAGO_TUTOR, EstadoTransaccion.COMPLETADA, pageable);
    }

    /** Obtiene todas las transacciones completadas de un tutor (para calcular total). */
    public java.util.List<TransaccionPago> obtenerTodasGananciasTutor(Long tutorId) {
        return transaccionRepository.findByTutorIdAndTipoAndEstadoOrderByCompletadoAtDesc(
                tutorId, TipoTransaccion.PAGO_TUTOR, EstadoTransaccion.COMPLETADA);
    }
}
