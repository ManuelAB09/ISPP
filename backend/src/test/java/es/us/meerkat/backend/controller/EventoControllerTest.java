package es.us.meerkat.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import es.us.meerkat.backend.controller.events.EventoController;
import es.us.meerkat.backend.dto.events.CreateEventRequest;
import es.us.meerkat.backend.dto.events.EventDetailResponse;
import es.us.meerkat.backend.dto.events.EventSummaryResponse;
import es.us.meerkat.backend.entity.communities.Comunidad;
import es.us.meerkat.backend.entity.events.Evento;
import es.us.meerkat.backend.entity.maps.Ubicacion;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.service.events.EventoService;

@ExtendWith(MockitoExtension.class)
class EventoControllerTest {

    @Mock private EventoService eventoService;

    @Mock
    private es.us.meerkat.backend.service.communities.AuthorizationService authorizationService;

    @InjectMocks private EventoController eventoController;

    @Test
    void obtenerEventosEnMapaShouldReturnVisibleEventsMappedAsSummary() {
        Evento evento = buildEvento(10L, true);
        when(eventoService.obtenerEventosEnMapa(null, null, null)).thenReturn(List.of(evento));

        ResponseEntity<List<EventSummaryResponse>> response =
                eventoController.obtenerEventosEnMapa(null, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getId()).isEqualTo(10L);
        assertThat(response.getBody().get(0).getTitulo()).isEqualTo("Meet de Álgebra");
    }

    @Test
    void obtenerUbicacionesRecomendadasShouldReturnRecommendedList() {
        when(eventoService.obtenerUbicacionesRecomendadas(37.38, -5.99, 10.0))
                .thenReturn(List.of("Biblioteca Central", "Coworking Norte"));

        ResponseEntity<List<String>> response =
                eventoController.obtenerUbicacionesRecomendadas(37.38, -5.99, 10.0);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly("Biblioteca Central", "Coworking Norte");
    }

    @Test
    void editarEventoShouldFailWhenUserIsNotAuthenticated() {
        assertThatThrownBy(
                        () ->
                                eventoController.editarEvento(
                                        1L,
                                        "Título",
                                        "Desc",
                                        LocalDateTime.now().plusDays(1),
                                        LocalDateTime.now().plusDays(1).plusHours(2),
                                        20,
                                        "Portátil",
                                        false,
                                        false,
                                        1L,
                                        true,
                                        null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401 UNAUTHORIZED");
    }

    @Test
    void editarEventoShouldFailWhenEditorIsNotCreator() {
        Usuario creador = new Usuario();
        creador.setId(99L);

        Usuario otroUsuario = new Usuario();
        otroUsuario.setId(1L);

        Evento evento = buildEvento(1L, true);
        evento.setCreador(creador);
        when(eventoService.obtenerEventoInterno(1L)).thenReturn(evento);

        assertThatThrownBy(
                        () ->
                                eventoController.editarEvento(
                                        1L,
                                        "Título",
                                        "Desc",
                                        LocalDateTime.now().plusDays(1),
                                        LocalDateTime.now().plusDays(1).plusHours(2),
                                        20,
                                        "Portátil",
                                        false,
                                        false,
                                        1L,
                                        true,
                                        otroUsuario))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");
    }

    @Test
    void editarEventoShouldFailWhenEventAlreadyStarted() {
        Usuario creador = new Usuario();
        creador.setId(1L);

        Evento evento = buildEvento(1L, true);
        evento.setCreador(creador);
        evento.setFechaHora(LocalDateTime.now().minusMinutes(5));
        when(eventoService.obtenerEventoInterno(1L)).thenReturn(evento);

        assertThatThrownBy(
                        () ->
                                eventoController.editarEvento(
                                        1L,
                                        "Título",
                                        "Desc",
                                        LocalDateTime.now().plusDays(1),
                                        LocalDateTime.now().plusDays(1).plusHours(2),
                                        20,
                                        "Portátil",
                                        false,
                                        false,
                                        1L,
                                        true,
                                        creador))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");
    }

    @Test
    void cancelarEventoShouldFailWhenEventAlreadyStarted() {
        Usuario creador = new Usuario();
        creador.setId(1L);

        Evento evento = buildEvento(1L, true);
        evento.setCreador(creador);
        evento.setFechaHora(LocalDateTime.now().minusMinutes(1));
        when(eventoService.obtenerEventoInterno(1L)).thenReturn(evento);

        assertThatThrownBy(() -> eventoController.cancelarEvento(1L, "motivo", creador))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");
    }

    private Evento buildEvento(final Long id, final boolean visibleMapa) {
        Usuario creador = new Usuario();
        creador.setId(1L);
        creador.setNombre("Organizador");

        Comunidad comunidad = Comunidad.builder().id(3L).nombre("Comunidad Matemáticas").build();

        Ubicacion ubicacion =
                Ubicacion.builder()
                        .id(5L)
                        .nombre("Biblioteca Central")
                        .direccion("Calle Estudio")
                        .latitud(37.38)
                        .longitud(-5.99)
                        .tipo("library")
                        .coste("GRATIS")
                        .build();

        Evento evento = new Evento();
        evento.setId(id);
        evento.setTitulo("Meet de Álgebra");
        evento.setDescripcion("Repaso final");
        evento.setFechaHora(LocalDateTime.now().plusDays(1));
        evento.setAforo(20);
        evento.setAsistentesConfirmados(5);
        evento.setEsVirtual(false);
        evento.setCancelado(false);
        evento.setVisibleMapa(visibleMapa);
        evento.setComunidad(comunidad);
        evento.setCreador(creador);
        evento.setUbicacion(ubicacion);
        return evento;
    }

    @Test
    void crearEventoShouldAllowWhenAdminOrProfesor() {
        Usuario usuario = new Usuario();
        usuario.setId(2L);

        CreateEventRequest req = new CreateEventRequest();
        req.setTitulo("Clase");
        req.setDescripcion("Desc");
        req.setFechaHora(LocalDateTime.now().plusDays(1));
        req.setFechaFin(LocalDateTime.now().plusDays(1).plusHours(1));
        req.setAforo(30);
        req.setVisibleEnMapa(true);

        when(authorizationService.isAdminOrProfesor(usuario.getId(), 3L)).thenReturn(true);

        Evento created = buildEvento(55L, true);
        when(eventoService.crearEvento(
                        usuario.getId(),
                        3L,
                        req.getTitulo(),
                        req.getDescripcion(),
                        req.getFechaHora(),
                        req.getFechaFin(),
                        req.getAforo(),
                        req.getQueLlevar(),
                        req.getEsVirtual(),
                        req.getPrivado(),
                        req.getEnlaceVirtual(),
                        req.getVisibleEnMapa(),
                        (Long) null))
                .thenReturn(created);

        ResponseEntity<?> resp = eventoController.crearEvento(3L, req, usuario);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        EventDetailResponse body = (EventDetailResponse) resp.getBody();
        assertThat(body.getId()).isEqualTo(55L);
    }

    @Test
    void crearEventoShouldThrow403WhenNotAuthorized() {
        Usuario usuario = new Usuario();
        usuario.setId(7L);

        CreateEventRequest req = new CreateEventRequest();
        req.setTitulo("Clase");
        req.setDescripcion("Desc");
        req.setFechaHora(LocalDateTime.now().plusDays(1));
        req.setFechaFin(LocalDateTime.now().plusDays(1).plusHours(1));
        req.setAforo(30);

        when(authorizationService.isAdminOrProfesor(usuario.getId(), 3L)).thenReturn(false);

        assertThatThrownBy(() -> eventoController.crearEvento(3L, req, usuario))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");
    }
}
