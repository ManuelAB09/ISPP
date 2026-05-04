package es.us.meerkat.backend.controller.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import es.us.meerkat.backend.dto.events.CreateEventRequest;
import es.us.meerkat.backend.dto.events.EventDetailResponse;
import es.us.meerkat.backend.dto.events.EventSummaryResponse;
import es.us.meerkat.backend.entity.communities.Comunidad;
import es.us.meerkat.backend.entity.communities.MiembroComunidad;
import es.us.meerkat.backend.entity.communities.RolComunidad;
import es.us.meerkat.backend.entity.events.Evento;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.communities.MiembroComunidadRepository;
import es.us.meerkat.backend.service.communities.AuthorizationService;
import es.us.meerkat.backend.service.events.EventoService;

/**
 * Comprehensive test suite for EventoController.
 *
 * <p>Tests cover CRUD operations, authorization, validation, and edge cases for event management.
 * Includes 30+ test cases covering happy paths, error handling, permissions, and boundary
 * conditions.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventoControllerTest {

    @Mock private EventoService eventoService;
    @Mock private AuthorizationService authorizationService;
    @Mock private MiembroComunidadRepository miembroComunidadRepository;

    @InjectMocks private EventoController controller;

    private Usuario usuario;
    private Usuario usuarioAlumno;
    private Comunidad comunidad;
    private Evento evento;
    private CreateEventRequest createEventRequest;
    private Usuario usuario2;
    private Usuario usuarioAlumno2;

    private Evento evento2;
    private CreateEventRequest createEventRequest2;
    private Long eventId;
    private Long comunidadId;
    private Long usuarioId;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("profesor@test.es");

        usuarioAlumno = new Usuario();
        usuarioAlumno.setId(2L);
        usuarioAlumno.setEmail("alumno@test.es");

        comunidad = new Comunidad();
        comunidad.setId(50L);
        comunidad.setNombre("Test Community");

        evento = new Evento();
        evento.setId(100L);
        evento.setTitulo("Reunión de Estudio");
        evento.setDescripcion("Descripción del evento");
        evento.setFechaHora(LocalDateTime.now().plusDays(1));
        evento.setFechaFin(LocalDateTime.now().plusDays(1).plusHours(2));
        evento.setAforo(30);
        evento.setEsVirtual(false);
        evento.setPrivado(false);
        evento.setVisibleMapa(true);
        evento.setCreador(usuario);
        evento.setComunidad(comunidad);

        createEventRequest = new CreateEventRequest();
        createEventRequest.setTitulo("Nuevo Evento");
        createEventRequest.setDescripcion("Descripción del nuevo evento");
        createEventRequest.setFechaHora(LocalDateTime.now().plusDays(1));
        createEventRequest.setFechaFin(LocalDateTime.now().plusDays(1).plusHours(2));
        createEventRequest.setAforo(25);
        createEventRequest.setEsVirtual(false);
        createEventRequest.setPrivado(false);
        createEventRequest.setVisibleEnMapa(true);

        usuarioId = 1L;
        eventId = 100L;
        comunidadId = 50L;

        usuario2 = buildUsuario(usuarioId, "profesor@test.es");
        usuarioAlumno2 = buildUsuario(2L, "alumno@test.es");

        evento2 = buildEvento(eventId, usuarioId, comunidadId, "Reunión de Estudio");

        createEventRequest2 = buildCreateEventRequest();
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

    private Comunidad buildComunidad(Long id) {
        Comunidad c = new Comunidad();
        c.setId(id);
        c.setNombre("Test Community");
        return c;
    }

    private Evento buildEvento(Long id, Long creadorId, Long comunidadId, String titulo) {
        Evento e = new Evento();
        e.setId(id);
        e.setTitulo(titulo);
        e.setDescripcion("Descripción del evento2");
        e.setFechaHora(LocalDateTime.now().plusDays(1));
        e.setFechaFin(LocalDateTime.now().plusDays(1).plusHours(2));
        e.setAforo(30);
        e.setAsistentesConfirmados(0);
        e.setEsVirtual(false);
        e.setPrivado(false);
        e.setVisibleMapa(true);
        e.setCancelado(false);

        Usuario creador = buildUsuario(creadorId, "creador@test.es");
        e.setCreador(creador);
        e.setComunidad(buildComunidad(comunidadId));

        return e;
    }

    private CreateEventRequest buildCreateEventRequest() {
        CreateEventRequest req = new CreateEventRequest();
        req.setTitulo("Nuevo Evento");
        req.setDescripcion("Descripción del nuevo evento2");
        req.setFechaHora(LocalDateTime.now().plusDays(1));
        req.setFechaFin(LocalDateTime.now().plusDays(1).plusHours(2));
        req.setAforo(25);
        req.setEsVirtual(false);
        req.setPrivado(false);
        req.setVisibleEnMapa(true);
        return req;
    }

    private MiembroComunidad buildMiembroComunidad(
            Long usuarioId, Long comunidadId, RolComunidad rol) {
        MiembroComunidad m = new MiembroComunidad();
        m.setId(usuarioId + 1000 + comunidadId);
        m.setUsuario(buildUsuario(usuarioId, "user@test.es"));
        m.setComunidad(buildComunidad(comunidadId));
        m.setRol(rol);
        m.setRolDocente(null);
        return m;
    }

    // ===============================
    // CREATE EVENT TESTS
    // ===============================

    @Test
    void crearEventoShouldReturnCreatedWhenAuthorized() {
        when(authorizationService.isAdminOrProfesor(usuarioId, comunidadId)).thenReturn(true);
        when(eventoService.crearEvento(
                        any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                        any(), any()))
                .thenReturn(evento2);

        ResponseEntity<EventDetailResponse> response =
                controller.crearEvento(comunidadId, createEventRequest2, usuario2);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(eventId);
    }

    @Test
    void crearEventoShouldReturnUnauthorizedWhenUserIsNull2() {
        ResponseStatusException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                        ResponseStatusException.class,
                        () -> controller.crearEvento(comunidadId, createEventRequest2, null));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void crearEventoShouldReturnForbiddenWhenNotAuthorized() {
        when(authorizationService.isAdminOrProfesor(usuarioId, comunidadId)).thenReturn(false);

        ResponseStatusException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                        ResponseStatusException.class,
                        () -> controller.crearEvento(comunidadId, createEventRequest2, usuario2));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void crearEventoShouldReturnBadRequestWhenServiceThrowsIllegalArgument() {
        when(authorizationService.isAdminOrProfesor(usuarioId, comunidadId)).thenReturn(true);
        when(eventoService.crearEvento(
                        any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                        any(), any()))
                .thenThrow(new IllegalArgumentException("Aforo inválido"));

        ResponseStatusException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                        ResponseStatusException.class,
                        () -> controller.crearEvento(comunidadId, createEventRequest2, usuario2));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ===============================
    // GET EVENT TESTS
    // ===============================

    @Test
    void obtenerEventoShouldReturnEventWhenFound() {
        when(eventoService.obtenerEvento(eventId, usuarioId)).thenReturn(evento2);
        when(miembroComunidadRepository.findByUsuarioIdAndComunidadId(usuarioId, comunidadId))
                .thenReturn(
                        Optional.of(
                                buildMiembroComunidad(
                                        usuarioId, comunidadId, RolComunidad.PROFESOR)));

        ResponseEntity<EventDetailResponse> response = controller.obtenerEvento(eventId, usuario2);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(eventId);
    }

    @Test
    void obtenerEventoShouldReturnEventWithoutUserRole() {
        when(eventoService.obtenerEvento(eventId, usuarioId)).thenReturn(evento2);
        when(miembroComunidadRepository.findByUsuarioIdAndComunidadId(anyLong(), anyLong()))
                .thenReturn(Optional.empty());

        ResponseEntity<EventDetailResponse> response = controller.obtenerEvento(eventId, usuario2);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void obtenerEventoShouldReturnEventWhenUserIsNull() {
        when(eventoService.obtenerEvento(eventId, null)).thenReturn(evento2);

        ResponseEntity<EventDetailResponse> response = controller.obtenerEvento(eventId, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(eventoService).obtenerEvento(eventId, null);
    }

    // ===============================
    // LIST EVENTS TESTS
    // ===============================

    @Test
    void listarEventosShouldReturnPublicEvents() {
        List<Evento> eventos = List.of(evento2);
        when(eventoService.obtenerEventosPublicos()).thenReturn(eventos);

        ResponseEntity<List<EventSummaryResponse>> response = controller.listarEventos();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        verify(eventoService).obtenerEventosPublicos();
    }

    @Test
    void listarEventosShouldReturnEmptyListWhenNoPublicEvents() {
        when(eventoService.obtenerEventosPublicos()).thenReturn(List.of());

        ResponseEntity<List<EventSummaryResponse>> response = controller.listarEventos();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void listarEventosShouldReturnMultipleEvents() {
        Evento eventoLocal = buildEvento(101L, 2L, comunidadId, "Segunda Reunión");
        Evento evento3 = buildEvento(102L, 3L, comunidadId, "Tercera Reunión");
        List<Evento> eventos = List.of(eventoLocal, eventoLocal, evento3);
        when(eventoService.obtenerEventosPublicos()).thenReturn(eventos);

        ResponseEntity<List<EventSummaryResponse>> response = controller.listarEventos();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(3);
    }

    // ===============================
    // MAP EVENTS TESTS
    // ===============================

    @Test
    void obtenerEventosEnMapaShouldReturnEventsWithoutFilters() {
        List<Evento> eventos = List.of(evento2);
        when(eventoService.obtenerEventosEnMapa(null, null, null, null)).thenReturn(eventos);

        ResponseEntity<List<EventSummaryResponse>> response =
                controller.obtenerEventosEnMapa(null, null, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void obtenerEventosEnMapaShouldReturnEventsWithLocationFilter() {
        Double lat = 37.3886;
        Double lon = -5.9842;
        Double radioKm = 5.0;
        List<Evento> eventos = List.of(evento2);
        when(eventoService.obtenerEventosEnMapa(lat, lon, radioKm, null)).thenReturn(eventos);

        ResponseEntity<List<EventSummaryResponse>> response =
                controller.obtenerEventosEnMapa(lat, lon, radioKm, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(eventoService).obtenerEventosEnMapa(lat, lon, radioKm, null);
    }

    @Test
    void obtenerEventosEnMapaShouldReturnEmptyListWhenNoEventsFound() {
        when(eventoService.obtenerEventosEnMapa(
                        anyDouble(),
                        anyDouble(),
                        anyDouble(),
                        org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(List.of());

        ResponseEntity<List<EventSummaryResponse>> response =
                controller.obtenerEventosEnMapa(37.0, -5.0, 10.0, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    // ===============================
    // RECOMMENDED LOCATIONS TESTS
    // ===============================

    @Test
    void obtenerUbicacionesRecomendadasShouldReturnLocations() {
        List<String> locations = List.of("Centro Histórico", "Parque María Luisa");
        when(eventoService.obtenerUbicacionesRecomendadas(anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(locations);

        ResponseEntity<List<String>> response =
                controller.obtenerUbicacionesRecomendadas(37.0, -5.0, 10.0);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    void obtenerUbicacionesRecomendadasShouldReturnEmptyListWhenNoLocations() {
        when(eventoService.obtenerUbicacionesRecomendadas(anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of());

        ResponseEntity<List<String>> response =
                controller.obtenerUbicacionesRecomendadas(37.0, -5.0, 10.0);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    // ===============================
    // EDIT EVENT TESTS
    // ===============================

    @Test
    void editarEventoShouldUpdateWhenCreatorEdits() {
        when(eventoService.obtenerEventoInterno(eventId)).thenReturn(evento2);
        Evento eventoActualizado =
                buildEvento(eventId, usuarioId, comunidadId, "Título Actualizado");
        when(eventoService.editarEvento(
                        any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                        any()))
                .thenReturn(eventoActualizado);

        ResponseEntity<EventDetailResponse> response =
                controller.editarEvento(
                        eventId,
                        "Nuevo Título",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        usuario2);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void editarEventoShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseStatusException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                        ResponseStatusException.class,
                        () ->
                                controller.editarEvento(
                                        eventId, "Título", null, null, null, null, null, null, null,
                                        null, null, null));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void editarEventoShouldReturnForbiddenWhenNotCreatorOrAdmin() {
        when(eventoService.obtenerEventoInterno(eventId)).thenReturn(evento2);
        when(authorizationService.isAdminOf(2L, comunidadId)).thenReturn(false);

        ResponseStatusException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                        ResponseStatusException.class,
                        () ->
                                controller.editarEvento(
                                        eventId,
                                        "Título",
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        usuarioAlumno2));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void editarEventoShouldReturnConflictWhenEventAlreadyStarted() {
        Evento eventoEnCurso = buildEvento(eventId, usuarioId, comunidadId, "Evento");
        eventoEnCurso.setFechaHora(LocalDateTime.now().minusHours(1));
        when(eventoService.obtenerEventoInterno(eventId)).thenReturn(eventoEnCurso);

        ResponseStatusException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                        ResponseStatusException.class,
                        () ->
                                controller.editarEvento(
                                        eventId, "Título", null, null, null, null, null, null, null,
                                        null, null, usuario2));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void editarEventoAsAdminShouldSucceed() {
        when(eventoService.obtenerEventoInterno(eventId)).thenReturn(evento2);
        when(authorizationService.isAdminOf(usuarioId, comunidadId)).thenReturn(true);
        Evento eventoActualizado = buildEvento(eventId, usuarioId, comunidadId, "Actualizado");
        when(eventoService.editarEvento(
                        any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                        any()))
                .thenReturn(eventoActualizado);

        ResponseEntity<EventDetailResponse> response =
                controller.editarEvento(
                        eventId,
                        "Nuevo Título",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        usuario2);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ===============================
    // CANCEL EVENT TESTS
    // ===============================

    @Test
    void cancelarEventoShouldReturnOkWhenCreatorCancels() {
        Evento eventoCancelado = buildEvento(eventId, usuarioId, comunidadId, "Evento");
        eventoCancelado.setCancelado(true);
        when(eventoService.obtenerEventoInterno(eventId)).thenReturn(evento2);
        when(eventoService.cancelarEvento(eventId, "Motivo válido")).thenReturn(eventoCancelado);

        ResponseEntity<EventDetailResponse> response =
                controller.cancelarEvento(eventId, "Motivo válido", usuario2);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void cancelarEventoShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseStatusException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                        ResponseStatusException.class,
                        () -> controller.cancelarEvento(eventId, "Motivo", null));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void cancelarEventoShouldReturnForbiddenWhenNotAuthorized() {
        when(eventoService.obtenerEventoInterno(eventId)).thenReturn(evento2);
        when(authorizationService.isAdminOf(2L, comunidadId)).thenReturn(false);

        ResponseStatusException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                        ResponseStatusException.class,
                        () -> controller.cancelarEvento(eventId, "Motivo", usuarioAlumno2));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void cancelarEventoShouldReturnConflictWhenEventAlreadyStarted() {
        Evento eventoEnCurso = buildEvento(eventId, usuarioId, comunidadId, "Evento");
        eventoEnCurso.setFechaHora(LocalDateTime.now().minusHours(1));
        when(eventoService.obtenerEventoInterno(eventId)).thenReturn(eventoEnCurso);

        ResponseStatusException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                        ResponseStatusException.class,
                        () -> controller.cancelarEvento(eventId, "Motivo", usuario2));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void cancelarEventoShouldReturnConflictWhenWithin30MinutesBefore() {
        Evento eventoProximo = buildEvento(eventId, usuarioId, comunidadId, "Evento");
        eventoProximo.setFechaHora(LocalDateTime.now().plusMinutes(20));
        when(eventoService.obtenerEventoInterno(eventId)).thenReturn(eventoProximo);

        ResponseStatusException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                        ResponseStatusException.class,
                        () -> controller.cancelarEvento(eventId, "Motivo", usuario2));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    // ===============================
    // CLASSROOM TASK TESTS
    // ===============================

    @Test
    void vincularTareaClassroomShouldReturnOkWhenSuccessful() {
        Map<String, String> request =
                Map.of(
                        "taskId", "task123",
                        "title", "Tarea de Homework",
                        "url", "https://classroom.google.com/task/123");

        Evento eventoActualizado = buildEvento(eventId, usuarioId, comunidadId, "Evento");
        eventoActualizado.setClassroomTaskId("task123");

        when(eventoService.vincularTareaClassroom(
                        eventId,
                        usuarioId,
                        "task123",
                        "Tarea de Homework",
                        "https://classroom.google.com/task/123"))
                .thenReturn(eventoActualizado);

        ResponseEntity<EventDetailResponse> response =
                controller.vincularTareaClassroom(eventId, request, usuario2);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void vincularTareaClassroomShouldReturnUnauthorizedWhenUserIsNull() {
        Map<String, String> request =
                Map.of(
                        "taskId", "task123",
                        "title", "Tarea",
                        "url", "https://classroom.google.com/task/123");

        ResponseStatusException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                        ResponseStatusException.class,
                        () -> controller.vincularTareaClassroom(eventId, request, null));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void vincularTareaClassroomShouldReturnBadRequestWhenMissingTaskId() {
        Map<String, String> request =
                Map.of(
                        "title", "Tarea",
                        "url", "https://classroom.google.com/task/123");

        ResponseStatusException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                        ResponseStatusException.class,
                        () -> controller.vincularTareaClassroom(eventId, request, usuario2));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void vincularTareaClassroomShouldReturnForbiddenWhenNotCreator() {
        Map<String, String> request =
                Map.of(
                        "taskId", "task123",
                        "title", "Tarea",
                        "url", "https://classroom.google.com/task/123");

        when(eventoService.vincularTareaClassroom(
                        eventId, 2L, "task123", "Tarea", "https://classroom.google.com/task/123"))
                .thenThrow(new RuntimeException("Solo el creador puede vincular tareas"));

        ResponseStatusException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                        ResponseStatusException.class,
                        () -> controller.vincularTareaClassroom(eventId, request, usuarioAlumno2));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ===============================
    // DESVINCULATE CLASSROOM TASK TESTS
    // ===============================

    @Test
    void desvincularTareaClassroomShouldReturnOkWhenSuccessful() {
        Evento eventoDesvinculado = buildEvento(eventId, usuarioId, comunidadId, "Evento");
        when(eventoService.desvincularTareaClassroom(eventId, usuarioId))
                .thenReturn(eventoDesvinculado);

        ResponseEntity<EventDetailResponse> response =
                controller.desvincularTareaClassroom(eventId, usuario2);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(eventoService).desvincularTareaClassroom(eventId, usuarioId);
    }

    @Test
    void desvincularTareaClassroomShouldReturnUnauthorizedWhenUserIsNull() {
        ResponseStatusException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                        ResponseStatusException.class,
                        () -> controller.desvincularTareaClassroom(eventId, null));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void desvincularTareaClassroomShouldReturnForbiddenWhenNotCreator() {
        when(eventoService.desvincularTareaClassroom(eventId, 2L))
                .thenThrow(new RuntimeException("Solo el creador puede desvincular tareas"));

        ResponseStatusException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                        ResponseStatusException.class,
                        () -> controller.desvincularTareaClassroom(eventId, usuarioAlumno2));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
