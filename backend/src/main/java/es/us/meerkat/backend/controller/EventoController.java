package es.us.meerkat.backend.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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
    // COMUNIDAD CREAR EVENTO                   //TODO: Hacer cuando la parte de comunidades este
    // ===============================

    /**
     * Crea un nuevo evento.
     *
     * @param organizadorId Identificador del usuario organizador.
     * @param titulo Título del evento.
     * @param descripcion Descripción del evento.
     * @param fechaInicio Fecha y hora de inicio.
     * @param fechaFin Fecha y hora de fin.
     * @param aforo Aforo máximo.
     * @param queLlevar Qué llevar al evento.
     * @param esVirtual Si es evento virtual.
     * @param privado Si es un evento privado.
     * @return El evento creado. @PostMapping @Operation(summary = "Crear nuevo evento", description
     *     = "Crea un nuevo evento de estudio") public ResponseEntity<Evento>
     *     crearEvento( @Parameter(description = "ID del organizador") @RequestParam final Long
     *     organizadorId, @Parameter(description = "Título del evento") @RequestParam final String
     *     titulo, @Parameter(description = "Descripción") @RequestParam final String
     *     descripcion, @Parameter(description = "Fecha/hora inicio") @RequestParam final
     *     LocalDateTime fechaInicio, @Parameter(description = "Fecha/hora fin") @RequestParam final
     *     LocalDateTime fechaFin, @Parameter(description = "Aforo máximo") @RequestParam final
     *     Integer aforo, @Parameter(description = "Qué llevar") @RequestParam final String
     *     queLlevar, @Parameter(description = "Es virtual") @RequestParam final Boolean
     *     esVirtual, @Parameter(description = "Es privado") @RequestParam final Boolean privado) {
     *     <p>final Evento evento = eventoService.crearEvento(organizadorId, titulo, descripcion,
     *     fechaInicio, fechaFin, aforo, queLlevar, esVirtual, privado);
     *     <p>return ResponseEntity.status(HttpStatus.CREATED).body(evento); }
     */
    // ===============================
    // OBTENER EVENTO
    // ===============================

    /**
     * Obtiene un evento por su ID.
     *
     * @param eventId Identificador del evento.
     * @return El evento encontrado.
     */
    @GetMapping("/{eventId}")
    @Operation(
            summary = "Obtener evento por ID",
            description = "Devuelve los detalles completos de un evento")
    public ResponseEntity<EventDetailResponse> obtenerEvento(
            @PathVariable @Parameter(description = "ID del evento") final Long eventId) {
        return ResponseEntity.ok(eventoService.obtenerEvento(eventId).toDTO());
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
     * Obtiene todos los eventos visibles en el mapa.
     *
     * @return Lista de eventos visibles en mapa.
     */
    @GetMapping("/map")
    @Operation(
            summary = "Obtener eventos en mapa",
            description = "Devuelve eventos marcados como visibles en el mapa")
    public ResponseEntity<List<EventSummaryResponse>> obtenerEventosEnMapa() {
        List<EventSummaryResponse> response =
                eventoService.obtenerEventosEnMapa().stream()
                        .map(Evento::toSummaryDTO)
                        .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Obtiene ubicaciones recomendadas para eventos.
     *
     * @return Lista de ubicaciones recomendadas.
     */
    @GetMapping("/recommended-locations")
    @Operation(
            summary = "Ubicaciones recomendadas",
            description = "Devuelve ubicaciones populares para eventos")
    public ResponseEntity<List<String>> obtenerUbicacionesRecomendadas() {
        return ResponseEntity.ok(eventoService.obtenerUbicacionesRecomendadas());
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
            @Parameter(description = "Título") @RequestParam final String titulo,
            @Parameter(description = "Descripción") @RequestParam final String descripcion,
            @Parameter(description = "Fecha/hora inicio") @RequestParam
                    final LocalDateTime fechaInicio,
            @Parameter(description = "Fecha/hora fin") @RequestParam final LocalDateTime fechaFin,
            @Parameter(description = "Aforo máximo") @RequestParam final Integer aforo,
            @Parameter(description = "Qué llevar") @RequestParam final String queLlevar,
            @Parameter(description = "Es virtual") @RequestParam final Boolean esVirtual,
            @Parameter(description = "Es privado") @RequestParam final Boolean privado,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }

        final Evento evento = eventoService.obtenerEvento(eventId);
        if (!evento.getCreador().getId().equals(usuario.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Solo el creador del evento puede editarlo");
        }

        final Evento eventoEditado =
                eventoService.editarEvento(
                        eventId,
                        titulo,
                        descripcion,
                        fechaInicio,
                        fechaFin,
                        aforo,
                        queLlevar,
                        esVirtual,
                        privado);

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

        final Evento evento = eventoService.obtenerEvento(eventId);
        if (!evento.getCreador().getId().equals(usuario.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Solo el creador del evento puede cancelarlo");
        }

        return ResponseEntity.ok(eventoService.cancelarEvento(eventId, motivo).toDTO());
    }
}
