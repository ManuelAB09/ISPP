package es.us.meerkat.backend.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import es.us.meerkat.backend.dto.CalificarReservaRequest;
import es.us.meerkat.backend.dto.CreateDisponibilidadRequest;
import es.us.meerkat.backend.dto.CreateReservaClaseRequest;
import es.us.meerkat.backend.dto.DisponibilidadTutorResponse;
import es.us.meerkat.backend.dto.PaymentUrlResponse;
import es.us.meerkat.backend.dto.ReservaClaseDetailResponse;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.security.AuthUtils;
import es.us.meerkat.backend.service.DisponibilidadService;
import es.us.meerkat.backend.service.ReservaClaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Controlador para gestionar reservas de clases directas con tutores. */
@RestController
@RequestMapping("/api/v1")
@Tag(
        name = "Reservas de Clases",
        description = "Gestión de reservas de clases directas con tutores")
@RequiredArgsConstructor
public class ReservaClaseController {

    private final ReservaClaseService reservaService;
    private final DisponibilidadService disponibilidadService;
    private final AuthUtils authUtils;

    // ==================== DISPONIBILIDAD DEL TUTOR ====================

    @PostMapping("/tutors/me/disponibilidad")
    @Operation(
            summary = "Crear nueva disponibilidad horaria",
            description = "El tutor crea una franja horaria disponible para reservas")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Disponibilidad creada"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "No eres tutor")
    })
    public ResponseEntity<DisponibilidadTutorResponse> crearDisponibilidad(
            @Valid @RequestBody CreateDisponibilidadRequest request,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        Long tutorId = authUtils.getTutorId(usuario);
        DisponibilidadTutorResponse response =
                disponibilidadService.crearDisponibilidad(tutorId, request, usuario.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/tutors/me/disponibilidad")
    @Operation(
            summary = "Obtener mis disponibilidades",
            description = "Lista todas las disponibilidades horarias activas del tutor")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de disponibilidades"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "No eres tutor")
    })
    public ResponseEntity<List<DisponibilidadTutorResponse>> getMisDisponibilidades(
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        Long tutorId = authUtils.getTutorId(usuario);
        List<DisponibilidadTutorResponse> disponibilidades =
                disponibilidadService.getDisponibilidades(tutorId);
        return ResponseEntity.ok(disponibilidades);
    }

    @GetMapping("/tutors/{tutorId}/disponibilidad")
    @Operation(
            summary = "Obtener disponibilidades de un tutor",
            description = "Lista las disponibilidades activas de un tutor específico")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de disponibilidades"),
        @ApiResponse(responseCode = "404", description = "Tutor no encontrado")
    })
    public ResponseEntity<List<DisponibilidadTutorResponse>> getDisponibilidadesTutor(
            @Parameter(description = "ID del tutor") @PathVariable Long tutorId) {

        List<DisponibilidadTutorResponse> disponibilidades =
                disponibilidadService.getDisponibilidades(tutorId);
        return ResponseEntity.ok(disponibilidades);
    }

    @GetMapping("/tutors/{tutorId}/disponibilidad-fecha")
    @Operation(
            summary = "Obtener disponibilidades para una fecha",
            description = "Lista las disponibilidades de un tutor para una fecha específica")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de disponibilidades"),
        @ApiResponse(responseCode = "404", description = "Tutor no encontrado")
    })
    public ResponseEntity<List<DisponibilidadTutorResponse>> getDisponibilidadesPorFecha(
            @Parameter(description = "ID del tutor") @PathVariable Long tutorId,
            @Parameter(description = "Fecha (YYYY-MM-DD)") @RequestParam String fecha) {

        LocalDate fechaParsed = LocalDate.parse(fecha);
        List<DisponibilidadTutorResponse> disponibilidades =
                disponibilidadService.getDisponibilidadesPorFecha(tutorId, fechaParsed);
        return ResponseEntity.ok(disponibilidades);
    }

    @PutMapping("/disponibilidad/{disponibilidadId}")
    @Operation(
            summary = "Actualizar disponibilidad",
            description = "Modifica una franja horaria existente")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Disponibilidad actualizada"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "No tienes permisos"),
        @ApiResponse(responseCode = "404", description = "Disponibilidad no encontrada")
    })
    public ResponseEntity<DisponibilidadTutorResponse> actualizarDisponibilidad(
            @Parameter(description = "ID de la disponibilidad") @PathVariable Long disponibilidadId,
            @Valid @RequestBody CreateDisponibilidadRequest request,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        DisponibilidadTutorResponse response =
                disponibilidadService.actualizarDisponibilidad(
                        disponibilidadId, request, usuario.getId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/disponibilidad/{disponibilidadId}")
    @Operation(summary = "Desactivar disponibilidad", description = "Desactiva una franja horaria")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Disponibilidad desactivada"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "No tienes permisos"),
        @ApiResponse(responseCode = "404", description = "Disponibilidad no encontrada")
    })
    public ResponseEntity<Void> desactivarDisponibilidad(
            @Parameter(description = "ID de la disponibilidad") @PathVariable Long disponibilidadId,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        disponibilidadService.desactivarDisponibilidad(disponibilidadId, usuario.getId());
        return ResponseEntity.noContent().build();
    }

    // ==================== RESERVAS DE CLASES ====================

    @PostMapping("/tutors/{tutorId}/reservas")
    @Operation(
            summary = "Crear reserva de clase",
            description = "Un alumno crea una nueva reserva con un tutor")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "Reserva creada, URL de pago en respuesta"),
        @ApiResponse(
                responseCode = "400",
                description = "Datos inválidos o conflicto de disponibilidad"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "404", description = "Tutor no encontrado"),
        @ApiResponse(
                responseCode = "409",
                description = "El tutor no está disponible en ese horario")
    })
    public ResponseEntity<PaymentUrlResponse> crearReserva(
            @Parameter(description = "ID del tutor") @PathVariable Long tutorId,
            @Valid @RequestBody CreateReservaClaseRequest request,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        PaymentUrlResponse paymentUrl =
                reservaService.crearReserva(tutorId, request, usuario.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentUrl);
    }

    @PutMapping("/reservas/{reservaId}/confirmar")
    @Operation(
            summary = "Confirmar reserva",
            description = "El tutor confirma una reserva pendiente")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reserva confirmada"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "No eres el tutor de esta reserva"),
        @ApiResponse(responseCode = "404", description = "Reserva no encontrada"),
        @ApiResponse(responseCode = "409", description = "La reserva no está en estado PENDIENTE")
    })
    public ResponseEntity<ReservaClaseDetailResponse> confirmarReserva(
            @Parameter(description = "ID de la reserva") @PathVariable Long reservaId,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        Long tutorId = authUtils.getTutorId(usuario);
        ReservaClaseDetailResponse response = reservaService.confirmarReserva(reservaId, tutorId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/reservas/{reservaId}")
    @Operation(summary = "Cancelar reserva", description = "Cancela una reserva (alumno o tutor)")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Reserva cancelada"),
        @ApiResponse(responseCode = "400", description = "La clase es muy pronto para cancelar"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "No tienes permisos para cancelar"),
        @ApiResponse(responseCode = "404", description = "Reserva no encontrada")
    })
    public ResponseEntity<Void> cancelarReserva(
            @Parameter(description = "ID de la reserva") @PathVariable Long reservaId,
            @Parameter(description = "Motivo de cancelación") @RequestParam(required = false)
                    String motivo,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        reservaService.cancelarReserva(reservaId, usuario.getId(), motivo);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reservas/{reservaId}/calificar")
    @Operation(
            summary = "Calificar clase completada",
            description = "El alumno califica y comenta la clase")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Clase calificada"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "Solo el alumno puede calificar"),
        @ApiResponse(responseCode = "404", description = "Reserva no encontrada")
    })
    public ResponseEntity<ReservaClaseDetailResponse> calificarReserva(
            @Parameter(description = "ID de la reserva") @PathVariable Long reservaId,
            @Valid @RequestBody CalificarReservaRequest request,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        ReservaClaseDetailResponse response =
                reservaService.calificarReserva(reservaId, request, usuario.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reservas/me")
    @Operation(
            summary = "Mis reservas como alumno",
            description = "Lista todas mis reservas como alumno")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de reservas paginada"),
        @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public ResponseEntity<Page<ReservaClaseDetailResponse>> getMisReservas(
            @Parameter(description = "Número de página (0-indexed)")
                    @RequestParam(defaultValue = "0")
                    int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "20")
                    int size,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<ReservaClaseDetailResponse> respuestas =
                reservaService.getMisReservasAlumno(usuario.getId(), pageable);
        return ResponseEntity.ok(respuestas);
    }

    @GetMapping("/tutors/me/reservas")
    @Operation(
            summary = "Mis reservas como tutor",
            description = "Lista todas mis reservas como tutor")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de reservas paginada"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "No eres tutor")
    })
    public ResponseEntity<Page<ReservaClaseDetailResponse>> getMisReservasTutor(
            @Parameter(description = "Número de página (0-indexed)")
                    @RequestParam(defaultValue = "0")
                    int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "20")
                    int size,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        Long tutorId = authUtils.getTutorId(usuario);
        Pageable pageable = PageRequest.of(page, size);
        Page<ReservaClaseDetailResponse> respuestas =
                reservaService.getMisReservasTutor(tutorId, pageable);
        return ResponseEntity.ok(respuestas);
    }

    @GetMapping("/reservas/{reservaId}")
    @Operation(
            summary = "Obtener detalle de reserva",
            description = "Obtiene toda la información de una reserva específica")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Detalles de la reserva"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(
                responseCode = "403",
                description = "No tienes permisos para ver esta reserva"),
        @ApiResponse(responseCode = "404", description = "Reserva no encontrada")
    })
    public ResponseEntity<ReservaClaseDetailResponse> getReservaDetalle(
            @Parameter(description = "ID de la reserva") @PathVariable Long reservaId,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        ReservaClaseDetailResponse response =
                reservaService.getReservaDetalle(reservaId, usuario.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tutors/me/proximas-reservas")
    @Operation(
            summary = "Próximas reservas del tutor",
            description = "Obtiene las próximas clases confirmadas",
            parameters = {@Parameter(name = "dias", description = "Días a anticipar (default 7)")})
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de próximas reservas"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "No eres tutor")
    })
    public ResponseEntity<List<ReservaClaseDetailResponse>> getProximasReservas(
            @Parameter(description = "Días a anticipar") @RequestParam(defaultValue = "7")
                    Integer dias,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        Long tutorId = authUtils.getTutorId(usuario);
        List<ReservaClaseDetailResponse> respuestas =
                reservaService.getProximasReservasTutor(tutorId, dias);
        return ResponseEntity.ok(respuestas);
    }
}
