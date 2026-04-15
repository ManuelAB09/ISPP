package es.us.meerkat.backend.service.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

import es.us.meerkat.backend.entity.communities.Comunidad;
import es.us.meerkat.backend.entity.events.AsistenciaEvento;
import es.us.meerkat.backend.entity.events.EstadoAsistencia;
import es.us.meerkat.backend.entity.events.Evento;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.communities.MiembroComunidadRepository;
import es.us.meerkat.backend.repository.events.AsistenciaEventoRepository;
import es.us.meerkat.backend.repository.events.EventoRepository;
import es.us.meerkat.backend.repository.users.UsuarioRepository;
import es.us.meerkat.backend.service.google.GoogleCalendarService;

@ExtendWith(MockitoExtension.class)
class AsistenciaEventoServiceTest {

    @Mock private AsistenciaEventoRepository asistenciaRepository;
    @Mock private EventoRepository eventoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private MiembroComunidadRepository miembroRepository;
    @Mock private GoogleCalendarService googleCalendarService;

    @InjectMocks private AsistenciaEventoService asistenciaEventoService;

    @Test
    void confirmarAsistenciaShouldCreateNewAttendance() {
        Evento evento = new Evento();
        evento.setId(10L);
        evento.setAsistentesConfirmados(0);
        evento.setAforo(100);

        Usuario usuario = new Usuario();
        usuario.setId(1L);

        when(eventoRepository.findById(10L)).thenReturn(Optional.of(evento));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(asistenciaRepository.findByEventoAndUsuario(10L, 1L)).thenReturn(Optional.empty());
        when(asistenciaRepository.save(any(AsistenciaEvento.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        AsistenciaEvento result = asistenciaEventoService.confirmarAsistencia(10L, 1L);

        assertThat(result.getEstado()).isEqualTo(EstadoAsistencia.CONFIRMADA);
        verify(eventoRepository).save(evento);
        verify(asistenciaRepository).save(any(AsistenciaEvento.class));
    }

    @Test
    void confirmarAsistenciaShouldReconfirmCancelledAttendance() {
        Evento evento = new Evento();
        evento.setId(10L);
        evento.setAsistentesConfirmados(0);
        evento.setAforo(100);

        Usuario usuario = new Usuario();
        usuario.setId(1L);

        AsistenciaEvento existing = new AsistenciaEvento();
        existing.setEvento(evento);
        existing.setUsuario(usuario);
        existing.setEstado(EstadoAsistencia.CANCELADA);

        when(eventoRepository.findById(10L)).thenReturn(Optional.of(evento));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(asistenciaRepository.findByEventoAndUsuario(10L, 1L))
                .thenReturn(Optional.of(existing));
        when(asistenciaRepository.save(any(AsistenciaEvento.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        AsistenciaEvento result = asistenciaEventoService.confirmarAsistencia(10L, 1L);

        assertThat(result.getEstado()).isEqualTo(EstadoAsistencia.CONFIRMADA);
        verify(eventoRepository).save(evento);
    }

    @Test
    void confirmarAsistenciaShouldThrowWhenEventNotFound() {
        when(eventoRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> asistenciaEventoService.confirmarAsistencia(10L, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Evento no encontrado");
    }

    @Test
    void confirmarAsistenciaShouldThrowWhenUserNotFound() {
        Evento evento = new Evento();
        evento.setId(10L);
        when(eventoRepository.findById(10L)).thenReturn(Optional.of(evento));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> asistenciaEventoService.confirmarAsistencia(10L, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Usuario no encontrado");
    }

    @Test
    void confirmarAsistenciaShouldThrowWhenNotCommunityMember() {
        Comunidad comunidad = new Comunidad();
        comunidad.setId(5L);

        Evento evento = new Evento();
        evento.setId(10L);
        evento.setComunidad(comunidad);

        Usuario usuario = new Usuario();
        usuario.setId(1L);

        when(eventoRepository.findById(10L)).thenReturn(Optional.of(evento));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(miembroRepository.findByUsuarioIdAndComunidadId(1L, 5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> asistenciaEventoService.confirmarAsistencia(10L, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("miembro de la comunidad");
    }

    @Test
    void confirmarAsistenciaShouldThrowWhenEventFull() {
        Evento evento = new Evento();
        evento.setId(10L);
        evento.setAforo(1);
        evento.setAsistentesConfirmados(1);

        Usuario usuario = new Usuario();
        usuario.setId(1L);

        when(eventoRepository.findById(10L)).thenReturn(Optional.of(evento));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> asistenciaEventoService.confirmarAsistencia(10L, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("aforo");
    }

    @Test
    void cancelarAsistenciaShouldUpdateStatusAndDecrement() {
        Usuario creador = new Usuario();
        creador.setId(99L);

        Evento evento = new Evento();
        evento.setId(10L);
        evento.setAsistentesConfirmados(5);
        evento.setCreador(creador);

        AsistenciaEvento asistencia = new AsistenciaEvento();
        asistencia.setEvento(evento);
        asistencia.setEstado(EstadoAsistencia.CONFIRMADA);

        when(asistenciaRepository.findByEventoAndUsuario(10L, 1L))
                .thenReturn(Optional.of(asistencia));
        when(eventoRepository.findById(10L)).thenReturn(Optional.of(evento));

        Usuario usuario = new Usuario();
        usuario.setId(1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        asistenciaEventoService.cancelarAsistencia(10L, 1L);

        assertThat(asistencia.getEstado()).isEqualTo(EstadoAsistencia.CANCELADA);
        verify(asistenciaRepository).save(asistencia);
        verify(eventoRepository).save(evento);
    }

    @Test
    void cancelarAsistenciaShouldThrowWhenNotFound() {
        when(asistenciaRepository.findByEventoAndUsuario(10L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> asistenciaEventoService.cancelarAsistencia(10L, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Asistencia no encontrada");
    }

    @Test
    void obtenerAsistenciaShouldReturnWhenExists() {
        AsistenciaEvento asistencia = new AsistenciaEvento();
        asistencia.setEstado(EstadoAsistencia.CONFIRMADA);

        when(asistenciaRepository.findByEventoAndUsuario(10L, 1L))
                .thenReturn(Optional.of(asistencia));

        AsistenciaEvento result = asistenciaEventoService.obtenerAsistencia(10L, 1L);
        assertThat(result.getEstado()).isEqualTo(EstadoAsistencia.CONFIRMADA);
    }

    @Test
    void obtenerAsistenciaShouldThrowWhenNotFound() {
        when(asistenciaRepository.findByEventoAndUsuario(10L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> asistenciaEventoService.obtenerAsistencia(10L, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Asistencia no encontrada");
    }

    @Test
    void obtenerAsistentesConfirmadosShouldDelegateToRepository() {
        AsistenciaEvento a1 = new AsistenciaEvento();
        a1.setEstado(EstadoAsistencia.CONFIRMADA);
        when(asistenciaRepository.findConfirmedAttendanceByEvent(10L)).thenReturn(List.of(a1));

        List<AsistenciaEvento> result = asistenciaEventoService.obtenerAsistentesConfirmados(10L);
        assertThat(result).hasSize(1);
    }

    @Test
    void obtenerAsistenciasEventoShouldReturnAll() {
        when(asistenciaRepository.findByEventoId(10L)).thenReturn(List.of());

        List<AsistenciaEvento> result = asistenciaEventoService.obtenerAsistenciasEvento(10L);
        assertThat(result).isEmpty();
    }

    @Test
    void contarAsistentesConfirmadosShouldDelegateToRepository() {
        when(asistenciaRepository.countConfirmedByEvent(10L)).thenReturn(5L);

        assertThat(asistenciaEventoService.contarAsistentesConfirmados(10L)).isEqualTo(5L);
    }

    @Test
    void cancelarAsistenciaShouldThrowWhenCreadorIntentaCancelar() {
        Usuario creador = new Usuario();
        creador.setId(1L);

        Evento evento = new Evento();
        evento.setId(10L);
        evento.setCreador(creador);
        evento.setFechaHora(LocalDateTime.now().plusDays(1));

        AsistenciaEvento asistencia = new AsistenciaEvento();
        asistencia.setEvento(evento);
        asistencia.setEstado(EstadoAsistencia.CONFIRMADA);

        when(asistenciaRepository.findByEventoAndUsuario(10L, 1L))
                .thenReturn(Optional.of(asistencia));

        assertThatThrownBy(() -> asistenciaEventoService.cancelarAsistencia(10L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("creador del evento no puede cancelar");
    }

    @Test
    void cancelarAsistenciaShouldThrowWhenEventoYaFinalizoConFechaFin() {
        Usuario creador = new Usuario();
        creador.setId(99L);

        Evento evento = new Evento();
        evento.setId(10L);
        evento.setCreador(creador);
        evento.setFechaHora(LocalDateTime.now().minusDays(2));
        evento.setFechaFin(LocalDateTime.now().minusDays(1));

        AsistenciaEvento asistencia = new AsistenciaEvento();
        asistencia.setEvento(evento);
        asistencia.setEstado(EstadoAsistencia.CONFIRMADA);

        when(asistenciaRepository.findByEventoAndUsuario(10L, 1L))
                .thenReturn(Optional.of(asistencia));

        assertThatThrownBy(() -> asistenciaEventoService.cancelarAsistencia(10L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ya ha finalizado");
    }

    @Test
    void cancelarAsistenciaShouldThrowWhenEventoYaEmpezaSinFechaFin() {
        Usuario creador = new Usuario();
        creador.setId(99L);

        Evento evento = new Evento();
        evento.setId(10L);
        evento.setCreador(creador);
        evento.setFechaHora(LocalDateTime.now().minusHours(1));
        // fechaFin null → usa fechaHora como fin

        AsistenciaEvento asistencia = new AsistenciaEvento();
        asistencia.setEvento(evento);
        asistencia.setEstado(EstadoAsistencia.CONFIRMADA);

        when(asistenciaRepository.findByEventoAndUsuario(10L, 1L))
                .thenReturn(Optional.of(asistencia));

        assertThatThrownBy(() -> asistenciaEventoService.cancelarAsistencia(10L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ya ha finalizado");
    }

    @Test
    void confirmarAsistenciaShouldThrowWhenEventoYaComenzo() {
        Evento evento = new Evento();
        evento.setId(10L);
        evento.setFechaHora(LocalDateTime.now().minusHours(1));

        when(eventoRepository.findById(10L)).thenReturn(Optional.of(evento));

        assertThatThrownBy(() -> asistenciaEventoService.confirmarAsistencia(10L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ya ha comenzado");
    }

    @Test
    void confirmarAsistenciaShouldNotIncrementWhenAlreadyConfirmed() {
        Evento evento = new Evento();
        evento.setId(10L);
        evento.setAsistentesConfirmados(3);
        evento.setAforo(100);

        Usuario usuario = new Usuario();
        usuario.setId(1L);

        AsistenciaEvento existing = new AsistenciaEvento();
        existing.setEvento(evento);
        existing.setUsuario(usuario);
        existing.setEstado(EstadoAsistencia.CONFIRMADA);

        when(eventoRepository.findById(10L)).thenReturn(Optional.of(evento));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(asistenciaRepository.findByEventoAndUsuario(10L, 1L))
                .thenReturn(Optional.of(existing));
        when(asistenciaRepository.save(any(AsistenciaEvento.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        AsistenciaEvento result = asistenciaEventoService.confirmarAsistencia(10L, 1L);

        assertThat(result.getEstado()).isEqualTo(EstadoAsistencia.CONFIRMADA);
        // El contador no debe cambiar porque ya estaba confirmada
        assertThat(evento.getAsistentesConfirmados()).isEqualTo(3);
    }

    @Test
    void cancelarAsistenciaShouldNotDecrementWhenNotConfirmed() {
        Usuario creador = new Usuario();
        creador.setId(99L);

        Evento evento = new Evento();
        evento.setId(10L);
        evento.setCreador(creador);
        evento.setFechaHora(LocalDateTime.now().plusDays(1));
        evento.setAsistentesConfirmados(2);

        AsistenciaEvento asistencia = new AsistenciaEvento();
        asistencia.setEvento(evento);
        asistencia.setEstado(EstadoAsistencia.CANCELADA); // ya estaba cancelada

        when(asistenciaRepository.findByEventoAndUsuario(10L, 1L))
                .thenReturn(Optional.of(asistencia));

        asistenciaEventoService.cancelarAsistencia(10L, 1L);

        assertThat(asistencia.getEstado()).isEqualTo(EstadoAsistencia.CANCELADA);
        // No debe tocar el contador ni el repositorio de eventos
        assertThat(evento.getAsistentesConfirmados()).isEqualTo(2);
    }
}
