package es.us.meerkat.backend.controller.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import es.us.meerkat.backend.dto.events.AttendanceResponse;
import es.us.meerkat.backend.entity.events.AsistenciaEvento;
import es.us.meerkat.backend.entity.events.EstadoAsistencia;
import es.us.meerkat.backend.entity.events.Evento;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.service.events.AsistenciaEventoService;

/**
 * Comprehensive test suite for AsistenciaEventoController.
 *
 * <p>Tests cover event attendance confirmation, cancellation, and listing. Includes 25+ test cases
 * covering happy paths, error handling, edge cases, and conflict scenarios.
 */
@ExtendWith(MockitoExtension.class)
class AsistenciaEventoControllerTest {

    @Mock private AsistenciaEventoService asistenciaEventoService;

    @InjectMocks private AsistenciaEventoController controller;

    private Usuario usuario;
    private Evento evento;
    private AsistenciaEvento asistencia;
    private Long eventId;
    private Long usuarioId;

    @BeforeEach
    void setUp() {
        eventId = 100L;
        usuarioId = 1L;

        usuario = buildUsuario(usuarioId, "user@test.es");
        evento = buildEvento(eventId);
        asistencia = buildAsistenciaEvento(1L, usuario, evento, EstadoAsistencia.CONFIRMADA);
    }

    // ===============================
    // HELPER METHODS
    // ===============================

    private Usuario buildUsuario(Long id, String email) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setEmail(email);
        return u;
    }

    private Evento buildEvento(Long id) {
        Evento e = new Evento();
        e.setId(id);
        e.setTitulo("Evento Test");
        e.setFechaHora(LocalDateTime.now().plusDays(1));
        e.setFechaFin(LocalDateTime.now().plusDays(1).plusHours(2));
        e.setAforo(30);
        e.setAsistentesConfirmados(0);
        return e;
    }

    private AsistenciaEvento buildAsistenciaEvento(
            Long id, Usuario usuario, Evento evento, EstadoAsistencia estado) {
        AsistenciaEvento a = new AsistenciaEvento();
        a.setId(id);
        a.setUsuario(usuario);
        a.setEvento(evento);
        a.setEstado(estado);
        return a;
    }

    private AttendanceResponse buildAttendanceResponse(
            Long id, Usuario usuario, EstadoAsistencia estado) {
        AttendanceResponse response = new AttendanceResponse();
        response.setId(id);
        response.setEstado(estado);
        return response;
    }

    // ===============================
    // CONFIRM ATTENDANCE TESTS
    // ===============================

    @Test
    void confirmarAsistenciaShouldReturnCreatedWhenSuccessful() {
        when(asistenciaEventoService.confirmarAsistencia(eventId, usuarioId))
                .thenReturn(asistencia);

        ResponseEntity<AttendanceResponse> response =
                controller.confirmarAsistencia(eventId, usuarioId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        verify(asistenciaEventoService).confirmarAsistencia(eventId, usuarioId);
    }

    @Test
    void confirmarAsistenciaShouldReturnConflictWhenAlreadyConfirmed() {
        when(asistenciaEventoService.confirmarAsistencia(eventId, usuarioId))
                .thenThrow(new IllegalStateException("Ya confirmaste tu asistencia"));

        ResponseEntity<AttendanceResponse> response =
                controller.confirmarAsistencia(eventId, usuarioId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void confirmarAsistenciaShouldReturnConflictWhenEventFull() {
        when(asistenciaEventoService.confirmarAsistencia(eventId, usuarioId))
                .thenThrow(new IllegalStateException("El evento está lleno"));

        ResponseEntity<AttendanceResponse> response =
                controller.confirmarAsistencia(eventId, usuarioId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void confirmarAsistenciaShouldReturnConflictWhenEventCanceled() {
        when(asistenciaEventoService.confirmarAsistencia(eventId, usuarioId))
                .thenThrow(new IllegalStateException("El evento ha sido cancelado"));

        ResponseEntity<AttendanceResponse> response =
                controller.confirmarAsistencia(eventId, usuarioId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    // ===============================
    // GET MY ATTENDANCE TESTS
    // ===============================

    @Test
    void obtenerAsistenciaPropiaShouldReturnAttendanceWhenFound() {
        when(asistenciaEventoService.obtenerAsistencia(eventId, usuarioId)).thenReturn(asistencia);

        ResponseEntity<AttendanceResponse> response =
                controller.obtenerAsistenciaPropia(eventId, usuarioId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        verify(asistenciaEventoService).obtenerAsistencia(eventId, usuarioId);
    }

    @Test
    void obtenerAsistenciaPropiaShouldReturnNoContentWhenNotFound() {
        when(asistenciaEventoService.obtenerAsistencia(eventId, usuarioId))
                .thenThrow(new RuntimeException("Asistencia no encontrada"));

        ResponseEntity<AttendanceResponse> response =
                controller.obtenerAsistenciaPropia(eventId, usuarioId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void obtenerAsistenciaPropiaShouldReturnBadRequestOnOtherError() {
        when(asistenciaEventoService.obtenerAsistencia(eventId, usuarioId))
                .thenThrow(new RuntimeException("Error de base de datos"));

        ResponseEntity<AttendanceResponse> response =
                controller.obtenerAsistenciaPropia(eventId, usuarioId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void obtenerAsistenciaPropiaShouldReturnConfirmedState() {
        AsistenciaEvento confirmada =
                buildAsistenciaEvento(1L, usuario, evento, EstadoAsistencia.CONFIRMADA);
        when(asistenciaEventoService.obtenerAsistencia(eventId, usuarioId)).thenReturn(confirmada);

        ResponseEntity<AttendanceResponse> response =
                controller.obtenerAsistenciaPropia(eventId, usuarioId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ===============================
    // CANCEL ATTENDANCE TESTS
    // ===============================

    @Test
    void cancelarAsistenciaPropiaShouldReturnNoContentWhenSuccessful() {
        doNothing().when(asistenciaEventoService).cancelarAsistencia(eventId, usuarioId);

        ResponseEntity<Void> response = controller.cancelarAsistenciaPropia(eventId, usuarioId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(asistenciaEventoService).cancelarAsistencia(eventId, usuarioId);
    }

    @Test
    void cancelarAsistenciaPropiaShouldReturnForbiddenWhenEventStarted() {
        doThrow(new IllegalStateException("No puedes cancelar después de empezar"))
                .when(asistenciaEventoService)
                .cancelarAsistencia(eventId, usuarioId);

        ResponseEntity<Void> response = controller.cancelarAsistenciaPropia(eventId, usuarioId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void cancelarAsistenciaPropiaShouldReturnNoContentWhenNotFound() {
        doThrow(new RuntimeException("Asistencia no encontrada"))
                .when(asistenciaEventoService)
                .cancelarAsistencia(eventId, usuarioId);

        ResponseEntity<Void> response = controller.cancelarAsistenciaPropia(eventId, usuarioId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void cancelarAsistenciaPropiaShouldReturnBadRequestOnOtherError() {
        doThrow(new RuntimeException("Error de base de datos"))
                .when(asistenciaEventoService)
                .cancelarAsistencia(eventId, usuarioId);

        ResponseEntity<Void> response = controller.cancelarAsistenciaPropia(eventId, usuarioId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ===============================
    // LIST ALL ATTENDANCES TESTS
    // ===============================

    @Test
    void obtenerAsistenciasEventoShouldReturnAllAttendances() {
        AsistenciaEvento asistencia2 =
                buildAsistenciaEvento(
                        2L, buildUsuario(2L, "otro@test.es"), evento, EstadoAsistencia.CONFIRMADA);
        when(asistenciaEventoService.obtenerAsistenciasEvento(eventId))
                .thenReturn(List.of(asistencia, asistencia2));

        ResponseEntity<List<AttendanceResponse>> response =
                controller.obtenerAsistenciasEvento(eventId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        verify(asistenciaEventoService).obtenerAsistenciasEvento(eventId);
    }

    @Test
    void obtenerAsistenciasEventoShouldReturnEmptyListWhenNoAttendances() {
        when(asistenciaEventoService.obtenerAsistenciasEvento(eventId)).thenReturn(List.of());

        ResponseEntity<List<AttendanceResponse>> response =
                controller.obtenerAsistenciasEvento(eventId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void obtenerAsistenciasEventoShouldReturnMultipleAttendances() {
        List<AsistenciaEvento> asistencias =
                List.of(
                        buildAsistenciaEvento(
                                1L,
                                buildUsuario(1L, "user1@test.es"),
                                evento,
                                EstadoAsistencia.CONFIRMADA),
                        buildAsistenciaEvento(
                                2L,
                                buildUsuario(2L, "user2@test.es"),
                                evento,
                                EstadoAsistencia.CONFIRMADA),
                        buildAsistenciaEvento(
                                3L,
                                buildUsuario(3L, "user3@test.es"),
                                evento,
                                EstadoAsistencia.CONFIRMADA));
        when(asistenciaEventoService.obtenerAsistenciasEvento(eventId)).thenReturn(asistencias);

        ResponseEntity<List<AttendanceResponse>> response =
                controller.obtenerAsistenciasEvento(eventId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(3);
    }

    // ===============================
    // LIST CONFIRMED ATTENDANCES TESTS
    // ===============================

    @Test
    void obtenerAsistentesConfirmadosShouldReturnConfirmedOnly() {
        AsistenciaEvento confirmada =
                buildAsistenciaEvento(1L, usuario, evento, EstadoAsistencia.CONFIRMADA);
        when(asistenciaEventoService.obtenerAsistentesConfirmados(eventId))
                .thenReturn(List.of(confirmada));

        ResponseEntity<List<AttendanceResponse>> response =
                controller.obtenerAsistentesConfirmados(eventId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        verify(asistenciaEventoService).obtenerAsistentesConfirmados(eventId);
    }

    @Test
    void obtenerAsistentesConfirmadosShouldReturnEmptyListWhenNoneConfirmed() {
        when(asistenciaEventoService.obtenerAsistentesConfirmados(eventId)).thenReturn(List.of());

        ResponseEntity<List<AttendanceResponse>> response =
                controller.obtenerAsistentesConfirmados(eventId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void obtenerAsistentesConfirmadosShouldReturnMultipleConfirmed() {
        List<AsistenciaEvento> confirmados =
                List.of(
                        buildAsistenciaEvento(
                                1L,
                                buildUsuario(1L, "user1@test.es"),
                                evento,
                                EstadoAsistencia.CONFIRMADA),
                        buildAsistenciaEvento(
                                2L,
                                buildUsuario(2L, "user2@test.es"),
                                evento,
                                EstadoAsistencia.CONFIRMADA));
        when(asistenciaEventoService.obtenerAsistentesConfirmados(eventId)).thenReturn(confirmados);

        ResponseEntity<List<AttendanceResponse>> response =
                controller.obtenerAsistentesConfirmados(eventId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
    }

    // ===============================
    // COUNT ATTENDEES TESTS
    // ===============================

    @Test
    void contarAsistentesShouldReturnCount() {
        when(asistenciaEventoService.contarAsistentesConfirmados(eventId)).thenReturn(5L);

        ResponseEntity<Long> response = controller.contarAsistentes(eventId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(5L);
        verify(asistenciaEventoService).contarAsistentesConfirmados(eventId);
    }

    @Test
    void contarAsistentesShouldReturnZeroWhenNoneConfirmed() {
        when(asistenciaEventoService.contarAsistentesConfirmados(eventId)).thenReturn(0L);

        ResponseEntity<Long> response = controller.contarAsistentes(eventId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(0L);
    }

    @Test
    void contarAsistentesShouldReturnFullCapacity() {
        when(asistenciaEventoService.contarAsistentesConfirmados(eventId)).thenReturn(30L);

        ResponseEntity<Long> response = controller.contarAsistentes(eventId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(30L);
    }

    @Test
    void contarAsistentesShouldReturnHighNumber() {
        when(asistenciaEventoService.contarAsistentesConfirmados(eventId)).thenReturn(999L);

        ResponseEntity<Long> response = controller.contarAsistentes(eventId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(999L);
    }

    // ===============================
    // EDGE CASES
    // ===============================

    @Test
    void confirmarAsistenciaShouldHandleNullUsuarioId() {
        // When usuario is null it should just return CONFLICT
        when(asistenciaEventoService.confirmarAsistencia(eventId, null))
                .thenThrow(new IllegalStateException("Invalid user"));

        ResponseEntity<AttendanceResponse> response = controller.confirmarAsistencia(eventId, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void confirmarAsistenciaShouldHandleNullEventoId() {
        when(asistenciaEventoService.confirmarAsistencia(null, usuarioId))
                .thenThrow(new IllegalStateException("Invalid event"));

        ResponseEntity<AttendanceResponse> response =
                controller.confirmarAsistencia(null, usuarioId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void obtenerAsistenciasEventoShouldHandleNullEventoId() {
        when(asistenciaEventoService.obtenerAsistenciasEvento(null)).thenReturn(List.of());

        ResponseEntity<List<AttendanceResponse>> response =
                controller.obtenerAsistenciasEvento(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void contarAsistentesShouldHandleNullEventoId() {
        when(asistenciaEventoService.contarAsistentesConfirmados(null)).thenReturn(0L);

        ResponseEntity<Long> response = controller.contarAsistentes(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(0L);
    }
}
