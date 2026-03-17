package es.us.meerkat.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import es.us.meerkat.backend.entity.AsistenciaEvento;
import es.us.meerkat.backend.entity.Comunidad;
import es.us.meerkat.backend.entity.EstadoAsistencia;
import es.us.meerkat.backend.entity.Evento;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.AsistenciaEventoRepository;
import es.us.meerkat.backend.repository.EventoRepository;
import es.us.meerkat.backend.repository.MiembroComunidadRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class AsistenciaEventoServiceTest {

    @Mock private AsistenciaEventoRepository asistenciaRepository;
    @Mock private EventoRepository eventoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private MiembroComunidadRepository miembroRepository;

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
        Evento evento = new Evento();
        evento.setId(10L);
        evento.setAsistentesConfirmados(5);

        AsistenciaEvento asistencia = new AsistenciaEvento();
        asistencia.setEvento(evento);
        asistencia.setEstado(EstadoAsistencia.CONFIRMADA);

        when(asistenciaRepository.findByEventoAndUsuario(10L, 1L))
                .thenReturn(Optional.of(asistencia));
        when(eventoRepository.findById(10L)).thenReturn(Optional.of(evento));

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
}
