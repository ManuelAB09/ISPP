package es.us.meerkat.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.us.meerkat.backend.dto.PaymentUrlResponse;
import es.us.meerkat.backend.entity.*;
import es.us.meerkat.backend.repository.TransaccionPagoRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

/**
 * Servicio para gestionar pagos y transacciones.
 *
 * <p>Mock de Stripe: En producción esto se integraría con la pasarela Stripe. Actualmente simula
 * los pagos para desarrollo.
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final TransaccionPagoRepository transaccionRepository;
    private final UsuarioRepository usuarioRepository;

    // Comisión de la plataforma en porcentaje
    private static final BigDecimal COMISION_PORCENTAJE = new BigDecimal("10");

    /**
     * Genera una URL de pago para verificación de tutor. Mock de Stripe - en producción devolvería
     * la URL de Stripe Checkout.
     *
     * @param tutorId ID del tutor
     * @param usuarioId ID del usuario autenticado
     * @return URL de pago
     */
    @Transactional
    public PaymentUrlResponse generarPagoVerificacionTutor(Long tutorId, Long usuarioId) {
        String sessionId = UUID.randomUUID().toString();
        String paymentUrl =
                "http://localhost:8080/api/v1/payment/checkout/verification/" + sessionId;
        return new PaymentUrlResponse(paymentUrl, sessionId);
    }

    /**
     * Genera una URL de pago para contratación de tutor. Mock de Stripe.
     *
     * @param tutorId ID del tutor
     * @param comunidadId ID de la comunidad
     * @param monto monto a pagar
     * @param usuarioId ID del usuario autenticado
     * @return URL de pago
     */
    @Transactional
    public PaymentUrlResponse generarPagoContratacionTutor(
            Long tutorId, Long comunidadId, BigDecimal monto, Long usuarioId) {
        String sessionId = UUID.randomUUID().toString();
        String paymentUrl = "http://localhost:8080/api/v1/payment/checkout/tutor/" + sessionId;
        return new PaymentUrlResponse(paymentUrl, sessionId);
    }

    /**
     * Genera una URL de pago para suscripción premium. Mock de Stripe.
     *
     * @param usuario usuario que se suscribe
     * @param plan plan a contratar
     * @return URL de pago
     */
    @Transactional
    public PaymentUrlResponse generarPagoSuscripcion(Usuario usuario, TipoPlan plan) {
        String sessionId = UUID.randomUUID().toString();
        String paymentUrl =
                "http://localhost:8080/api/v1/payment/checkout/subscription/" + sessionId;
        return new PaymentUrlResponse(paymentUrl, sessionId);
    }

    /**
     * Genera una URL de pago para upgrade de comunidad a premium. Mock de Stripe.
     *
     * @param comunidadId ID de la comunidad
     * @param monto monto a pagar
     * @return URL de pago
     */
    @Transactional
    public PaymentUrlResponse generarPagoUpgradeComunidad(Long comunidadId, BigDecimal monto) {
        String sessionId = UUID.randomUUID().toString();
        String paymentUrl =
                "http://localhost:8080/api/v1/payment/checkout/community-upgrade/" + sessionId;
        return new PaymentUrlResponse(paymentUrl, sessionId);
    }

    /**
     * Genera una URL de pago para plan corporativo. Mock de Stripe.
     *
     * @param institucionId ID de la institución
     * @param monto monto a pagar
     * @return URL de pago
     */
    @Transactional
    public PaymentUrlResponse generarPagoPlanCorporativo(Long institucionId, BigDecimal monto) {
        String sessionId = UUID.randomUUID().toString();
        String paymentUrl =
                "http://localhost:8080/api/v1/payment/checkout/corporate-plan/" + sessionId;
        return new PaymentUrlResponse(paymentUrl, sessionId);
    }

    /**
     * Procesa (simula) un pago exitoso.
     *
     * @param usuarioId ID del usuario
     * @param tipo tipo de transacción
     * @param monto monto del pago
     * @param descripcion descripción de la transacción
     * @param tutor tutor involucrado (si aplica)
     * @return transacción creada
     */
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

        // Calcular comisión
        BigDecimal comision = calcularComision(monto);
        BigDecimal montoNeto = monto.subtract(comision);

        // Crear transacción
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

    /**
     * Procesa un pago fallido.
     *
     * @param usuarioId ID del usuario
     * @param tipo tipo de transacción
     * @param monto monto del pago
     * @param razon razón del fallo
     * @return transacción creada con estado fallido
     */
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

    /**
     * Obtiene el historial de pagos de un usuario.
     *
     * @param usuarioId ID del usuario
     * @param pageable información de paginación
     * @return página con las transacciones
     */
    public Page<TransaccionPago> obtenerHistorialPagos(Long usuarioId, Pageable pageable) {
        return transaccionRepository.findByUsuarioIdOrderByIniciadoAtDesc(usuarioId, pageable);
    }

    /**
     * Obtiene una transacción específica.
     *
     * @param transactionId ID de la transacción
     * @param usuarioId ID del usuario (para verificar permisos)
     * @return transacción
     */
    public Optional<TransaccionPago> obtenerTransaccion(Long transactionId, Long usuarioId) {
        return transaccionRepository.findByIdAndUsuarioId(transactionId, usuarioId);
    }

    /**
     * Calcula la comisión de la plataforma sobre un monto.
     *
     * @param monto monto original
     * @return comisión calculada
     */
    public BigDecimal calcularComision(BigDecimal monto) {
        return monto.multiply(COMISION_PORCENTAJE)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    /**
     * Calcula el monto neto después de comisión.
     *
     * @param monto monto original
     * @return monto neto
     */
    public BigDecimal calcularMontoNeto(BigDecimal monto) {
        return monto.subtract(calcularComision(monto));
    }
}
