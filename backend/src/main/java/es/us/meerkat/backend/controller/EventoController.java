package es.us.meerkat.backend.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import es.us.meerkat.backend.dto.CreateEventRequest;
import es.us.meerkat.backend.dto.EventDetailResponse;
import es.us.meerkat.backend.dto.EventSummaryResponse;
import es.us.meerkat.backend.entity.Evento;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.service.EventoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** Controlador para manejar las operaciones relacionadas con los eventos. */
@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Tag(name = "Eventos", description = "Gestión de eventos y quedadas de estudio")
public class EventoController {

    /** Servicio para operaciones de evento. */
    private final EventoService eventoService;

    // ===============================
    // CREAR EVENTO
    // ===============================

    /**
     * Crea un nuevo evento en una comunidad.
     *
     * @param comunidadId Identificador de la comunidad.
     * @param request DTO con los datos del evento.
     * @param usuario Usuario autenticado que crea el evento.
     * @return El evento creado.
     */
    @PostMapping("/{comunidadId}")
    @Operation(
            summary = "Crear nuevo evento",
            description = "Crea un nuevo evento de estudio en una comunidad")
    public ResponseEntity<EventDetailResponse> crearEvento(
            @PathVariable @Parameter(description = "ID de la comunidad") final Long comunidadId,
            @RequestBody final CreateEventRequest request,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }

        final Evento evento;
        try {
            evento =
                    eventoService.crearEvento(
                            usuario.getId(),
                            comunidadId,
                            request.getTitulo(),
                            request.getDescripcion(),
                            request.getFechaHora(),
                            request.getFechaFin(),
                            request.getAforo(),
                            request.getQueLlevar(),
                            request.getEsVirtual(),
                            request.getPrivado(),
                            request.getEnlaceVirtual(),
                            request.getVisibleEnMapa(),
                            null);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(evento.toDTO());
    }

    // ===============================
    // OBTENER EVENTO
    // ===============================

    /**
     * Obtiene un evento por su ID.
     *
     * @param eventId Identificador del evento.
     * @param usuario Usuario autenticado (puede ser null).
     * @return El evento encontrado.
     */
    @GetMapping("/{eventId}")
    @Operation(
            summary = "Obtener evento por ID",
            description = "Devuelve los detalles completos de un evento")
    public ResponseEntity<EventDetailResponse> obtenerEvento(
            @PathVariable @Parameter(description = "ID del evento") final Long eventId,
            @AuthenticationPrincipal Usuario usuario) {
        Long usuarioId = usuario != null ? usuario.getId() : null;
        return ResponseEntity.ok(eventoService.obtenerEvento(eventId, usuarioId).toDTO());
    }

    /**
     * Obtiene todos los eventos públicos.
     *
     * @return Lista de eventos públicos.
     */
    @GetMapping
    @Operation(
            summary = "Listar eventos",
            description = "Obtiene lista de eventos públicos disponibles")
    public ResponseEntity<List<EventSummaryResponse>> listarEventos() {
        List<EventSummaryResponse> response =
                eventoService.obtenerEventosPublicos().stream()
                        .map(Evento::toSummaryDTO)
                        .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Obtiene todos los eventos visibles en el mapa. Opcionalmente filtra por radio de distancia.
     *
     * @param lat Latitud del centro de búsqueda (opcional).
     * @param lon Longitud del centro de búsqueda (opcional).
     * @param radioKm Radio de búsqueda en km (opcional).
     * @return Lista de eventos visibles en mapa.
     */
    @GetMapping("/map")
    @Operation(
            summary = "Obtener eventos en mapa",
            description =
                    "Devuelve eventos públicos marcados como visibles en el mapa,"
                            + " opcionalmente filtrados por radio de distancia")
    public ResponseEntity<List<EventSummaryResponse>> obtenerEventosEnMapa(
            @RequestParam(required = false)
                    @Parameter(description = "Latitud del centro de búsqueda")
                    final Double lat,
            @RequestParam(required = false)
                    @Parameter(description = "Longitud del centro de búsqueda")
                    final Double lon,
            @RequestParam(required = false) @Parameter(description = "Radio de búsqueda en km")
                    final Double radioKm) {
        List<EventSummaryResponse> response =
                eventoService.obtenerEventosEnMapa(lat, lon, radioKm).stream()
                        .map(Evento::toSummaryDTO)
                        .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Obtiene ubicaciones recomendadas para eventos dentro de un radio.
     *
     * @param lat Latitud del centro de búsqueda.
     * @param lon Longitud del centro de búsqueda.
     * @param radioKm Radio de búsqueda en km.
     * @return Lista de ubicaciones recomendadas.
     */
    @GetMapping("/recommended-locations")
    @Operation(
            summary = "Ubicaciones recomendadas",
            description = "Devuelve nombres de ubicaciones con eventos activos dentro del radio")
    public ResponseEntity<List<String>> obtenerUbicacionesRecomendadas(
            @RequestParam(required = false)
                    @Parameter(description = "Latitud del centro de búsqueda")
                    final Double lat,
            @RequestParam(required = false)
                    @Parameter(description = "Longitud del centro de búsqueda")
                    final Double lon,
            @RequestParam(required = false) @Parameter(description = "Radio de búsqueda en km")
                    final Double radioKm) {
        return ResponseEntity.ok(eventoService.obtenerUbicacionesRecomendadas(lat, lon, radioKm));
    }

    // ===============================
    // EDITAR EVENTO
    // ===============================

    /**
     * Edita un evento existente.
     *
     * @param eventId Identificador del evento.
     * @param titulo Título del evento.
     * @param descripcion Descripción del evento.
     * @param fechaInicio Fecha y hora de inicio.
     * @param fechaFin Fecha y hora de fin.
     * @param aforo Aforo máximo.
     * @param queLlevar Qué llevar al evento.
     * @param esVirtual Si es evento virtual.
     * @param privado Si es un evento privado.
     * @return El evento actualizado.
     */
    @PutMapping("/{eventId}")
    @Operation(
            summary = "Editar evento",
            description = "Actualiza la información de un evento existente")
    public ResponseEntity<EventDetailResponse> editarEvento(
            @PathVariable @Parameter(description = "ID del evento") final Long eventId,
            @Parameter(description = "Título") @RequestParam(required = false) final String titulo,
            @Parameter(description = "Descripción") @RequestParam(required = false)
                    final String descripcion,
            @Parameter(description = "Fecha/hora inicio") @RequestParam(required = false)
                    final LocalDateTime fechaInicio,
            @Parameter(description = "Fecha/hora fin") @RequestParam(required = false)
                    final LocalDateTime fechaFin,
            @Parameter(description = "Aforo máximo") @RequestParam(required = false)
                    final Integer aforo,
            @Parameter(description = "Qué llevar") @RequestParam(required = false)
                    final String queLlevar,
            @Parameter(description = "Es virtual") @RequestParam(required = false)
                    final Boolean esVirtual,
            @Parameter(description = "Es privado") @RequestParam(required = false)
                    final Boolean privado,
            @Parameter(description = "ID de ubicación") @RequestParam(required = false)
                    final Long ubicacionId,
            @Parameter(description = "Visible en mapa") @RequestParam(required = false)
                    final Boolean visibleEnMapa,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }

        final Evento evento = eventoService.obtenerEventoInterno(eventId);
        if (!evento.getCreador().getId().equals(usuario.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Solo el creador del evento puede editarlo");
        }

        if (evento.getFechaHora() != null && !evento.getFechaHora().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "No se puede actualizar un evento que ya ha comenzado");
        }

        final String tituloFinal = titulo != null ? titulo : evento.getTitulo();
        final String descripcionFinal = descripcion != null ? descripcion : evento.getDescripcion();
        final LocalDateTime fechaInicioFinal =
                fechaInicio != null ? fechaInicio : evento.getFechaHora();
        final LocalDateTime fechaFinFinal = fechaFin != null ? fechaFin : evento.getFechaFin();
        final Integer aforoFinal = aforo != null ? aforo : evento.getAforo();
        final String queLlevarFinal = queLlevar != null ? queLlevar : evento.getQueLlevar();
        final Boolean esVirtualFinal = esVirtual != null ? esVirtual : evento.getEsVirtual();
        final Boolean privadoFinal = privado != null ? privado : evento.getPrivado();

        final Evento eventoEditado;
        try {
            eventoEditado =
                    eventoService.editarEvento(
                            eventId,
                            tituloFinal,
                            descripcionFinal,
                            fechaInicioFinal,
                            fechaFinFinal,
                            aforoFinal,
                            queLlevarFinal,
                            esVirtualFinal,
                            privadoFinal,
                            ubicacionId,
                            visibleEnMapa);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        return ResponseEntity.ok(eventoEditado.toDTO());
    }

    // ===============================
    // CANCELAR EVENTO
    // ===============================

    /**
     * Cancela un evento existente.
     *
     * @param eventId Identificador del evento.
     * @param motivo Motivo de la cancelación.
     * @return El evento cancelado.
     */
    @PostMapping(
            "/{eventId}/cancel") // TODO: Esto debería de ser un PUT o un PATCH pero yo no mando
    // asiq nos vemo
    @Operation(summary = "Cancelar evento", description = "Cancela un evento y registra el motivo")
    public ResponseEntity<EventDetailResponse> cancelarEvento(
            @PathVariable @Parameter(description = "ID del evento") final Long eventId,
            @Parameter(description = "Motivo") @RequestParam final String motivo,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }

        final Evento evento = eventoService.obtenerEventoInterno(eventId);
        if (!evento.getCreador().getId().equals(usuario.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Solo el creador del evento puede cancelarlo");
        }

        if (evento.getFechaHora() != null && !evento.getFechaHora().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "No se puede cancelar un evento que ya ha comenzado");
        }

        // Solo se puede cancelar hasta 30 minutos antes del inicio
        if (evento.getFechaHora() != null
                && evento.getFechaHora().minusMinutes(30).isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Solo se puede cancelar un evento hasta 30 minutos antes de su inicio");
        }

        try {
            return ResponseEntity.ok(eventoService.cancelarEvento(eventId, motivo).toDTO());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    // ===============================
    // CLASSROOM TASK
    // ===============================

    /**
     * Vincula una tarea de Google Classroom a un evento.
     *
     * @param eventId Identificador del evento.
     * @param request Cuerpo con la info de la tarea (taskId, title, url).
     * @param usuario Usuario autenticado.
     * @return El evento actualizado.
     */
    @PostMapping("/{eventId}/classroom-task")
    @Operation(
            summary = "Vincular tarea de Classroom",
            description = "Vincula una tarea de Google Classroom a un evento")
    public ResponseEntity<EventDetailResponse> vincularTareaClassroom(
            @PathVariable @Parameter(description = "ID del evento") final Long eventId,
            @RequestBody final Map<String, String> request,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }

        String taskId = request.get("taskId");
        String title = request.get("title");
        String url = request.get("url");

        if (taskId == null || title == null || url == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Faltan datos de la tarea");
        }

        try {
            Evento eventoEditado =
                    eventoService.vincularTareaClassroom(
                            eventId, usuario.getId(), taskId, title, url);
            return ResponseEntity.ok(eventoEditado.toDTO());
        } catch (RuntimeException e) {
            if (e.getMessage().contains("creador")) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * Desvincula una tarea de Google Classroom de un evento.
     *
     * @param eventId Identificador del evento.
     * @param usuario Usuario autenticado.
     * @return El evento actualizado.
     */
    @DeleteMapping("/{eventId}/classroom-task")
    @Operation(
            summary = "Desvincular tarea de Classroom",
            description = "Desvincula una tarea de Google Classroom de un evento")
    public ResponseEntity<EventDetailResponse> desvincularTareaClassroom(
            @PathVariable @Parameter(description = "ID del evento") final Long eventId,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }

        try {
            Evento eventoEditado =
                    eventoService.desvincularTareaClassroom(eventId, usuario.getId());
            return ResponseEntity.ok(eventoEditado.toDTO());
        } catch (RuntimeException e) {
            if (e.getMessage().contains("creador")) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
