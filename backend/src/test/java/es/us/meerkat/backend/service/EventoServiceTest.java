package es.us.meerkat.backend.service;

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

import es.us.meerkat.backend.entity.Comunidad;
import es.us.meerkat.backend.entity.Evento;
import es.us.meerkat.backend.entity.MiembroComunidad;
import es.us.meerkat.backend.entity.TipoGrupo;
import es.us.meerkat.backend.entity.Ubicacion;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.ComunidadRepository;
import es.us.meerkat.backend.repository.EventoRepository;
import es.us.meerkat.backend.repository.MiembroComunidadRepository;
import es.us.meerkat.backend.repository.UbicacionRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class EventoServiceTest {

    @Mock private EventoRepository eventoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ComunidadRepository comunidadRepository;
    @Mock private MiembroComunidadRepository miembroComunidadRepository;
    @Mock private UbicacionRepository ubicacionRepository;
    @Mock private AuthorizationService authorizationService;

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

        Evento evento = new Evento();
        evento.setId(eventId);
        evento.setUbicacion(buildUbicacion(9L));

        when(eventoRepository.findById(eventId)).thenReturn(Optional.of(evento));
        when(ubicacionRepository.findById(ubicacionId))
                .thenReturn(Optional.of(buildUbicacion(ubicacionId)));
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

        when(eventoRepository.findVisibleOnMap()).thenReturn(List.of(e1, e2));

        List<Evento> visibles = eventoService.obtenerEventosEnMapa();

        assertThat(visibles).hasSize(2);
        verify(eventoRepository).findVisibleOnMap();
    }

    @Test
    void obtenerUbicacionesRecomendadasShouldReturnPlaceholderListUntilImplemented() {
        List<String> result = eventoService.obtenerUbicacionesRecomendadas();

        assertThat(result).isEmpty();
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
