package es.us.meerkat.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.stripe.exception.StripeException;

import es.us.meerkat.backend.dto.PaymentUrlResponse;
import es.us.meerkat.backend.entity.EstadoTransaccion;
import es.us.meerkat.backend.entity.TipoTransaccion;
import es.us.meerkat.backend.entity.TransaccionPago;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.TransaccionPagoRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private TransaccionPagoRepository transaccionRepository;
    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks private PaymentService paymentService;

    @Test
    void generarPagoVerificacionTutorShouldCreatePaymentSession() throws StripeException {
        Long tutorId = 1L;
        Long usuarioId = 10L;

        PaymentUrlResponse response =
                paymentService.generarPagoVerificacionTutor(tutorId, usuarioId);

        assertThat(response).isNotNull();
        assertThat(response.getUrl()).isNotNull();
        assertThat(response.getSessionId()).isNotNull();
    }

    @Test
    void generarPagoContratacionTutorShouldCreatePaymentSession() throws StripeException {
        Long tutorId = 1L;
        Long comunidadId = 10L;
        BigDecimal monto = new BigDecimal("100.00");
        Long usuarioId = 20L;

        PaymentUrlResponse response =
                paymentService.generarPagoContratacionTutor(tutorId, comunidadId, monto, usuarioId);

        assertThat(response).isNotNull();
        assertThat(response.getUrl()).isNotNull();
        assertThat(response.getSessionId()).isNotNull();
    }

    @Test
    void generarPagoUpgradeComunidadShouldCreatePaymentSession() throws StripeException {
        Long comunidadId = 10L;
        BigDecimal monto = new BigDecimal("50.00");

        PaymentUrlResponse response =
                paymentService.generarPagoUpgradeComunidad(comunidadId, monto);

        assertThat(response).isNotNull();
        assertThat(response.paymentUrl()).isNotNull();
        assertThat(response.sessionId()).isNotNull();
    }

    @Test
    void procesarPagoExitosoShouldCreateCompletedTransaction() {
        Long usuarioId = 1L;
        Usuario usuario = buildUsuario(usuarioId);
        BigDecimal monto = new BigDecimal("100.00");

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(transaccionRepository.save(any(TransaccionPago.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransaccionPago transaccion =
                paymentService.procesarPagoExitoso(
                        usuarioId, TipoTransaccion.PAGO_TUTOR, monto, "Pago de tutor", null);

        assertThat(transaccion.getEstado()).isEqualTo(EstadoTransaccion.COMPLETADA);
        assertThat(transaccion.getMonto()).isEqualTo(monto);
        assertThat(transaccion.getUsuario()).isEqualTo(usuario);
        assertThat(transaccion.getTipo()).isEqualTo(TipoTransaccion.PAGO_TUTOR);
        verify(transaccionRepository).save(transaccion);
    }

    @Test
    void procesarPagoExitosoShouldCalculateCommission() {
        Long usuarioId = 1L;
        Usuario usuario = buildUsuario(usuarioId);
        BigDecimal monto = new BigDecimal("100.00");

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(transaccionRepository.save(any(TransaccionPago.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransaccionPago transaccion =
                paymentService.procesarPagoExitoso(
                        usuarioId, TipoTransaccion.PAGO_TUTOR, monto, "Pago de tutor", null);

        assertThat(transaccion.getComision()).isEqualTo(new BigDecimal("10.00")); // 10% de 100
    }

    @Test
    void procesarPagoExitosoShouldFailWhenUsuarioNotFound() {
        Long usuarioId = 999L;
        BigDecimal monto = new BigDecimal("100.00");

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                paymentService.procesarPagoExitoso(
                                        usuarioId, TipoTransaccion.PAGO_TUTOR, monto, "Pago", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Usuario no encontrado");
    }

    @Test
    void procesarPagoFallidoShouldCreateFailedTransaction() {
        Long usuarioId = 1L;
        Usuario usuario = buildUsuario(usuarioId);
        BigDecimal monto = new BigDecimal("100.00");

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(transaccionRepository.save(any(TransaccionPago.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransaccionPago transaccion =
                paymentService.procesarPagoFallido(
                        usuarioId, TipoTransaccion.PAGO_TUTOR, monto, "Falta de fondos");

        assertThat(transaccion.getEstado()).isEqualTo(EstadoTransaccion.FALLIDA);
        assertThat(transaccion.getMonto()).isEqualTo(monto);
        verify(transaccionRepository).save(transaccion);
    }

    @Test
    void procesarPagoFallidoShouldFailWhenUsuarioNotFound() {
        Long usuarioId = 999L;
        BigDecimal monto = new BigDecimal("100.00");

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                paymentService.procesarPagoFallido(
                                        usuarioId, TipoTransaccion.PAGO_TUTOR, monto, "Error"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Usuario no encontrado");
    }

    @Test
    void obtenerHistorialPagosShouldReturnPageOfTransactions() {
        Long usuarioId = 1L;
        when(transaccionRepository.findByUsuarioIdOrderByIniciadoAtDesc(
                        usuarioId, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(java.util.List.of()));

        var result = paymentService.obtenerHistorialPagos(usuarioId, PageRequest.of(0, 10));

        assertThat(result).isNotNull();
        verify(transaccionRepository)
                .findByUsuarioIdOrderByIniciadoAtDesc(usuarioId, PageRequest.of(0, 10));
    }

    @Test
    void obtenerTransaccionShouldReturnTransactionWhenExists() {
        Long transactionId = 1L;
        Long usuarioId = 10L;
        TransaccionPago transaccion = buildTransaccion(transactionId, usuarioId);

        when(transaccionRepository.findByIdAndUsuarioId(transactionId, usuarioId))
                .thenReturn(Optional.of(transaccion));

        var result = paymentService.obtenerTransaccion(transactionId, usuarioId);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(transactionId);
    }

    @Test
    void calcularComisionShouldReturn10PercentOfAmount() {
        BigDecimal monto = new BigDecimal("100.00");
        BigDecimal comision = paymentService.calcularComision(monto);

        assertThat(comision).isEqualTo(new BigDecimal("10.00"));
    }

    @Test
    void calcularMontoNetoShouldReturnNetAmount() {
        BigDecimal monto = new BigDecimal("100.00");
        BigDecimal montoNeto = paymentService.calcularMontoNeto(monto);

        assertThat(montoNeto).isEqualTo(new BigDecimal("90.00"));
    }

    // Helper methods
    private Usuario buildUsuario(Long id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNombre("Juan");
        usuario.setEmail("juan@example.com");
        usuario.setPassword("password");
        return usuario;
    }

    private TransaccionPago buildTransaccion(Long id, Long usuarioId) {
        return TransaccionPago.builder()
                .id(id)
                .usuario(buildUsuario(usuarioId))
                .tipo(TipoTransaccion.PAGO_TUTOR)
                .monto(new BigDecimal("100.00"))
                .estado(EstadoTransaccion.COMPLETADA)
                .build();
    }
}
