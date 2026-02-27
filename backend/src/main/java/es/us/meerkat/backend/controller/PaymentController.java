package es.us.meerkat.backend.controller;

import java.time.LocalDate;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import es.us.meerkat.backend.dto.PageInfo;
import es.us.meerkat.backend.dto.TransactionListResponse;
import es.us.meerkat.backend.dto.TransactionResponse;
import es.us.meerkat.backend.entity.TransaccionPago;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** Controlador REST para gestionar pagos y transacciones. */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Pagos", description = "Gestión de pagos y transacciones")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Obtiene el historial de pagos del usuario autenticado.
     *
     * @param usuario Usuario autenticado
     * @param tipo Tipo de transacción (opcional)
     * @param desde Fecha desde (opcional)
     * @param hasta Fecha hasta (opcional)
     * @param page Número de página
     * @param size Tamaño de página
     * @return Lista paginada de transacciones
     */
    @GetMapping("/history")
    @Operation(
            summary = "Historial de pagos",
            description = "Devuelve el historial de pagos del usuario autenticado")
    public ResponseEntity<TransactionListResponse> getPaymentHistory(
            @AuthenticationPrincipal final Usuario usuario,
            @Parameter(description = "Filtrar por tipo de transacción")
                    @RequestParam(required = false)
                    String tipo,
            @Parameter(description = "Fecha desde") @RequestParam(required = false) LocalDate desde,
            @Parameter(description = "Fecha hasta") @RequestParam(required = false) LocalDate hasta,
            @Parameter(description = "Número de página (0-indexed)")
                    @RequestParam(defaultValue = "0")
                    int page,
            @Parameter(description = "Elementos por página") @RequestParam(defaultValue = "20")
                    int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<TransaccionPago> payments =
                paymentService.obtenerHistorialPagos(usuario.getId(), pageable);

        // Convertir a DTOs
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

    /**
     * Obtiene el detalle de una transacción específica.
     *
     * @param transactionId ID de la transacción
     * @param usuario Usuario autenticado
     * @return Detalle de la transacción
     */
    @GetMapping("/{transactionId}")
    @Operation(
            summary = "Obtener detalle de transacción",
            description = "Devuelve el detalle de una transacción específica")
    public ResponseEntity<TransactionResponse> getTransaction(
            @Parameter(description = "ID de la transacción") @PathVariable Long transactionId,
            @AuthenticationPrincipal final Usuario usuario) {

        return paymentService
                .obtenerTransaccion(transactionId, usuario.getId())
                .map(transaccion -> ResponseEntity.ok(toTransactionResponse(transaccion)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Webhook para recibir notificaciones de Stripe (simplificado para mock). En producción esto
     * verificaría la firma de Stripe y procesaría el evento.
     *
     * @param payload Payload del webhook
     * @return Confirmación de recepción
     */
    @PostMapping("/webhook")
    @Operation(
            summary = "Webhook de pasarela de pago",
            description = "Endpoint para recibir notificaciones de la pasarela de pago (Stripe)")
    public ResponseEntity<?> handleWebhook(@RequestBody String payload) {
        // En producción:
        // 1. Verificar la firma del webhook
        // 2. Parsear el evento
        // 3. Procesar según el tipo de evento

        // Para mock, simplemente aceptamos
        return ResponseEntity.ok().build();
    }

    /**
     * Convierte una entidad TransaccionPago a su DTO.
     *
     * @param transaccion la transacción
     * @return DTO de transacción
     */
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
                                                : java.math.BigDecimal.ZERO))
                .estado(transaccion.getEstado())
                .iniciadoAt(transaccion.getIniciadoAt())
                .completadoAt(transaccion.getCompletadoAt())
                .build();
    }
}
