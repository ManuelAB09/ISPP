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
import org.springframework.data.domain.PageImpl;
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
