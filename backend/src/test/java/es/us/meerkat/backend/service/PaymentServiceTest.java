package es.us.meerkat.backend.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import es.us.meerkat.backend.repository.TransaccionPagoRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private TransaccionPagoRepository transaccionRepository;

    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        // Configurar valores por defecto si es necesario
    }

    @Test
    void paymentServiceShouldBeInstantiated() {
        assertThat(paymentService).isNotNull();
    }

    @Test
    void transaccionRepositoryShouldBeMocked() {
        assertThat(transaccionRepository).isNotNull();
    }
}
