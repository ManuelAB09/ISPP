package es.us.meerkat.backend.service.subscriptions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;

import es.us.meerkat.backend.dto.subscriptions.PaymentUrlResponse;
import es.us.meerkat.backend.entity.subscriptions.EstadoTransaccion;
import es.us.meerkat.backend.entity.subscriptions.TipoPlan;
import es.us.meerkat.backend.entity.subscriptions.TipoPlanCorporativo;
import es.us.meerkat.backend.entity.subscriptions.TipoTransaccion;
import es.us.meerkat.backend.entity.subscriptions.TransaccionPago;
import es.us.meerkat.backend.entity.tutors.Tutor;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.subscriptions.TransaccionPagoRepository;
import es.us.meerkat.backend.repository.tutors.TutorRepository;
import es.us.meerkat.backend.repository.users.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private TransaccionPagoRepository transaccionRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private TutorRepository tutorRepository;

    @InjectMocks private PaymentService paymentService;

    @Test
    void generarPagoVerificacionTutorShouldCreatePaymentSession() throws StripeException {
        Long tutorId = 1L;
        Long usuarioId = 10L;

        Session sessionMock = org.mockito.Mockito.mock(Session.class);
        when(sessionMock.getUrl()).thenReturn("http://fake.url");
        when(sessionMock.getId()).thenReturn("session123");

        try (var mocked = mockStatic(Session.class)) {
            mocked.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenReturn(sessionMock);

            PaymentUrlResponse response =
                    paymentService.generarPagoVerificacionTutor(tutorId, usuarioId);

            assertThat(response).isNotNull();
            assertThat(response.paymentUrl()).isEqualTo("http://fake.url");
            assertThat(response.sessionId()).isEqualTo("session123");
        }
    }

    @Test
    void generarPagoContratacionTutorShouldCreatePaymentSession() throws StripeException {
        Long tutorId = 1L;
        Long comunidadId = 10L;
        BigDecimal monto = new BigDecimal("100.00");
        Long usuarioId = 20L;

        Session sessionMock = org.mockito.Mockito.mock(Session.class);
        when(sessionMock.getUrl()).thenReturn("http://fake.url");
        when(sessionMock.getId()).thenReturn("session123");

        try (var mocked = mockStatic(Session.class)) {
            mocked.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenReturn(sessionMock);

            PaymentUrlResponse response =
                    paymentService.generarPagoContratacionTutor(
                            tutorId, comunidadId, monto, usuarioId);

            assertThat(response).isNotNull();
            assertThat(response.paymentUrl()).isEqualTo("http://fake.url");
            assertThat(response.sessionId()).isEqualTo("session123");
        }
    }

    @Test
    void generarPagoUpgradeComunidadShouldCreatePaymentSession() throws StripeException {
        Long comunidadId = 10L;
        BigDecimal monto = new BigDecimal("50.00");

        Session sessionMock = org.mockito.Mockito.mock(Session.class);
        when(sessionMock.getUrl()).thenReturn("http://fake.url");
        when(sessionMock.getId()).thenReturn("session123");

        try (var mocked = mockStatic(Session.class)) {
            mocked.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenReturn(sessionMock);

            PaymentUrlResponse response =
                    paymentService.generarPagoUpgradeComunidad(comunidadId, monto);

            assertThat(response).isNotNull();
            assertThat(response.paymentUrl()).isEqualTo("http://fake.url");
            assertThat(response.sessionId()).isEqualTo("session123");
        }
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

    @Test
    void obtenerGananciasTutorShouldReturnPagedResults() {
        Long tutorId = 5L;
        Pageable pageable = PageRequest.of(0, 10);
        TransaccionPago tx = buildTransaccion(1L, 10L);
        Page<TransaccionPago> page = new PageImpl<>(List.of(tx));

        when(transaccionRepository.findByTutorIdAndTipoAndEstadoOrderByCompletadoAtDesc(
                        tutorId,
                        TipoTransaccion.PAGO_TUTOR,
                        EstadoTransaccion.COMPLETADA,
                        pageable))
                .thenReturn(page);

        Page<TransaccionPago> result = paymentService.obtenerGananciasTutor(tutorId, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(transaccionRepository)
                .findByTutorIdAndTipoAndEstadoOrderByCompletadoAtDesc(
                        tutorId,
                        TipoTransaccion.PAGO_TUTOR,
                        EstadoTransaccion.COMPLETADA,
                        pageable);
    }

    @Test
    void obtenerGananciasTutorShouldReturnEmptyPageWhenNoTransactions() {
        Long tutorId = 99L;
        Pageable pageable = PageRequest.of(0, 10);

        when(transaccionRepository.findByTutorIdAndTipoAndEstadoOrderByCompletadoAtDesc(
                        tutorId,
                        TipoTransaccion.PAGO_TUTOR,
                        EstadoTransaccion.COMPLETADA,
                        pageable))
                .thenReturn(Page.empty());

        Page<TransaccionPago> result = paymentService.obtenerGananciasTutor(tutorId, pageable);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void obtenerTodasGananciasTutorShouldReturnList() {
        Long tutorId = 5L;
        TransaccionPago tx = buildTransaccion(1L, 10L);

        when(transaccionRepository.findByTutorIdAndTipoAndEstadoOrderByCompletadoAtDesc(
                        tutorId, TipoTransaccion.PAGO_TUTOR, EstadoTransaccion.COMPLETADA))
                .thenReturn(List.of(tx));

        List<TransaccionPago> result = paymentService.obtenerTodasGananciasTutor(tutorId);

        assertThat(result).hasSize(1);
    }

    @Test
    void obtenerTodasGananciasTutorShouldReturnEmptyListWhenNone() {
        Long tutorId = 99L;

        when(transaccionRepository.findByTutorIdAndTipoAndEstadoOrderByCompletadoAtDesc(
                        tutorId, TipoTransaccion.PAGO_TUTOR, EstadoTransaccion.COMPLETADA))
                .thenReturn(List.of());

        List<TransaccionPago> result = paymentService.obtenerTodasGananciasTutor(tutorId);

        assertThat(result).isEmpty();
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

    // ── generarPagoSuscripcion ──────────────────────────────────────

    @Test
    void generarPagoSuscripcionShouldCreatePremiumMensualSession() throws StripeException {
        Usuario usuario = buildUsuario(1L);
        Session sessionMock = org.mockito.Mockito.mock(Session.class);
        when(sessionMock.getUrl()).thenReturn("http://premium.url");
        when(sessionMock.getId()).thenReturn("ses_premium");

        try (var mocked = mockStatic(Session.class)) {
            mocked.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenReturn(sessionMock);

            PaymentUrlResponse result =
                    paymentService.generarPagoSuscripcion(usuario, TipoPlan.PREMIUM, "mensual");

            assertThat(result.paymentUrl()).isEqualTo("http://premium.url");
            assertThat(result.sessionId()).isEqualTo("ses_premium");
        }
    }

    @Test
    void generarPagoSuscripcionShouldCreatePremiumAnualSession() throws StripeException {
        Usuario usuario = buildUsuario(1L);
        Session sessionMock = org.mockito.Mockito.mock(Session.class);
        when(sessionMock.getUrl()).thenReturn("http://premium-anual.url");
        when(sessionMock.getId()).thenReturn("ses_premium_anual");

        try (var mocked = mockStatic(Session.class)) {
            mocked.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenReturn(sessionMock);

            PaymentUrlResponse result =
                    paymentService.generarPagoSuscripcion(usuario, TipoPlan.PREMIUM, "anual");

            assertThat(result.paymentUrl()).isEqualTo("http://premium-anual.url");
        }
    }

    @Test
    void generarPagoSuscripcionShouldCreateProMensualSession() throws StripeException {
        Usuario usuario = buildUsuario(1L);
        Session sessionMock = org.mockito.Mockito.mock(Session.class);
        when(sessionMock.getUrl()).thenReturn("http://pro.url");
        when(sessionMock.getId()).thenReturn("ses_pro");

        try (var mocked = mockStatic(Session.class)) {
            mocked.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenReturn(sessionMock);

            PaymentUrlResponse result =
                    paymentService.generarPagoSuscripcion(usuario, TipoPlan.PRO, "mensual");

            assertThat(result.paymentUrl()).isEqualTo("http://pro.url");
        }
    }

    @Test
    void generarPagoSuscripcionShouldDefaultToPremuimWhenNullPlan() throws StripeException {
        Usuario usuario = buildUsuario(1L);
        Session sessionMock = org.mockito.Mockito.mock(Session.class);
        when(sessionMock.getUrl()).thenReturn("http://default.url");
        when(sessionMock.getId()).thenReturn("ses_default");

        try (var mocked = mockStatic(Session.class)) {
            mocked.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenReturn(sessionMock);

            PaymentUrlResponse result =
                    paymentService.generarPagoSuscripcion(usuario, null, "mensual");

            assertThat(result.paymentUrl()).isEqualTo("http://default.url");
        }
    }

    // ── reembolsarPago ──────────────────────────────────────────────

    @Test
    void reembolsarPagoShouldReturnEarlyWhenPaymentIntentIdNull() throws StripeException {
        paymentService.reembolsarPago(null, buildUsuario(1L), null, BigDecimal.TEN);

        verify(transaccionRepository, never()).save(any());
    }

    @Test
    void reembolsarPagoShouldReturnEarlyWhenPaymentIntentIdBlank() throws StripeException {
        paymentService.reembolsarPago("  ", buildUsuario(1L), null, BigDecimal.TEN);

        verify(transaccionRepository, never()).save(any());
    }

    @Test
    void reembolsarPagoShouldCreateRefundAndMarkTransactionAsReembolsada() throws StripeException {
        Usuario usuario = buildUsuario(1L);
        Tutor tutor = new Tutor();
        tutor.setId(10L);

        Refund refundMock = org.mockito.Mockito.mock(Refund.class);
        when(refundMock.getId()).thenReturn("re_test");

        TransaccionPago tx = buildTransaccion(100L, 1L);
        when(transaccionRepository
                        .findTopByUsuarioIdAndTutorIdAndTipoAndEstadoOrderByCompletadoAtDesc(
                                1L, 10L, TipoTransaccion.PAGO_TUTOR, EstadoTransaccion.COMPLETADA))
                .thenReturn(Optional.of(tx));

        try (var mocked = mockStatic(Refund.class)) {
            mocked.when(() -> Refund.create(any(RefundCreateParams.class))).thenReturn(refundMock);

            paymentService.reembolsarPago("pi_test", usuario, tutor, BigDecimal.TEN);

            assertThat(tx.getEstado()).isEqualTo(EstadoTransaccion.REEMBOLSADA);
            verify(transaccionRepository).save(tx);
        }
    }

    // ── cuentaConectadaActiva ───────────────────────────────────────

    @Test
    void cuentaConectadaActivaShouldReturnTrueWhenBothEnabled() throws StripeException {
        Account accountMock = org.mockito.Mockito.mock(Account.class);
        when(accountMock.getChargesEnabled()).thenReturn(true);
        when(accountMock.getPayoutsEnabled()).thenReturn(true);

        try (var mocked = mockStatic(Account.class)) {
            mocked.when(() -> Account.retrieve("acct_123")).thenReturn(accountMock);

            boolean result = paymentService.cuentaConectadaActiva("acct_123");

            assertThat(result).isTrue();
        }
    }

    @Test
    void cuentaConectadaActivaShouldReturnFalseWhenChargesDisabled() throws StripeException {
        Account accountMock = org.mockito.Mockito.mock(Account.class);
        when(accountMock.getChargesEnabled()).thenReturn(false);

        try (var mocked = mockStatic(Account.class)) {
            mocked.when(() -> Account.retrieve("acct_123")).thenReturn(accountMock);

            boolean result = paymentService.cuentaConectadaActiva("acct_123");

            assertThat(result).isFalse();
        }
    }

    // ── crearPaymentIntentContratacionTutor ─────────────────────────

    @Test
    void crearPaymentIntentContratacionTutorShouldReturnClientSecretAndId() throws StripeException {
        Tutor tutor = new Tutor();
        tutor.setId(1L);
        tutor.setStripeAccountId(null);
        when(tutorRepository.findById(1L)).thenReturn(Optional.of(tutor));

        PaymentIntent piMock = org.mockito.Mockito.mock(PaymentIntent.class);
        when(piMock.getClientSecret()).thenReturn("secret_abc");
        when(piMock.getId()).thenReturn("pi_abc");

        try (var mocked = mockStatic(PaymentIntent.class)) {
            mocked.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                    .thenReturn(piMock);

            Map<String, String> result =
                    paymentService.crearPaymentIntentContratacionTutor(
                            1L, 5L, new BigDecimal("50.00"), 10L, "test@test.com");

            assertThat(result).containsEntry("clientSecret", "secret_abc");
            assertThat(result).containsEntry("paymentIntentId", "pi_abc");
        }
    }

    @Test
    void crearPaymentIntentContratacionTutorShouldRouteToStripeConnectedAccount()
            throws StripeException {
        Tutor tutor = new Tutor();
        tutor.setId(1L);
        tutor.setStripeAccountId("acct_tutor_123");
        when(tutorRepository.findById(1L)).thenReturn(Optional.of(tutor));

        PaymentIntent piMock = org.mockito.Mockito.mock(PaymentIntent.class);
        when(piMock.getClientSecret()).thenReturn("secret_connected");
        when(piMock.getId()).thenReturn("pi_connected");

        try (var mocked = mockStatic(PaymentIntent.class)) {
            mocked.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                    .thenReturn(piMock);

            Map<String, String> result =
                    paymentService.crearPaymentIntentContratacionTutor(
                            1L, 2L, new BigDecimal("100.00"), 10L, "user@email.com");

            assertThat(result).containsEntry("clientSecret", "secret_connected");
        }
    }

    @Test
    void crearPaymentIntentContratacionTutorShouldHandleNullComunidadAndEmail()
            throws StripeException {
        when(tutorRepository.findById(1L)).thenReturn(Optional.empty());

        PaymentIntent piMock = org.mockito.Mockito.mock(PaymentIntent.class);
        when(piMock.getClientSecret()).thenReturn("secret_min");
        when(piMock.getId()).thenReturn("pi_min");

        try (var mocked = mockStatic(PaymentIntent.class)) {
            mocked.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                    .thenReturn(piMock);

            Map<String, String> result =
                    paymentService.crearPaymentIntentContratacionTutor(
                            1L, null, new BigDecimal("30.00"), 10L, null);

            assertThat(result).containsEntry("clientSecret", "secret_min");
        }
    }

    @Test
    void crearPaymentIntentContratacionTutorShouldSkipConnectedWhenBlankAccountId()
            throws StripeException {
        Tutor tutor = new Tutor();
        tutor.setId(1L);
        tutor.setStripeAccountId("  ");
        when(tutorRepository.findById(1L)).thenReturn(Optional.of(tutor));

        PaymentIntent piMock = org.mockito.Mockito.mock(PaymentIntent.class);
        when(piMock.getClientSecret()).thenReturn("secret_blank");
        when(piMock.getId()).thenReturn("pi_blank");

        try (var mocked = mockStatic(PaymentIntent.class)) {
            mocked.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                    .thenReturn(piMock);

            Map<String, String> result =
                    paymentService.crearPaymentIntentContratacionTutor(
                            1L, 5L, new BigDecimal("25.00"), 10L, "test@test.com");

            assertThat(result).containsEntry("clientSecret", "secret_blank");
        }
    }

    // ── generarPagoPlanCorporativo ──────────────────────────────────

    @Test
    void generarPagoPlanCorporativoShouldCreateBasicoMensualSession() throws StripeException {
        Session sessionMock = org.mockito.Mockito.mock(Session.class);
        when(sessionMock.getUrl()).thenReturn("http://corp.url");
        when(sessionMock.getId()).thenReturn("ses_corp");

        try (var mocked = mockStatic(Session.class)) {
            mocked.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenReturn(sessionMock);

            PaymentUrlResponse result =
                    paymentService.generarPagoPlanCorporativo(
                            1L,
                            TipoPlanCorporativo.BASICO,
                            new BigDecimal("10.00"),
                            "mensual",
                            "admin@corp.com");

            assertThat(result.paymentUrl()).isEqualTo("http://corp.url");
            assertThat(result.sessionId()).isEqualTo("ses_corp");
        }
    }

    @Test
    void generarPagoPlanCorporativoShouldCreateEstandarAnualSession() throws StripeException {
        Session sessionMock = org.mockito.Mockito.mock(Session.class);
        when(sessionMock.getUrl()).thenReturn("http://estandar-anual.url");
        when(sessionMock.getId()).thenReturn("ses_est_anual");

        try (var mocked = mockStatic(Session.class)) {
            mocked.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenReturn(sessionMock);

            PaymentUrlResponse result =
                    paymentService.generarPagoPlanCorporativo(
                            2L,
                            TipoPlanCorporativo.ESTANDAR,
                            new BigDecimal("200.00"),
                            "anual",
                            null);

            assertThat(result.paymentUrl()).isEqualTo("http://estandar-anual.url");
        }
    }

    @Test
    void generarPagoPlanCorporativoShouldCreatePremiumAnualByAmountSession()
            throws StripeException {
        Session sessionMock = org.mockito.Mockito.mock(Session.class);
        when(sessionMock.getUrl()).thenReturn("http://premium-amt.url");
        when(sessionMock.getId()).thenReturn("ses_premium_amt");

        try (var mocked = mockStatic(Session.class)) {
            mocked.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenReturn(sessionMock);

            // monto > 100 triggers esAnual even with "mensual" periodo
            PaymentUrlResponse result =
                    paymentService.generarPagoPlanCorporativo(
                            3L,
                            TipoPlanCorporativo.PREMIUM,
                            new BigDecimal("150.00"),
                            "mensual",
                            "contact@corp.com");

            assertThat(result.paymentUrl()).isEqualTo("http://premium-amt.url");
        }
    }

    // ── crearPaymentIntentSuscripcion ───────────────────────────────

    @Test
    void crearPaymentIntentSuscripcionShouldReturnMapForPremiumMensual() throws StripeException {
        Usuario usuario = buildUsuario(1L);

        PaymentIntent piMock = org.mockito.Mockito.mock(PaymentIntent.class);
        when(piMock.getClientSecret()).thenReturn("secret_pm");
        when(piMock.getId()).thenReturn("pi_pm");

        try (var mocked = mockStatic(PaymentIntent.class)) {
            mocked.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                    .thenReturn(piMock);

            Map<String, String> result =
                    paymentService.crearPaymentIntentSuscripcion(
                            usuario, TipoPlan.PREMIUM, "mensual");

            assertThat(result).containsEntry("clientSecret", "secret_pm");
            assertThat(result).containsEntry("paymentIntentId", "pi_pm");
        }
    }

    @Test
    void crearPaymentIntentSuscripcionShouldReturnMapForProAnual() throws StripeException {
        Usuario usuario = buildUsuario(1L);

        PaymentIntent piMock = org.mockito.Mockito.mock(PaymentIntent.class);
        when(piMock.getClientSecret()).thenReturn("secret_pa");
        when(piMock.getId()).thenReturn("pi_pa");

        try (var mocked = mockStatic(PaymentIntent.class)) {
            mocked.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                    .thenReturn(piMock);

            Map<String, String> result =
                    paymentService.crearPaymentIntentSuscripcion(usuario, TipoPlan.PRO, "anual");

            assertThat(result).containsEntry("clientSecret", "secret_pa");
        }
    }

    @Test
    void crearPaymentIntentSuscripcionShouldDefaultToPremuimWhenFree() throws StripeException {
        Usuario usuario = buildUsuario(1L);

        PaymentIntent piMock = org.mockito.Mockito.mock(PaymentIntent.class);
        when(piMock.getClientSecret()).thenReturn("secret_def");
        when(piMock.getId()).thenReturn("pi_def");

        try (var mocked = mockStatic(PaymentIntent.class)) {
            mocked.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                    .thenReturn(piMock);

            Map<String, String> result =
                    paymentService.crearPaymentIntentSuscripcion(usuario, TipoPlan.FREE, "mensual");

            assertThat(result).containsEntry("clientSecret", "secret_def");
        }
    }

    // ── crearPaymentIntentVerificacion ──────────────────────────────

    @Test
    void crearPaymentIntentVerificacionShouldReturnMapWithSecretAndId() throws StripeException {
        PaymentIntent piMock = org.mockito.Mockito.mock(PaymentIntent.class);
        when(piMock.getClientSecret()).thenReturn("secret_ver");
        when(piMock.getId()).thenReturn("pi_ver");

        try (var mocked = mockStatic(PaymentIntent.class)) {
            mocked.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                    .thenReturn(piMock);

            Map<String, String> result =
                    paymentService.crearPaymentIntentVerificacion(5L, 10L, "tutor@test.com");

            assertThat(result).containsEntry("clientSecret", "secret_ver");
            assertThat(result).containsEntry("paymentIntentId", "pi_ver");
        }
    }

    // ── crearCuentaConectadaTutor ───────────────────────────────────

    @Test
    void crearCuentaConectadaTutorShouldReturnExistingAccountId() throws StripeException {
        Tutor tutor = new Tutor();
        tutor.setStripeAccountId("acct_existing");
        Usuario usuario = buildUsuario(1L);
        tutor.setUsuario(usuario);

        String result = paymentService.crearCuentaConectadaTutor(tutor);

        assertThat(result).isEqualTo("acct_existing");
        verify(tutorRepository, never()).save(any());
    }

    @Test
    void crearCuentaConectadaTutorShouldCreateNewAccountWhenNone() throws StripeException {
        Tutor tutor = new Tutor();
        tutor.setStripeAccountId(null);
        Usuario usuario = buildUsuario(1L);
        tutor.setUsuario(usuario);

        Account accountMock = org.mockito.Mockito.mock(Account.class);
        when(accountMock.getId()).thenReturn("acct_new");

        try (var mocked = mockStatic(Account.class)) {
            mocked.when(() -> Account.create(any(AccountCreateParams.class)))
                    .thenReturn(accountMock);

            String result = paymentService.crearCuentaConectadaTutor(tutor);

            assertThat(result).isEqualTo("acct_new");
            assertThat(tutor.getStripeAccountId()).isEqualTo("acct_new");
            verify(tutorRepository).save(tutor);
        }
    }
}
