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

import es.us.meerkat.backend.dto.EventSummaryResponse;
import es.us.meerkat.backend.entity.Comunidad;
import es.us.meerkat.backend.entity.Evento;
import es.us.meerkat.backend.entity.Ubicacion;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.service.EventoService;

@ExtendWith(MockitoExtension.class)
class EventoControllerTest {

    @Mock private EventoService eventoService;

    @InjectMocks private EventoController eventoController;

    @Test
    void obtenerEventosEnMapaShouldReturnVisibleEventsMappedAsSummary() {
        Evento evento = buildEvento(10L, true);
        when(eventoService.obtenerEventosEnMapa()).thenReturn(List.of(evento));

        ResponseEntity<List<EventSummaryResponse>> response =
                eventoController.obtenerEventosEnMapa();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getId()).isEqualTo(10L);
        assertThat(response.getBody().get(0).getTitulo()).isEqualTo("Meet de Álgebra");
    }

    @Test
    void obtenerUbicacionesRecomendadasShouldReturnRecommendedList() {
        when(eventoService.obtenerUbicacionesRecomendadas())
                .thenReturn(List.of("Biblioteca Central", "Coworking Norte"));

        ResponseEntity<List<String>> response = eventoController.obtenerUbicacionesRecomendadas();

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
        when(eventoService.obtenerEvento(1L)).thenReturn(evento);

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
                                        otroUsuario))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");
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
}
