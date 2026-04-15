package es.us.meerkat.backend.service.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import es.us.meerkat.backend.entity.communities.MiembroComunidad;
import es.us.meerkat.backend.entity.communities.TipoGrupo;
import es.us.meerkat.backend.entity.events.Evento;
import es.us.meerkat.backend.entity.maps.Ubicacion;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.communities.ComunidadRepository;
import es.us.meerkat.backend.repository.communities.MiembroComunidadRepository;
import es.us.meerkat.backend.repository.events.AsistenciaEventoRepository;
import es.us.meerkat.backend.repository.events.EventoRepository;
import es.us.meerkat.backend.repository.maps.UbicacionRepository;
import es.us.meerkat.backend.repository.users.UsuarioRepository;
import es.us.meerkat.backend.service.communities.AuthorizationService;
import es.us.meerkat.backend.service.google.GoogleCalendarService;

@ExtendWith(MockitoExtension.class)
class EventoServiceTest {

    @Mock private EventoRepository eventoRepository;
    @Mock private AsistenciaEventoRepository asistenciaEventoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ComunidadRepository comunidadRepository;
    @Mock private MiembroComunidadRepository miembroComunidadRepository;
    @Mock private UbicacionRepository ubicacionRepository;
    @Mock private AuthorizationService authorizationService;
    @Mock private GoogleCalendarService googleCalendarService;

    @InjectMocks private EventoService eventoService;

    @Test
    void crearEventoShouldPersistLocationAndMapVisibilityWhenDataIsValid() {
        Long creadorId = 1L;
        Long comunidadId = 2L;
        Long ubicacionId = 5L;

        Usuario usuario = buildUsuario(creadorId);
        Comunidad comunidad = buildComunidad(comunidadId);
        Ubicacion ubicacion = buildUbicacion(ubicacionId);

        when(usuarioRepository.findById(creadorId)).thenReturn(Optional.of(usuario));
        when(comunidadRepository.findById(comunidadId)).thenReturn(Optional.of(comunidad));
        when(miembroComunidadRepository.findByUsuarioIdAndComunidadId(creadorId, comunidadId))
                .thenReturn(
                        Optional.of(
                                MiembroComunidad.builder()
                                        .usuario(usuario)
                                        .comunidad(comunidad)
                                        .build()));
        when(ubicacionRepository.findById(ubicacionId)).thenReturn(Optional.of(ubicacion));
        when(eventoRepository.save(any(Evento.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Evento evento =
                eventoService.crearEvento(
                        creadorId,
                        comunidadId,
                        "Meet de Cálculo",
                        "Repaso de integrales",
                        LocalDateTime.now().plusDays(1),
                        LocalDateTime.now().plusDays(1).plusHours(2),
                        30,
                        "Portátil",
                        false,
                        false,
                        null,
                        true,
                        ubicacionId);

        assertThat(evento.getUbicacion()).isEqualTo(ubicacion);
        assertThat(evento.getVisibleMapa()).isTrue();
        assertThat(evento.getComunidad()).isEqualTo(comunidad);
        assertThat(evento.getCreador()).isEqualTo(usuario);
    }

    @Test
    void crearEventoShouldFailWhenUserIsNotCommunityMember() {
        Long creadorId = 1L;
        Long comunidadId = 2L;

        when(usuarioRepository.findById(creadorId))
                .thenReturn(Optional.of(buildUsuario(creadorId)));
        when(comunidadRepository.findById(comunidadId))
                .thenReturn(Optional.of(buildComunidad(comunidadId)));
        when(miembroComunidadRepository.findByUsuarioIdAndComunidadId(creadorId, comunidadId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                eventoService.crearEvento(
                                        creadorId,
                                        comunidadId,
                                        "Meet",
                                        "Desc",
                                        LocalDateTime.now().plusDays(1),
                                        LocalDateTime.now().plusDays(1).plusHours(1),
                                        20,
                                        "Nada",
                                        false,
                                        false,
                                        null,
                                        true,
                                        null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no perteneces");
    }

    @Test
    void crearEventoShouldFailWhenStartDateTimeIsInThePast() {
        Long creadorId = 1L;
        Long comunidadId = 2L;

        assertThatThrownBy(
                        () ->
                                eventoService.crearEvento(
                                        creadorId,
                                        comunidadId,
                                        "Meet",
                                        "Desc",
                                        LocalDateTime.now().minusMinutes(5),
                                        LocalDateTime.now().plusHours(1),
                                        20,
                                        "Nada",
                                        false,
                                        false,
                                        null,
                                        true,
                                        null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fecha y hora actual");
    }

    @Test
    void editarEventoShouldUpdateLocationAndClearItForVirtualEvents() {
        Long eventId = 10L;
        Long ubicacionId = 5L;
        Long comunidadId = 2L;
        Long creadorId = 1L;

        Evento evento = new Evento();
        evento.setId(eventId);
        evento.setUbicacion(buildUbicacion(9L));
        evento.setComunidad(buildComunidad(comunidadId));
        evento.setCreador(buildUsuario(creadorId));

        when(eventoRepository.findById(eventId)).thenReturn(Optional.of(evento));
        when(ubicacionRepository.findById(ubicacionId))
                .thenReturn(Optional.of(buildUbicacion(ubicacionId)));
        when(miembroComunidadRepository.findUsuarioIdsByComunidadId(comunidadId))
                .thenReturn(List.of());
        when(eventoRepository.save(any(Evento.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Evento presencial =
                eventoService.editarEvento(
                        eventId,
                        "Nuevo título",
                        "Nueva desc",
                        LocalDateTime.now().plusDays(2),
                        LocalDateTime.now().plusDays(2).plusHours(2),
                        40,
                        "Cuaderno",
                        false,
                        false,
                        ubicacionId,
                        true);

        assertThat(presencial.getUbicacion()).isNotNull();

        Evento virtual =
                eventoService.editarEvento(
                        eventId,
                        "Nuevo título",
                        "Nueva desc",
                        LocalDateTime.now().plusDays(2),
                        LocalDateTime.now().plusDays(2).plusHours(2),
                        40,
                        "Cuaderno",
                        true,
                        false,
                        null,
                        false);

        assertThat(virtual.getUbicacion()).isNull();
        assertThat(virtual.getVisibleMapa()).isFalse();
    }

    @Test
    void editarEventoShouldFailWhenEventAlreadyStarted() {
        Long eventId = 12L;
        Evento evento = new Evento();
        evento.setId(eventId);
        evento.setFechaHora(LocalDateTime.now().minusHours(1));

        when(eventoRepository.findById(eventId)).thenReturn(Optional.of(evento));

        assertThatThrownBy(
                        () ->
                                eventoService.editarEvento(
                                        eventId,
                                        "Nuevo título",
                                        "Nueva desc",
                                        LocalDateTime.now().plusDays(1),
                                        LocalDateTime.now().plusDays(1).plusHours(1),
                                        25,
                                        "Cuaderno",
                                        false,
                                        false,
                                        null,
                                        true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ya ha comenzado");
    }

    @Test
    void editarEventoShouldFailWhenNewStartDateTimeIsInThePast() {
        Long eventId = 99L;

        assertThatThrownBy(
                        () ->
                                eventoService.editarEvento(
                                        eventId,
                                        "Nuevo título",
                                        "Nueva desc",
                                        LocalDateTime.now().minusMinutes(1),
                                        LocalDateTime.now().plusHours(2),
                                        25,
                                        "Cuaderno",
                                        false,
                                        false,
                                        null,
                                        true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fecha y hora actual");
    }

    @Test
    void cancelarEventoShouldFailWhenEventAlreadyStarted() {
        Long eventId = 13L;
        Evento evento = new Evento();
        evento.setId(eventId);
        evento.setFechaHora(LocalDateTime.now().minusMinutes(10));

        when(eventoRepository.findById(eventId)).thenReturn(Optional.of(evento));

        assertThatThrownBy(() -> eventoService.cancelarEvento(eventId, "motivo"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ya ha comenzado");
    }

    @Test
    void obtenerEventosEnMapaShouldReturnOnlyRepositoryVisibleEvents() {
        Evento e1 = new Evento();
        e1.setId(1L);
        Evento e2 = new Evento();
        e2.setId(2L);

        when(eventoRepository.findVisibleOnMap(any(LocalDateTime.class)))
                .thenReturn(List.of(e1, e2));

        List<Evento> visibles = eventoService.obtenerEventosEnMapa(null, null, null);

        assertThat(visibles).hasSize(2);
        verify(eventoRepository).findVisibleOnMap(any(LocalDateTime.class));
    }

    @Test
    void obtenerUbicacionesRecomendadasShouldReturnEmptyWhenNoParams() {
        List<String> result = eventoService.obtenerUbicacionesRecomendadas(null, null, null);

        assertThat(result).isEmpty();
    }

    @Test
    void obtenerEventosPorComunidadShouldUseCanceladosQueryWhenRequested() {
        Long comunidadId = 2L;

        Comunidad comunidad = new Comunidad();
        comunidad.setId(comunidadId);

        Evento publico = new Evento();
        publico.setId(1L);
        publico.setPrivado(false);
        publico.setComunidad(comunidad);

        Evento privado = new Evento();
        privado.setId(2L);
        privado.setPrivado(true);
        privado.setComunidad(comunidad);

        when(eventoRepository.findByComunidadIdCancelados(comunidadId))
                .thenReturn(List.of(publico, privado));
        when(authorizationService.isMemberOf(1L, comunidadId)) // ← cambio clave
                .thenReturn(true);

        List<Evento> result = eventoService.obtenerEventosPorComunidad(comunidadId, true, 1L);

        assertThat(result).hasSize(2);
        verify(eventoRepository).findByComunidadIdCancelados(comunidadId);
    }

    @Test
    void obtenerEventosPorComunidadShouldUseFutureNonCancelledQueryWhenCanceladosNotIncluded() {
        Long comunidadId = 2L;
        Evento publico = new Evento();
        publico.setId(1L);
        publico.setPrivado(false);
        publico.setComunidad(buildComunidad(comunidadId));

        when(eventoRepository.findByComunidadIdAndCanceladoFalseAndFuture(
                        eq(comunidadId), any(LocalDateTime.class)))
                .thenReturn(List.of(publico));

        List<Evento> result = eventoService.obtenerEventosPorComunidad(comunidadId, false, null);

        assertThat(result).hasSize(1);
        verify(eventoRepository)
                .findByComunidadIdAndCanceladoFalseAndFuture(
                        eq(comunidadId), any(LocalDateTime.class));
    }

    @Test
    void obtenerEventosPorComunidadShouldFilterPrivateEventsForAnonymousUsers() {
        Long comunidadId = 2L;
        Evento publico = new Evento();
        publico.setId(1L);
        publico.setPrivado(false);
        publico.setComunidad(buildComunidad(comunidadId));

        Evento privado = new Evento();
        privado.setId(2L);
        privado.setPrivado(true);
        privado.setComunidad(buildComunidad(comunidadId));

        when(eventoRepository.findByComunidadIdCancelados(comunidadId))
                .thenReturn(List.of(publico, privado));

        List<Evento> result = eventoService.obtenerEventosPorComunidad(comunidadId, true, null);

        assertThat(result).extracting(Evento::getId).containsExactly(1L);
    }

    private Usuario buildUsuario(final Long id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNombre("Usuario " + id);
        usuario.setEmail("user" + id + "@meerkat.es");
        return usuario;
    }

    private Comunidad buildComunidad(final Long id) {
        return Comunidad.builder()
                .id(id)
                .nombre("Comunidad Test")
                .tipoGrupo(TipoGrupo.COMUNIDAD_PUBLICA)
                .build();
    }

    private Ubicacion buildUbicacion(final Long id) {
        return Ubicacion.builder()
                .id(id)
                .nombre("Biblioteca")
                .direccion("Calle Estudio")
                .latitud(37.38)
                .longitud(-5.99)
                .tipo("library")
                .coste("GRATIS")
                .build();
    }
}
