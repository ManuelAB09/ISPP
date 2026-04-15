package es.us.meerkat.backend.service.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import es.us.meerkat.backend.dto.events.MisEventosItemResponse;
import es.us.meerkat.backend.dto.notifications.AlertaEventoResponse;
import es.us.meerkat.backend.entity.communities.Comunidad;
import es.us.meerkat.backend.entity.events.AsistenciaEvento;
import es.us.meerkat.backend.entity.events.EstadoAsistencia;
import es.us.meerkat.backend.entity.events.Evento;
import es.us.meerkat.backend.entity.events.TipoEvento;
import es.us.meerkat.backend.entity.notifications.AlertaEvento;
import es.us.meerkat.backend.entity.notifications.TipoAlerta;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.events.AsistenciaEventoRepository;
import es.us.meerkat.backend.repository.events.EventoRepository;
import es.us.meerkat.backend.repository.notifications.AlertaEventoRepository;
import es.us.meerkat.backend.repository.users.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class MisEventosServiceTest {

    @Mock private EventoRepository eventoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AlertaEventoRepository alertaEventoRepository;
    @Mock private AsistenciaEventoRepository asistenciaEventoRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks private MisEventosService misEventosService;

    private Usuario buildUsuario(Long id) {
        return Usuario.builder()
                .id(id)
                .nombre("User " + id)
                .email("u" + id + "@t.com")
                .password("p")
                .build();
    }

    private Evento buildEvento(Long id, LocalDateTime fechaHora) {
        Evento e = new Evento();
        e.setId(id);
        e.setTitulo("Evento " + id);
        e.setFechaHora(fechaHora);
        e.setTipoEvento(TipoEvento.CLASE);
        e.setComunidad(Comunidad.builder().id(10L).nombre("MiCom").build());
        e.setCancelado(false);
        e.setEsVirtual(false);
        return e;
    }

    // ================================================================
    // obtenerMisEventos
    // ================================================================

    @Test
    void obtenerMisEventosShouldReturnMappedEvents() {
        Evento evento = buildEvento(1L, LocalDateTime.now().plusHours(3));

        when(usuarioRepository.existsById(1L)).thenReturn(true);
        when(eventoRepository.findProximosEventosByUsuarioId(eq(1L), any(), any()))
                .thenReturn(List.of(evento));
        when(asistenciaEventoRepository.findByEventoIdAndUsuarioId(1L, 1L))
                .thenReturn(Optional.empty());

        List<MisEventosItemResponse> result = misEventosService.obtenerMisEventos(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitulo()).isEqualTo("Evento 1");
        assertThat(result.get(0).getComunidadNombre()).isEqualTo("MiCom");
    }

    @Test
    void obtenerMisEventosShouldMarkEventAsInminente() {
        Evento evento = buildEvento(1L, LocalDateTime.now().plusMinutes(10));

        when(usuarioRepository.existsById(1L)).thenReturn(true);
        when(eventoRepository.findProximosEventosByUsuarioId(eq(1L), any(), any()))
                .thenReturn(List.of(evento));
        when(asistenciaEventoRepository.findByEventoIdAndUsuarioId(1L, 1L))
                .thenReturn(Optional.empty());

        List<MisEventosItemResponse> result = misEventosService.obtenerMisEventos(1L);

        assertThat(result.get(0).getInminenteEn15Min()).isTrue();
        assertThat(result.get(0).getProximaEn24H()).isTrue();
    }

    @Test
    void obtenerMisEventosShouldMarkAsistenciaConfirmada() {
        Evento evento = buildEvento(1L, LocalDateTime.now().plusHours(3));

        AsistenciaEvento asistencia = new AsistenciaEvento();
        asistencia.setEstado(EstadoAsistencia.CONFIRMADA);

        when(usuarioRepository.existsById(1L)).thenReturn(true);
        when(eventoRepository.findProximosEventosByUsuarioId(eq(1L), any(), any()))
                .thenReturn(List.of(evento));
        when(asistenciaEventoRepository.findByEventoIdAndUsuarioId(1L, 1L))
                .thenReturn(Optional.of(asistencia));

        List<MisEventosItemResponse> result = misEventosService.obtenerMisEventos(1L);

        assertThat(result.get(0).getAsistenciaConfirmada()).isTrue();
    }

    @Test
    void obtenerMisEventosShouldThrowWhenUsuarioNotFound() {
        when(usuarioRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> misEventosService.obtenerMisEventos(99L))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void obtenerMisEventosShouldHandleEventWithNullComunidad() {
        Evento evento = buildEvento(1L, LocalDateTime.now().plusHours(3));
        evento.setComunidad(null);

        when(usuarioRepository.existsById(1L)).thenReturn(true);
        when(eventoRepository.findProximosEventosByUsuarioId(eq(1L), any(), any()))
                .thenReturn(List.of(evento));
        when(asistenciaEventoRepository.findByEventoIdAndUsuarioId(1L, 1L))
                .thenReturn(Optional.empty());

        List<MisEventosItemResponse> result = misEventosService.obtenerMisEventos(1L);

        assertThat(result.get(0).getComunidadNombre()).isNull();
        assertThat(result.get(0).getComunidadId()).isNull();
    }

    @Test
    void obtenerMisEventosShouldHandleEventWithNullTipoEvento() {
        Evento evento = buildEvento(1L, LocalDateTime.now().plusHours(3));
        evento.setTipoEvento(null);

        when(usuarioRepository.existsById(1L)).thenReturn(true);
        when(eventoRepository.findProximosEventosByUsuarioId(eq(1L), any(), any()))
                .thenReturn(List.of(evento));
        when(asistenciaEventoRepository.findByEventoIdAndUsuarioId(1L, 1L))
                .thenReturn(Optional.empty());

        List<MisEventosItemResponse> result = misEventosService.obtenerMisEventos(1L);

        // Should default to OTRO
        assertThat(result.get(0).getTipoEvento()).isEqualTo(TipoEvento.OTRO);
    }

    // ================================================================
    // obtenerHistorialEventos
    // ================================================================

    @Test
    void obtenerHistorialEventosShouldFilterCancelledWhenRequested() {
        Evento normal = buildEvento(1L, LocalDateTime.now().minusDays(1));
        Evento cancelado = buildEvento(2L, LocalDateTime.now().minusDays(2));
        cancelado.setCancelado(true);

        when(usuarioRepository.existsById(1L)).thenReturn(true);
        when(eventoRepository.findAllEventosByUsuarioId(1L)).thenReturn(List.of(normal, cancelado));
        when(asistenciaEventoRepository.findByEventoIdAndUsuarioId(anyLong(), eq(1L)))
                .thenReturn(Optional.empty());

        List<MisEventosItemResponse> result = misEventosService.obtenerHistorialEventos(1L, false);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    @Test
    void obtenerHistorialEventosShouldIncludeCancelledWhenRequested() {
        Evento normal = buildEvento(1L, LocalDateTime.now().minusDays(1));
        Evento cancelado = buildEvento(2L, LocalDateTime.now().minusDays(2));
        cancelado.setCancelado(true);

        when(usuarioRepository.existsById(1L)).thenReturn(true);
        when(eventoRepository.findAllEventosByUsuarioId(1L)).thenReturn(List.of(normal, cancelado));
        when(asistenciaEventoRepository.findByEventoIdAndUsuarioId(anyLong(), eq(1L)))
                .thenReturn(Optional.empty());

        List<MisEventosItemResponse> result = misEventosService.obtenerHistorialEventos(1L, true);

        assertThat(result).hasSize(2);
    }

    // ================================================================
    // obtenerAlertasNoLeidas
    // ================================================================

    @Test
    void obtenerAlertasNoLeidasShouldReturnMappedAlertas() {
        Usuario usuario = buildUsuario(1L);
        Evento evento = buildEvento(1L, LocalDateTime.now().plusHours(1));

        AlertaEvento alerta = new AlertaEvento();
        alerta.setId(10L);
        alerta.setTipo(TipoAlerta.PROXIMA_24H);
        alerta.setMensaje("Evento pronto");
        alerta.setLeida(false);
        alerta.setEvento(evento);
        alerta.setUsuario(usuario);

        when(usuarioRepository.existsById(1L)).thenReturn(true);
        when(alertaEventoRepository.findUnreadByUsuarioId(1L)).thenReturn(List.of(alerta));

        List<AlertaEventoResponse> result = misEventosService.obtenerAlertasNoLeidas(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTipo()).isEqualTo(TipoAlerta.PROXIMA_24H);
    }

    // ================================================================
    // contarAlertasNoLeidas
    // ================================================================

    @Test
    void contarAlertasNoLeidasShouldReturnCount() {
        when(usuarioRepository.existsById(1L)).thenReturn(true);
        when(alertaEventoRepository.countUnreadByUsuarioId(1L)).thenReturn(5L);

        Long count = misEventosService.contarAlertasNoLeidas(1L);

        assertThat(count).isEqualTo(5L);
    }

    // ================================================================
    // marcarAlertaComoLeida
    // ================================================================

    @Test
    void marcarAlertaComeLeidaShouldUpdateAlerta() {
        Usuario usuario = buildUsuario(1L);
        Evento evento = buildEvento(1L, LocalDateTime.now().plusHours(1));

        AlertaEvento alerta = new AlertaEvento();
        alerta.setId(10L);
        alerta.setTipo(TipoAlerta.INMINENTE_15MIN);
        alerta.setMensaje("msg");
        alerta.setLeida(false);
        alerta.setEvento(evento);
        alerta.setUsuario(usuario);

        when(alertaEventoRepository.findById(10L)).thenReturn(Optional.of(alerta));
        when(alertaEventoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(alertaEventoRepository.countUnreadByUsuarioId(1L)).thenReturn(0L);

        AlertaEventoResponse result = misEventosService.marcarAlertaComoLeida(10L, 1L);

        assertThat(result.getLeida()).isTrue();
    }

    @Test
    void marcarAlertaComeLeidaShouldThrowWhenNotOwned() {
        AlertaEvento alerta = new AlertaEvento();
        alerta.setId(10L);
        alerta.setUsuario(buildUsuario(99L));

        when(alertaEventoRepository.findById(10L)).thenReturn(Optional.of(alerta));

        assertThatThrownBy(() -> misEventosService.marcarAlertaComoLeida(10L, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No tienes permiso");
    }

    @Test
    void marcarAlertaComeLeidaShouldThrowWhenNotFound() {
        when(alertaEventoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> misEventosService.marcarAlertaComoLeida(99L, 1L))
                .isInstanceOf(RuntimeException.class);
    }

    // ================================================================
    // marcarTodasComoLeidas
    // ================================================================

    @Test
    void marcarTodasComeLeidasShouldCallRepository() {
        when(alertaEventoRepository.countUnreadByUsuarioId(1L)).thenReturn(0L);

        misEventosService.marcarTodasComoLeidas(1L);

        verify(alertaEventoRepository).markAllAsReadByUsuarioId(eq(1L), any(LocalDateTime.class));
    }

    // ================================================================
    // generarAlertasParaRango
    // ================================================================

    @Test
    void generarAlertasParaRangoShouldCreateAlertasForNewEvents() {
        LocalDateTime desde = LocalDateTime.now();
        LocalDateTime hasta = desde.plusMinutes(15);
        Evento evento = buildEvento(1L, desde.plusMinutes(10));
        Usuario usuario = buildUsuario(5L);

        when(eventoRepository.findEventosInRange(desde, hasta)).thenReturn(List.of(evento));
        when(alertaEventoRepository.findEventoIdsWithAlertaInRange(
                        desde, hasta, TipoAlerta.INMINENTE_15MIN))
                .thenReturn(List.of());
        when(usuarioRepository.findMiembrosByComunidadId(10L)).thenReturn(List.of(usuario));
        when(alertaEventoRepository.findByEventoIdAndUsuarioIdAndTipo(
                        1L, 5L, TipoAlerta.INMINENTE_15MIN))
                .thenReturn(Optional.empty());
        when(alertaEventoRepository.countUnreadByUsuarioId(5L)).thenReturn(1L);

        misEventosService.generarAlertasParaRango(desde, hasta, TipoAlerta.INMINENTE_15MIN);

        verify(alertaEventoRepository).save(any(AlertaEvento.class));
    }

    @Test
    void generarAlertasParaRangoShouldSkipEventsWithExistingAlertas() {
        LocalDateTime desde = LocalDateTime.now();
        LocalDateTime hasta = desde.plusMinutes(15);
        Evento evento = buildEvento(1L, desde.plusMinutes(10));

        when(eventoRepository.findEventosInRange(desde, hasta)).thenReturn(List.of(evento));
        when(alertaEventoRepository.findEventoIdsWithAlertaInRange(
                        desde, hasta, TipoAlerta.PROXIMA_24H))
                .thenReturn(List.of(1L)); // Already has alerta

        misEventosService.generarAlertasParaRango(desde, hasta, TipoAlerta.PROXIMA_24H);

        verify(alertaEventoRepository, never()).save(any());
    }

    @Test
    void generarAlertasParaRangoShouldSkipEventWithNullComunidad() {
        LocalDateTime desde = LocalDateTime.now();
        LocalDateTime hasta = desde.plusMinutes(15);
        Evento evento = buildEvento(1L, desde.plusMinutes(10));
        evento.setComunidad(null);

        when(eventoRepository.findEventosInRange(desde, hasta)).thenReturn(List.of(evento));
        when(alertaEventoRepository.findEventoIdsWithAlertaInRange(
                        desde, hasta, TipoAlerta.PROXIMA_24H))
                .thenReturn(List.of());

        misEventosService.generarAlertasParaRango(desde, hasta, TipoAlerta.PROXIMA_24H);

        verify(usuarioRepository, never()).findMiembrosByComunidadId(any());
    }

    @Test
    void generarAlertasParaRangoShouldNotDuplicateForSameUser() {
        LocalDateTime desde = LocalDateTime.now();
        LocalDateTime hasta = desde.plusMinutes(15);
        Evento evento = buildEvento(1L, desde.plusMinutes(10));
        Usuario usuario = buildUsuario(5L);

        when(eventoRepository.findEventosInRange(desde, hasta)).thenReturn(List.of(evento));
        when(alertaEventoRepository.findEventoIdsWithAlertaInRange(
                        desde, hasta, TipoAlerta.INMINENTE_15MIN))
                .thenReturn(List.of());
        when(usuarioRepository.findMiembrosByComunidadId(10L)).thenReturn(List.of(usuario));
        when(alertaEventoRepository.findByEventoIdAndUsuarioIdAndTipo(
                        1L, 5L, TipoAlerta.INMINENTE_15MIN))
                .thenReturn(Optional.of(new AlertaEvento())); // Already exists

        misEventosService.generarAlertasParaRango(desde, hasta, TipoAlerta.INMINENTE_15MIN);

        verify(alertaEventoRepository, never()).save(any(AlertaEvento.class));
    }
}
