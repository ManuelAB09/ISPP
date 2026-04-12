package es.us.meerkat.backend.controller.subscriptions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import es.us.meerkat.backend.dto.subscriptions.TransactionListResponse;
import es.us.meerkat.backend.dto.subscriptions.TransactionResponse;
import es.us.meerkat.backend.entity.subscriptions.TransaccionPago;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.service.communities.InstitutionService;
import es.us.meerkat.backend.service.subscriptions.PaymentService;
import es.us.meerkat.backend.service.subscriptions.SuscripcionService;
import es.us.meerkat.backend.service.tutors.TutorService;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock private PaymentService paymentService;
    @Mock private SuscripcionService suscripcionService;
    @Mock private InstitutionService institutionService;
    @Mock private TutorService tutorService;

    @InjectMocks private PaymentController controller;

    private Usuario buildUsuario(Long id) {
        Usuario u = new Usuario();
        u.setId(id);
        return u;
    }

    private TransaccionPago buildTransaccion(Long id) {
        TransaccionPago t = new TransaccionPago();
        t.setId(id);
        t.setMonto(java.math.BigDecimal.ZERO);
        return t;
    }

    @Test
    void getPaymentHistoryShouldReturnOk() {
        Usuario usuario = buildUsuario(1L);
        TransaccionPago transaccion = buildTransaccion(1L);
        Page<TransaccionPago> page =
                new PageImpl<>(java.util.List.of(transaccion), PageRequest.of(0, 20), 1);

        when(paymentService.obtenerHistorialPagos(1L, PageRequest.of(0, 20))).thenReturn(page);

        ResponseEntity<TransactionListResponse> response =
                controller.getPaymentHistory(usuario, null, null, null, 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
    }

    @Test
    void getPaymentHistoryShouldReturnEmptyList() {
        Usuario usuario = buildUsuario(1L);
        Page<TransaccionPago> emptyPage =
                new PageImpl<>(java.util.List.of(), PageRequest.of(0, 20), 0);

        when(paymentService.obtenerHistorialPagos(1L, PageRequest.of(0, 20))).thenReturn(emptyPage);

        ResponseEntity<TransactionListResponse> response =
                controller.getPaymentHistory(usuario, null, null, null, 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getContent()).isEmpty();
    }

    @Test
    void getTransactionShouldReturnOkWhenExists() {
        Usuario usuario = buildUsuario(1L);
        TransaccionPago transaccion = buildTransaccion(1L);

        when(paymentService.obtenerTransaccion(1L, 1L)).thenReturn(Optional.of(transaccion));

        ResponseEntity<TransactionResponse> response = controller.getTransaction(1L, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getTransactionShouldReturnNotFoundWhenDoesNotExist() {
        Usuario usuario = buildUsuario(1L);

        when(paymentService.obtenerTransaccion(1L, 1L)).thenReturn(Optional.empty());

        ResponseEntity<TransactionResponse> response = controller.getTransaction(1L, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getPaymentHistoryShouldHandleMultiplePages() {
        Usuario usuario = buildUsuario(1L);
        TransaccionPago t1 = buildTransaccion(1L);
        TransaccionPago t2 = buildTransaccion(2L);
        Page<TransaccionPago> page =
                new PageImpl<>(java.util.List.of(t1, t2), PageRequest.of(0, 20), 40);

        when(paymentService.obtenerHistorialPagos(1L, PageRequest.of(0, 20))).thenReturn(page);

        ResponseEntity<TransactionListResponse> response =
                controller.getPaymentHistory(usuario, null, null, null, 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getContent()).hasSize(2);
        assertThat(response.getBody().getPage().getTotalElements()).isEqualTo(40);
    }

    @Test
    void getPaymentHistoryShouldReturnOkWithResults() {
        Usuario usuario = buildUsuario(1L);
        when(paymentService.obtenerHistorialPagos(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of()));

        ResponseEntity<TransactionListResponse> response =
                controller.getPaymentHistory(usuario, null, null, null, 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void getTransactionShouldReturnOkWhenFound() {
        Usuario usuario = buildUsuario(1L);
        TransaccionPago tx = new TransaccionPago();
        tx.setId(99L);
        tx.setMonto(java.math.BigDecimal.TEN);
        tx.setComision(java.math.BigDecimal.ZERO);
        when(paymentService.obtenerTransaccion(99L, 1L)).thenReturn(Optional.of(tx));

        ResponseEntity<TransactionResponse> response = controller.getTransaction(99L, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getTransactionShouldReturnNotFoundWhenMissing() {
        Usuario usuario = buildUsuario(1L);
        when(paymentService.obtenerTransaccion(999L, 1L)).thenReturn(Optional.empty());

        ResponseEntity<TransactionResponse> response = controller.getTransaction(999L, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
