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

    @Test
    void getPaymentHistoryShouldSupportPagination() {
        Usuario usuario = buildUsuario(1L);
        TransaccionPago t1 = buildTransaccion(1L);
        TransaccionPago t2 = buildTransaccion(2L);
        TransaccionPago t3 = buildTransaccion(3L);

        Page<TransaccionPago> page2 =
                new PageImpl<>(java.util.List.of(t3), PageRequest.of(1, 2), 3);

        when(paymentService.obtenerHistorialPagos(1L, PageRequest.of(1, 2))).thenReturn(page2);

        ResponseEntity<TransactionListResponse> response =
                controller.getPaymentHistory(usuario, null, null, null, 1, 2);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getPage().getNumber()).isEqualTo(1);
        assertThat(response.getBody().getPage().getSize()).isEqualTo(2);
        assertThat(response.getBody().getPage().getTotalElements()).isEqualTo(3);
    }

    @Test
    void getPaymentHistoryShouldReturnFirstPage() {
        Usuario usuario = buildUsuario(1L);
        TransaccionPago transaccion = buildTransaccion(1L);
        Page<TransaccionPago> firstPage =
                new PageImpl<>(java.util.List.of(transaccion), PageRequest.of(0, 20), 50);

        when(paymentService.obtenerHistorialPagos(1L, PageRequest.of(0, 20))).thenReturn(firstPage);

        ResponseEntity<TransactionListResponse> response =
                controller.getPaymentHistory(usuario, null, null, null, 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getPage().getNumber()).isEqualTo(0);
        assertThat(response.getBody().getPage().getTotalPages()).isEqualTo(3);
    }

    @Test
    void getPaymentHistoryShouldReturnLastPage() {
        Usuario usuario = buildUsuario(1L);
        TransaccionPago transaccion = buildTransaccion(1L);
        Page<TransaccionPago> lastPage =
                new PageImpl<>(java.util.List.of(transaccion), PageRequest.of(2, 20), 50);

        when(paymentService.obtenerHistorialPagos(1L, PageRequest.of(2, 20))).thenReturn(lastPage);

        ResponseEntity<TransactionListResponse> response =
                controller.getPaymentHistory(usuario, null, null, null, 2, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getPage().getNumber()).isEqualTo(2);
        assertThat(response.getBody().getPage().getTotalPages()).isEqualTo(3);
    }

    @Test
    void getTransactionShouldHandleMultipleQueriesForDifferentUsers() {
        Usuario usuario1 = buildUsuario(1L);
        Usuario usuario2 = buildUsuario(2L);
        TransaccionPago tx1 = buildTransaccion(100L);
        TransaccionPago tx2 = buildTransaccion(200L);

        when(paymentService.obtenerTransaccion(100L, 1L)).thenReturn(Optional.of(tx1));
        when(paymentService.obtenerTransaccion(200L, 2L)).thenReturn(Optional.of(tx2));

        ResponseEntity<TransactionResponse> response1 = controller.getTransaction(100L, usuario1);
        ResponseEntity<TransactionResponse> response2 = controller.getTransaction(200L, usuario2);

        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getPaymentHistoryShouldHandleLargeAmounts() {
        Usuario usuario = buildUsuario(1L);
        TransaccionPago largeTransaction = new TransaccionPago();
        largeTransaction.setId(1L);
        largeTransaction.setMonto(java.math.BigDecimal.valueOf(99999.99));
        largeTransaction.setComision(java.math.BigDecimal.ZERO);

        Page<TransaccionPago> page =
                new PageImpl<>(java.util.List.of(largeTransaction), PageRequest.of(0, 20), 1);

        when(paymentService.obtenerHistorialPagos(1L, PageRequest.of(0, 20))).thenReturn(page);

        ResponseEntity<TransactionListResponse> response =
                controller.getPaymentHistory(usuario, null, null, null, 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getContent()).hasSize(1);
    }

    @Test
    void getPaymentHistoryShouldConsiderFilterParameters() {
        Usuario usuario = buildUsuario(1L);
        TransaccionPago transaccion = buildTransaccion(1L);
        Page<TransaccionPago> page =
                new PageImpl<>(java.util.List.of(transaccion), PageRequest.of(0, 20), 1);

        when(paymentService.obtenerHistorialPagos(1L, PageRequest.of(0, 20))).thenReturn(page);

        java.time.LocalDate desde = java.time.LocalDate.now().minusMonths(1);
        java.time.LocalDate hasta = java.time.LocalDate.now();

        ResponseEntity<TransactionListResponse> response =
                controller.getPaymentHistory(usuario, "PAGO", desde, hasta, 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getContent()).hasSize(1);
    }

    @Test
    void getPaymentHistoryShouldHandleCustomPageSize() {
        Usuario usuario = buildUsuario(1L);
        TransaccionPago t1 = buildTransaccion(1L);
        TransaccionPago t2 = buildTransaccion(2L);
        TransaccionPago t3 = buildTransaccion(3L);
        TransaccionPago t4 = buildTransaccion(4L);
        TransaccionPago t5 = buildTransaccion(5L);

        Page<TransaccionPago> page =
                new PageImpl<>(java.util.List.of(t1, t2, t3, t4, t5), PageRequest.of(0, 5), 25);

        when(paymentService.obtenerHistorialPagos(1L, PageRequest.of(0, 5))).thenReturn(page);

        ResponseEntity<TransactionListResponse> response =
                controller.getPaymentHistory(usuario, null, null, null, 0, 5);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getContent()).hasSize(5);
        assertThat(response.getBody().getPage().getSize()).isEqualTo(5);
    }

    @Test
    void getTransactionShouldReturnResponseWithCorrectStructure() {
        Usuario usuario = buildUsuario(1L);
        TransaccionPago tx = new TransaccionPago();
        tx.setId(123L);
        tx.setMonto(java.math.BigDecimal.valueOf(50.00));
        tx.setComision(java.math.BigDecimal.valueOf(2.50));

        when(paymentService.obtenerTransaccion(123L, 1L)).thenReturn(Optional.of(tx));

        ResponseEntity<TransactionResponse> response = controller.getTransaction(123L, usuario);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(123L);
        assertThat(response.getBody().getMonto()).isEqualTo(java.math.BigDecimal.valueOf(50.00));
    }
}
