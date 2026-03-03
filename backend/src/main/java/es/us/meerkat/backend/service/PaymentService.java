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
import com.stripe.model.checkout.Session;
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
    @Transactional
    public PaymentUrlResponse generarPagoVerificacionTutor(Long tutorId, Long usuarioId)
            throws StripeException {

        Map<String, String> metadata = new HashMap<>();
        metadata.put("tipo", TipoTransaccion.PAGO_VERIFICACION.name());
        metadata.put("usuarioId", usuarioId.toString());
        metadata.put("tutorId", tutorId.toString());

        Session session =
                crearSesionPagoUnico("Verificación de tutor", new BigDecimal("9.99"), metadata);
        log.info("Sesión Stripe verificación tutor creada: {}", session.getId());
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

    /** Suscripción PREMIUM → TipoTransaccion.SUSCRIPCION, modo SUBSCRIPTION (recurrente) */
    @Transactional
    public PaymentUrlResponse generarPagoSuscripcion(Usuario usuario, TipoPlan plan)
            throws StripeException {

        if (plan == TipoPlan.FREE) {
            throw new IllegalArgumentException("El plan FREE no requiere pago");
        }

        Map<String, String> metadata = new HashMap<>();
        metadata.put("tipo", TipoTransaccion.SUSCRIPCION.name());
        metadata.put("usuarioId", usuario.getId().toString());
        metadata.put("plan", plan.name());

        SessionCreateParams params =
                SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                        .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                        .setCancelUrl(cancelUrl)
                        .setCustomerEmail(usuario.getEmail())
                        .addLineItem(
                                SessionCreateParams.LineItem.builder()
                                        .setQuantity(1L)
                                        .setPrice(pricePremium)
                                        .build())
                        .putAllMetadata(metadata)
                        .build();

        Session session = Session.create(params);
        log.info(
                "Sesión Stripe suscripción PREMIUM creada para usuario {}: {}",
                usuario.getId(),
                session.getId());
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

    /**
     * Plan corporativo → TipoTransaccion.COMISION (no hay un tipo específico, se registra como
     * comisión de plataforma)
     */
    @Transactional
    public PaymentUrlResponse generarPagoPlanCorporativo(
            Long institucionId, TipoPlanCorporativo tipoPlan, BigDecimal monto)
            throws StripeException {

        Map<String, String> metadata = new HashMap<>();
        metadata.put("tipo", TipoTransaccion.COMISION.name());
        metadata.put("institucionId", institucionId.toString());
        metadata.put("tipoPlanCorporativo", tipoPlan.name());

        Session session =
                crearSesionPagoUnico("Plan corporativo - " + tipoPlan.name(), monto, metadata);
        log.info(
                "Sesión Stripe plan corporativo {} creada para institución {}: {}",
                tipoPlan,
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
}
