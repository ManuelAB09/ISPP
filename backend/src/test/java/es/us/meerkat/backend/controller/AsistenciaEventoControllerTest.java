package es.us.meerkat.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import es.us.meerkat.backend.entity.AsistenciaEvento;
import es.us.meerkat.backend.entity.EstadoAsistencia;
import es.us.meerkat.backend.entity.Evento;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.service.AsistenciaEventoService;

@ExtendWith(MockitoExtension.class)
class AsistenciaEventoControllerTest {

    @Mock private AsistenciaEventoService asistenciaEventoService;

    @InjectMocks private AsistenciaEventoController controller;

    private Usuario buildUsuario(Long id) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre("Test User");
        return u;
    }

    @Test
    void confirmarAsistenciaShouldReturnCreatedOnSuccess() {
        Usuario usuario = buildUsuario(1L);
        Evento evento = new Evento();
        evento.setId(10L);
        AsistenciaEvento asistencia = new AsistenciaEvento();
        asistencia.setId(100L);
        asistencia.setEvento(evento);
        asistencia.setUsuario(usuario);
        asistencia.setEstado(EstadoAsistencia.CONFIRMADA);

        when(asistenciaEventoService.confirmarAsistencia(10L, 1L)).thenReturn(asistencia);

        ResponseEntity<?> response = controller.confirmarAsistencia(10L, 1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void cancelarAsistenciaShouldReturnNoContent() {
        ResponseEntity<?> response = controller.cancelarAsistenciaPropia(10L, 1L);

        verify(asistenciaEventoService).cancelarAsistencia(10L, 1L);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void contarAsistentesShouldReturnCount() {
        when(asistenciaEventoService.contarAsistentesConfirmados(10L)).thenReturn(5L);

        ResponseEntity<Long> response = controller.contarAsistentes(10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(5L);
    }
}
