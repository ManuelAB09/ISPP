package es.us.meerkat.backend.service.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import es.us.meerkat.backend.dto.notifications.AlarmaPersonalizadaResponse;
import es.us.meerkat.backend.dto.notifications.CrearAlarmaRequest;
import es.us.meerkat.backend.dto.notifications.CrearAlarmasLoteRequest;
import es.us.meerkat.backend.entity.communities.Comunidad;
import es.us.meerkat.backend.entity.events.Evento;
import es.us.meerkat.backend.entity.events.TipoEvento;
import es.us.meerkat.backend.entity.notifications.AlarmaPersonalizada;
import es.us.meerkat.backend.entity.notifications.PreferenciasNotificacion;
import es.us.meerkat.backend.entity.notifications.TipoCanal;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.events.EventoRepository;
import es.us.meerkat.backend.repository.notifications.AlarmaPersonalizadaRepository;
import es.us.meerkat.backend.repository.notifications.AlertaEventoRepository;
import es.us.meerkat.backend.repository.users.UsuarioRepository;
import es.us.meerkat.backend.service.emails.EmailService;
import es.us.meerkat.backend.service.notifications.PreferenciasNotificacionService;

@ExtendWith(MockitoExtension.class)
class AlarmaPersonalizadaServiceTest {

    @Mock private AlarmaPersonalizadaRepository alarmaRepository;
    @Mock private EventoRepository eventoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AlertaEventoRepository alertaEventoRepository;
    @Mock private PreferenciasNotificacionService preferenciasService;
    @Mock private EmailService emailService;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks private AlarmaPersonalizadaService alarmaService;

    private Usuario buildUsuario(Long id) {
        return Usuario.builder()
                .id(id)
                .nombre("User " + id)
                .email("u" + id + "@t.com")
                .password("p")
                .build();
    }

    private Evento buildEvento(Long id, boolean cancelado) {
        Evento e = new Evento();
        e.setId(id);
        e.setTitulo("Evento " + id);
        e.setFechaHora(LocalDateTime.now().plusDays(1));
        e.setCancelado(cancelado);
        e.setComunidad(Comunidad.builder().id(10L).nombre("C").build());
        e.setTipoEvento(TipoEvento.CLASE);
        return e;
    }

    // ================================================================
    // crearAlarma
    // ================================================================

    @Test
    void crearAlarmaShouldCreateNewAlarma() {
        Usuario usuario = buildUsuario(1L);
        Evento evento = buildEvento(1L, false);
        CrearAlarmaRequest request = new CrearAlarmaRequest();
        request.setMinutosAntes(30);
        request.setCanal(TipoCanal.PLATAFORMA);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(alarmaRepository.findByEventoIdAndUsuarioIdAndMinutosAntes(1L, 1L, 30))
                .thenReturn(Optional.empty());
        when(alarmaRepository.save(any(AlarmaPersonalizada.class)))
                .thenAnswer(
                        inv -> {
                            AlarmaPersonalizada a = inv.getArgument(0);
                            a.setId(100L);
                            return a;
                        });

        AlarmaPersonalizadaResponse response = alarmaService.crearAlarma(1L, 1L, request);

        assertThat(response).isNotNull();
        verify(alarmaRepository).save(any(AlarmaPersonalizada.class));
    }

    @Test
    void crearAlarmaShouldReturnExistingWhenDuplicate() {
        Usuario usuario = buildUsuario(1L);
        Evento evento = buildEvento(1L, false);
        CrearAlarmaRequest request = new CrearAlarmaRequest();
        request.setMinutosAntes(30);
        request.setCanal(TipoCanal.PLATAFORMA);

        AlarmaPersonalizada existing = new AlarmaPersonalizada();
        existing.setId(50L);
        existing.setMinutosAntes(30);
        existing.setCanal(TipoCanal.PLATAFORMA);
        existing.setDisparada(false);
        existing.setEvento(evento);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(alarmaRepository.findByEventoIdAndUsuarioIdAndMinutosAntes(1L, 1L, 30))
                .thenReturn(Optional.of(existing));

        AlarmaPersonalizadaResponse response = alarmaService.crearAlarma(1L, 1L, request);

        assertThat(response.getId()).isEqualTo(50L);
        verify(alarmaRepository, never()).save(any());
    }

    @Test
    void crearAlarmaShouldThrowWhenEventCancelled() {
        Usuario usuario = buildUsuario(1L);
        Evento evento = buildEvento(1L, true);
        CrearAlarmaRequest request = new CrearAlarmaRequest();
        request.setMinutosAntes(30);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));

        assertThatThrownBy(() -> alarmaService.crearAlarma(1L, 1L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("cancelado");
    }

    @Test
    void crearAlarmaShouldThrowWhenEventInPast() {
        Usuario usuario = buildUsuario(1L);
        Evento evento = new Evento();
        evento.setId(1L);
        evento.setFechaHora(LocalDateTime.now().minusHours(2));
        evento.setCancelado(false);

        CrearAlarmaRequest request = new CrearAlarmaRequest();
        request.setMinutosAntes(30);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));

        assertThatThrownBy(() -> alarmaService.crearAlarma(1L, 1L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ya ha comenzado");
    }

    @Test
    void crearAlarmaShouldUseDefaultCanalFromPreferences() {
        Usuario usuario = buildUsuario(1L);
        Evento evento = buildEvento(1L, false);
        CrearAlarmaRequest request = new CrearAlarmaRequest();
        request.setMinutosAntes(60);
        request.setCanal(null); // no canal specified

        PreferenciasNotificacion prefs = new PreferenciasNotificacion();
        prefs.setCanalAlarmasPorDefecto(TipoCanal.EMAIL);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(alarmaRepository.findByEventoIdAndUsuarioIdAndMinutosAntes(1L, 1L, 60))
                .thenReturn(Optional.empty());
        when(preferenciasService.getOrCreate(1L)).thenReturn(prefs);
        when(alarmaRepository.save(any(AlarmaPersonalizada.class)))
                .thenAnswer(
                        inv -> {
                            AlarmaPersonalizada a = inv.getArgument(0);
                            a.setId(100L);
                            return a;
                        });

        alarmaService.crearAlarma(1L, 1L, request);

        ArgumentCaptor<AlarmaPersonalizada> captor =
                ArgumentCaptor.forClass(AlarmaPersonalizada.class);
        verify(alarmaRepository).save(captor.capture());
        assertThat(captor.getValue().getCanal()).isEqualTo(TipoCanal.EMAIL);
    }

    @Test
    void crearAlarmaShouldUseAmbosWhenNoPreferenceSet() {
        Usuario usuario = buildUsuario(1L);
        Evento evento = buildEvento(1L, false);
        CrearAlarmaRequest request = new CrearAlarmaRequest();
        request.setMinutosAntes(60);
        request.setCanal(null);

        PreferenciasNotificacion prefs = new PreferenciasNotificacion();
        prefs.setCanalAlarmasPorDefecto(null);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(alarmaRepository.findByEventoIdAndUsuarioIdAndMinutosAntes(1L, 1L, 60))
                .thenReturn(Optional.empty());
        when(preferenciasService.getOrCreate(1L)).thenReturn(prefs);
        when(alarmaRepository.save(any(AlarmaPersonalizada.class)))
                .thenAnswer(
                        inv -> {
                            AlarmaPersonalizada a = inv.getArgument(0);
                            a.setId(100L);
                            return a;
                        });

        alarmaService.crearAlarma(1L, 1L, request);

        ArgumentCaptor<AlarmaPersonalizada> captor =
                ArgumentCaptor.forClass(AlarmaPersonalizada.class);
        verify(alarmaRepository).save(captor.capture());
        assertThat(captor.getValue().getCanal()).isEqualTo(TipoCanal.AMBOS);
    }

    @Test
    void crearAlarmaShouldThrowWhenUsuarioNotFound() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        CrearAlarmaRequest request = new CrearAlarmaRequest();
        request.setMinutosAntes(30);

        assertThatThrownBy(() -> alarmaService.crearAlarma(99L, 1L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Usuario no encontrado");
    }

    @Test
    void crearAlarmaShouldThrowWhenEventoNotFound() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(buildUsuario(1L)));
        when(eventoRepository.findById(99L)).thenReturn(Optional.empty());

        CrearAlarmaRequest request = new CrearAlarmaRequest();
        request.setMinutosAntes(30);

        assertThatThrownBy(() -> alarmaService.crearAlarma(1L, 99L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Evento no encontrado");
    }

    // ================================================================
    // crearAlarmasLote
    // ================================================================

    @Test
    void crearAlarmasLoteShouldCreateOnlyNonDuplicates() {
        Usuario usuario = buildUsuario(1L);
        Evento evento = buildEvento(1L, false);
        CrearAlarmasLoteRequest request = new CrearAlarmasLoteRequest();
        request.setMinutosAntesList(List.of(15, 30, 60));
        request.setCanal(TipoCanal.PLATAFORMA);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        // 30 min already exists
        when(alarmaRepository.findByEventoIdAndUsuarioIdAndMinutosAntes(1L, 1L, 15))
                .thenReturn(Optional.empty());
        when(alarmaRepository.findByEventoIdAndUsuarioIdAndMinutosAntes(1L, 1L, 30))
                .thenReturn(Optional.of(new AlarmaPersonalizada()));
        when(alarmaRepository.findByEventoIdAndUsuarioIdAndMinutosAntes(1L, 1L, 60))
                .thenReturn(Optional.empty());
        when(alarmaRepository.save(any(AlarmaPersonalizada.class)))
                .thenAnswer(
                        inv -> {
                            AlarmaPersonalizada a = inv.getArgument(0);
                            a.setId(100L);
                            return a;
                        });

        List<AlarmaPersonalizadaResponse> result = alarmaService.crearAlarmasLote(1L, 1L, request);

        // Only 15 and 60 should be created (30 already exists)
        assertThat(result).hasSize(2);
    }

    // ================================================================
    // listarAlarmasPendientes
    // ================================================================

    @Test
    void listarAlarmasPendientesShouldReturnMappedResponses() {
        Evento evento = buildEvento(1L, false);
        AlarmaPersonalizada alarma = new AlarmaPersonalizada();
        alarma.setId(1L);
        alarma.setMinutosAntes(30);
        alarma.setCanal(TipoCanal.PLATAFORMA);
        alarma.setDisparada(false);
        alarma.setEvento(evento);

        when(alarmaRepository.findPendientesByUsuarioId(1L)).thenReturn(List.of(alarma));

        List<AlarmaPersonalizadaResponse> result = alarmaService.listarAlarmasPendientes(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEventoId()).isEqualTo(1L);
    }

    // ================================================================
    // listarAlarmasDeEvento
    // ================================================================

    @Test
    void listarAlarmasDeEventoShouldReturnAlarmasForEvent() {
        Evento evento = buildEvento(5L, false);
        AlarmaPersonalizada alarma = new AlarmaPersonalizada();
        alarma.setId(1L);
        alarma.setMinutosAntes(60);
        alarma.setCanal(TipoCanal.EMAIL);
        alarma.setDisparada(false);
        alarma.setEvento(evento);

        when(alarmaRepository.findByEventoIdAndUsuarioId(5L, 1L)).thenReturn(List.of(alarma));

        List<AlarmaPersonalizadaResponse> result = alarmaService.listarAlarmasDeEvento(1L, 5L);

        assertThat(result).hasSize(1);
    }

    // ================================================================
    // eliminarAlarma
    // ================================================================

    @Test
    void eliminarAlarmaShouldDeleteWhenOwnedByUser() {
        AlarmaPersonalizada alarma = new AlarmaPersonalizada();
        alarma.setId(1L);
        alarma.setUsuario(buildUsuario(5L));

        when(alarmaRepository.findById(1L)).thenReturn(Optional.of(alarma));

        alarmaService.eliminarAlarma(1L, 5L);

        verify(alarmaRepository).delete(alarma);
    }

    @Test
    void eliminarAlarmaShouldThrowWhenNotOwnedByUser() {
        AlarmaPersonalizada alarma = new AlarmaPersonalizada();
        alarma.setId(1L);
        alarma.setUsuario(buildUsuario(5L));

        when(alarmaRepository.findById(1L)).thenReturn(Optional.of(alarma));

        assertThatThrownBy(() -> alarmaService.eliminarAlarma(1L, 99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No tienes permiso");
    }

    @Test
    void eliminarAlarmaShouldThrowWhenNotFound() {
        when(alarmaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alarmaService.eliminarAlarma(99L, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no encontrada");
    }

    // ================================================================
    // eliminarAlarmasDeEvento
    // ================================================================

    @Test
    void eliminarAlarmasDeEventoShouldCallRepository() {
        alarmaService.eliminarAlarmasDeEvento(1L, 5L);

        verify(alarmaRepository).deleteByEventoIdAndUsuarioId(5L, 1L);
    }

    // ================================================================
    // dispararAlarmasPendientes
    // ================================================================

    @Test
    void dispararAlarmasPendientesShouldFirePlatformAlarms() {
        Evento evento = buildEvento(1L, false);
        Usuario usuario = buildUsuario(5L);

        AlarmaPersonalizada alarma = new AlarmaPersonalizada();
        alarma.setId(1L);
        alarma.setMinutosAntes(15);
        alarma.setCanal(TipoCanal.PLATAFORMA);
        alarma.setDisparada(false);
        alarma.setEvento(evento);
        alarma.setUsuario(usuario);

        when(alarmaRepository.findAlarmasPendientesADisparar(any(LocalDateTime.class)))
                .thenReturn(List.of(alarma));
        when(alertaEventoRepository.countUnreadByUsuarioId(5L)).thenReturn(3L);

        alarmaService.dispararAlarmasPendientes();

        verify(alertaEventoRepository).save(any());
        verify(alarmaRepository).save(alarma);
        assertThat(alarma.getDisparada()).isTrue();
    }

    @Test
    void dispararAlarmasPendientesShouldSendEmailForEmailChannel() throws Exception {
        Evento evento = buildEvento(1L, false);
        Usuario usuario = buildUsuario(5L);

        AlarmaPersonalizada alarma = new AlarmaPersonalizada();
        alarma.setId(1L);
        alarma.setMinutosAntes(60);
        alarma.setCanal(TipoCanal.EMAIL);
        alarma.setDisparada(false);
        alarma.setEvento(evento);
        alarma.setUsuario(usuario);

        when(alarmaRepository.findAlarmasPendientesADisparar(any(LocalDateTime.class)))
                .thenReturn(List.of(alarma));

        alarmaService.dispararAlarmasPendientes();

        verify(emailService).enviarRecordatorioAlarma(eq(usuario), eq(evento), any(String.class));
        verify(alarmaRepository).save(alarma);
    }

    @Test
    void dispararAlarmasPendientesShouldHandleExceptionGracefully() throws Exception {
        Evento evento = buildEvento(1L, false);
        Usuario usuario = buildUsuario(5L);

        AlarmaPersonalizada alarma = new AlarmaPersonalizada();
        alarma.setId(1L);
        alarma.setMinutosAntes(15);
        alarma.setCanal(TipoCanal.PLATAFORMA);
        alarma.setDisparada(false);
        alarma.setEvento(evento);
        alarma.setUsuario(usuario);

        when(alarmaRepository.findAlarmasPendientesADisparar(any(LocalDateTime.class)))
                .thenReturn(List.of(alarma));
        when(alertaEventoRepository.save(any())).thenThrow(new RuntimeException("DB error"));

        // Should not throw — exceptions are caught
        alarmaService.dispararAlarmasPendientes();

        // alarma should NOT be marked as disparada since the exception was thrown before that
        verify(alarmaRepository, never()).save(any());
    }

    @Test
    void dispararAlarmasPendientesShouldDoNothingWhenNoPending() {
        when(alarmaRepository.findAlarmasPendientesADisparar(any(LocalDateTime.class)))
                .thenReturn(List.of());

        alarmaService.dispararAlarmasPendientes();

        verify(alarmaRepository, never()).save(any());
    }

    // ================================================================
    // limpiarAlarmasAntiguas
    // ================================================================

    @Test
    void limpiarAlarmasAntiguasShouldCallRepository() {
        alarmaService.limpiarAlarmasAntiguas();

        verify(alarmaRepository).deleteOldDisparadas(any(LocalDateTime.class));
    }
}
