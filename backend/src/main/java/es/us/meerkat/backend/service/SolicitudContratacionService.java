package es.us.meerkat.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.us.meerkat.backend.dto.SolicitudContratacionRequest;
import es.us.meerkat.backend.dto.SolicitudContratacionResponse;
import es.us.meerkat.backend.entity.EstadoSolicitudContratacion;
import es.us.meerkat.backend.entity.SolicitudContratacionDirecta;
import es.us.meerkat.backend.entity.Tutor;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.SolicitudContratacionDirectaRepository;
import es.us.meerkat.backend.repository.TutorRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Servicio para gestionar las solicitudes de contratación directa alumno-tutor. */
@Slf4j
@Service
@RequiredArgsConstructor
public class SolicitudContratacionService {

    private static final Set<String> MODALIDADES_VALIDAS =
            Set.of("ONLINE", "PRESENCIAL", "HIBRIDO");

    private final SolicitudContratacionDirectaRepository solicitudRepository;
    private final TutorRepository tutorRepository;
    private final UsuarioRepository usuarioRepository;
    private final SimpMessagingTemplate broker;
    private final EmailService emailService;

    /**
     * Crea una solicitud de contratación directa.
     *
     * @param alumnoId ID del alumno que solicita
     * @param tutorId ID del tutor solicitado
     * @param request datos de la solicitud
     * @return respuesta con los datos de la solicitud creada
     */
    @Transactional
    public SolicitudContratacionResponse crearSolicitud(
            Long alumnoId, Long tutorId, SolicitudContratacionRequest request) {

        Usuario alumno =
                usuarioRepository
                        .findById(alumnoId)
                        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Tutor tutor =
                tutorRepository
                        .findById(tutorId)
                        .orElseThrow(() -> new IllegalArgumentException("Tutor no encontrado"));

        if (tutor.getUsuario().getId().equals(alumnoId)) {
            throw new IllegalArgumentException("No puedes contratarte a ti mismo");
        }

        if (!Boolean.TRUE.equals(tutor.getVerificado())) {
            throw new IllegalArgumentException(
                    "El tutor debe estar verificado para ser contratado");
        }

        if (!request.getHoraFin().isAfter(request.getHoraInicio())) {
            throw new IllegalArgumentException(
                    "La hora de fin debe ser posterior a la hora de inicio");
        }

        // Validar modalidad
        String modalidad =
                request.getModalidad() != null ? request.getModalidad().toUpperCase() : "ONLINE";
        if (!MODALIDADES_VALIDAS.contains(modalidad)) {
            throw new IllegalArgumentException(
                    "Modalidad no válida. Valores permitidos: ONLINE, PRESENCIAL, HIBRIDO");
        }

        // Comprobar conflictos de horario (evitar dobles reservas)
        List<SolicitudContratacionDirecta> conflictos =
                solicitudRepository.findConflictingBookings(
                        tutor.getId(),
                        request.getDia(),
                        request.getHoraInicio(),
                        request.getHoraFin());
        if (!conflictos.isEmpty()) {
            throw new IllegalArgumentException(
                    "El profesor ya tiene una reserva confirmada en ese horario. Elige otro"
                            + " momento.");
        }

        // Calcular importe total
        long minutos = Duration.between(request.getHoraInicio(), request.getHoraFin()).toMinutes();
        BigDecimal horas =
                BigDecimal.valueOf(minutos).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        BigDecimal tarifaHora = tutor.getTarifaHora();
        BigDecimal importeTotal = tarifaHora.multiply(horas).setScale(2, RoundingMode.HALF_UP);

        SolicitudContratacionDirecta solicitud =
                SolicitudContratacionDirecta.builder()
                        .alumno(alumno)
                        .tutor(tutor)
                        .dia(request.getDia())
                        .horaInicio(request.getHoraInicio())
                        .horaFin(request.getHoraFin())
                        .tarifaHora(tarifaHora)
                        .importeTotal(importeTotal)
                        .modalidad(modalidad)
                        .mensaje(request.getMensaje())
                        .estado(EstadoSolicitudContratacion.PENDIENTE)
                        .createdAt(LocalDateTime.now())
                        .build();

        solicitudRepository.save(solicitud);

        SolicitudContratacionResponse response = mapToResponse(solicitud);

        // Notificar al tutor por WebSocket
        broker.convertAndSendToUser(
                tutor.getUsuario().getEmail(), "/queue/solicitud_contratacion", response);

        return response;
    }

    /**
     * El tutor acepta una solicitud.
     *
     * @param solicitudId ID de la solicitud
     * @param tutorUsuarioId ID del usuario tutor
     * @return respuesta actualizada
     */
    @Transactional
    public SolicitudContratacionResponse aceptarSolicitud(Long solicitudId, Long tutorUsuarioId) {

        SolicitudContratacionDirecta solicitud =
                solicitudRepository
                        .findById(solicitudId)
                        .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));

        if (!solicitud.getTutor().getUsuario().getId().equals(tutorUsuarioId)) {
            throw new IllegalArgumentException("No tienes permiso para gestionar esta solicitud");
        }

        if (solicitud.getEstado() != EstadoSolicitudContratacion.PENDIENTE) {
            throw new IllegalArgumentException("La solicitud ya no está pendiente");
        }

        solicitud.setEstado(EstadoSolicitudContratacion.ACEPTADA);
        solicitudRepository.save(solicitud);

        // Comprobar conflictos al aceptar (otra solicitud ya aceptada/pagada en ese horario)
        List<SolicitudContratacionDirecta> conflictos =
                solicitudRepository.findConflictingBookingsExcluding(
                        solicitud.getTutor().getId(),
                        solicitud.getDia(),
                        solicitud.getHoraInicio(),
                        solicitud.getHoraFin(),
                        solicitud.getId());
        if (!conflictos.isEmpty()) {
            solicitud.setEstado(EstadoSolicitudContratacion.PENDIENTE);
            solicitudRepository.save(solicitud);
            throw new IllegalArgumentException(
                    "Ya tienes una reserva confirmada en ese horario. Rechaza esta o cancela la"
                            + " otra primero.");
        }

        SolicitudContratacionResponse response = mapToResponse(solicitud);

        // Notificar al alumno por WebSocket
        broker.convertAndSendToUser(
                solicitud.getAlumno().getEmail(),
                "/queue/solicitud_contratacion_respuesta",
                response);

        return response;
    }

    /**
     * El tutor rechaza una solicitud.
     *
     * @param solicitudId ID de la solicitud
     * @param tutorUsuarioId ID del usuario tutor
     * @param motivo motivo de rechazo
     * @return respuesta actualizada
     */
    @Transactional
    public SolicitudContratacionResponse rechazarSolicitud(
            Long solicitudId, Long tutorUsuarioId, String motivo) {

        SolicitudContratacionDirecta solicitud =
                solicitudRepository
                        .findById(solicitudId)
                        .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));

        if (!solicitud.getTutor().getUsuario().getId().equals(tutorUsuarioId)) {
            throw new IllegalArgumentException("No tienes permiso para gestionar esta solicitud");
        }

        if (solicitud.getEstado() != EstadoSolicitudContratacion.PENDIENTE) {
            throw new IllegalArgumentException("La solicitud ya no está pendiente");
        }

        solicitud.setEstado(EstadoSolicitudContratacion.RECHAZADA);
        solicitud.setMotivoRechazo(motivo);
        solicitudRepository.save(solicitud);

        SolicitudContratacionResponse response = mapToResponse(solicitud);

        // Notificar al alumno por WebSocket
        broker.convertAndSendToUser(
                solicitud.getAlumno().getEmail(),
                "/queue/solicitud_contratacion_respuesta",
                response);

        return response;
    }

    /**
     * Marca la solicitud como pagada.
     *
     * @param solicitudId ID de la solicitud
     * @param alumnoId ID del alumno
     * @return respuesta actualizada
     */
    @Transactional
    public SolicitudContratacionResponse marcarComoPagada(Long solicitudId, Long alumnoId) {

        SolicitudContratacionDirecta solicitud =
                solicitudRepository
                        .findById(solicitudId)
                        .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));

        if (!solicitud.getAlumno().getId().equals(alumnoId)) {
            throw new IllegalArgumentException("No tienes permiso para esta operación");
        }

        if (solicitud.getEstado() != EstadoSolicitudContratacion.ACEPTADA) {
            throw new IllegalArgumentException(
                    "La solicitud debe estar aceptada para poder pagarla");
        }

        solicitud.setEstado(EstadoSolicitudContratacion.PAGADA);
        solicitudRepository.save(solicitud);

        SolicitudContratacionResponse response = mapToResponse(solicitud);

        // Notificar al tutor que el pago se completó
        broker.convertAndSendToUser(
                solicitud.getTutor().getUsuario().getEmail(),
                "/queue/solicitud_contratacion_pagada",
                response);

        return response;
    }

    /** Obtiene las solicitudes pendientes del tutor. */
    @Transactional(readOnly = true)
    public List<SolicitudContratacionResponse> obtenerSolicitudesPendientesDelTutor(
            Long tutorUsuarioId) {
        Tutor tutor =
                tutorRepository
                        .findByUsuarioId(tutorUsuarioId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Perfil de tutor no encontrado"));

        return solicitudRepository.findPendientesByTutorId(tutor.getId()).stream()
                .map(this::mapToResponse)
                .toList();
    }

    /** Obtiene todas las solicitudes del tutor (todas los estados). */
    @Transactional(readOnly = true)
    public List<SolicitudContratacionResponse> obtenerSolicitudesDelTutor(Long tutorUsuarioId) {
        Tutor tutor =
                tutorRepository
                        .findByUsuarioId(tutorUsuarioId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Perfil de tutor no encontrado"));

        return solicitudRepository.findByTutorIdOrderByCreatedAtDesc(tutor.getId()).stream()
                .map(this::mapToResponse)
                .toList();
    }

    /** Obtiene las solicitudes realizadas por un alumno. */
    @Transactional(readOnly = true)
    public List<SolicitudContratacionResponse> obtenerSolicitudesDelAlumno(Long alumnoId) {
        return solicitudRepository.findByAlumnoIdOrderByCreatedAtDesc(alumnoId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Obtiene una solicitud aceptada para proceder al pago. Valida que el alumno sea el propietario
     * y que la solicitud esté aceptada.
     */
    @Transactional(readOnly = true)
    public SolicitudContratacionResponse obtenerSolicitudParaPago(Long solicitudId, Long alumnoId) {
        SolicitudContratacionDirecta solicitud =
                solicitudRepository
                        .findById(solicitudId)
                        .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));

        if (!solicitud.getAlumno().getId().equals(alumnoId)) {
            throw new IllegalArgumentException("No tienes permiso para esta operación");
        }

        if (solicitud.getEstado() != EstadoSolicitudContratacion.ACEPTADA) {
            throw new IllegalArgumentException(
                    "La solicitud debe estar aceptada para poder pagarla");
        }

        return mapToResponse(solicitud);
    }

    /**
     * El tutor cancela una reserva (ACEPTADA o PAGADA). Notifica automáticamente al alumno por
     * WebSocket y email.
     */
    @Transactional
    public SolicitudContratacionResponse cancelarSolicitud(
            Long solicitudId, Long tutorUsuarioId, String motivo) {

        SolicitudContratacionDirecta solicitud =
                solicitudRepository
                        .findById(solicitudId)
                        .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));

        if (!solicitud.getTutor().getUsuario().getId().equals(tutorUsuarioId)) {
            throw new IllegalArgumentException("No tienes permiso para gestionar esta solicitud");
        }

        if (solicitud.getEstado() != EstadoSolicitudContratacion.ACEPTADA
                && solicitud.getEstado() != EstadoSolicitudContratacion.PAGADA
                && solicitud.getEstado() != EstadoSolicitudContratacion.PENDIENTE) {
            throw new IllegalArgumentException(
                    "Esta solicitud no se puede cancelar en su estado actual");
        }

        solicitud.setEstado(EstadoSolicitudContratacion.CANCELADA);
        solicitud.setMotivoRechazo(motivo);
        solicitudRepository.save(solicitud);

        SolicitudContratacionResponse response = mapToResponse(solicitud);

        // Notificar al alumno por WebSocket
        broker.convertAndSendToUser(
                solicitud.getAlumno().getEmail(),
                "/queue/solicitud_contratacion_respuesta",
                response);

        // Notificar al alumno por email
        try {
            emailService.sendBookingCancellationEmail(
                    solicitud.getAlumno().getEmail(),
                    solicitud.getAlumno().getNombre(),
                    solicitud.getTutor().getUsuario().getNombre(),
                    solicitud.getDia(),
                    solicitud.getHoraInicio(),
                    solicitud.getHoraFin(),
                    motivo);
        } catch (Exception e) {
            log.warn("No se pudo enviar email de cancelación: {}", e.getMessage());
        }

        return response;
    }

    /**
     * El tutor reprograma una reserva (ACEPTADA o PAGADA). Cambia fecha/hora y notifica al alumno.
     */
    @Transactional
    public SolicitudContratacionResponse reprogramarSolicitud(
            Long solicitudId,
            Long tutorUsuarioId,
            LocalDate nuevoDia,
            LocalTime nuevaHoraInicio,
            LocalTime nuevaHoraFin) {

        SolicitudContratacionDirecta solicitud =
                solicitudRepository
                        .findById(solicitudId)
                        .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));

        if (!solicitud.getTutor().getUsuario().getId().equals(tutorUsuarioId)) {
            throw new IllegalArgumentException("No tienes permiso para gestionar esta solicitud");
        }

        if (solicitud.getEstado() != EstadoSolicitudContratacion.ACEPTADA
                && solicitud.getEstado() != EstadoSolicitudContratacion.PAGADA) {
            throw new IllegalArgumentException(
                    "Solo se pueden reprogramar reservas aceptadas o pagadas");
        }

        if (!nuevaHoraFin.isAfter(nuevaHoraInicio)) {
            throw new IllegalArgumentException(
                    "La hora de fin debe ser posterior a la hora de inicio");
        }

        // Si ya está pagada, la duración debe ser exactamente la misma
        if (solicitud.getEstado() == EstadoSolicitudContratacion.PAGADA) {
            long duracionOriginal =
                    Duration.between(solicitud.getHoraInicio(), solicitud.getHoraFin()).toMinutes();
            long duracionNueva = Duration.between(nuevaHoraInicio, nuevaHoraFin).toMinutes();
            if (duracionOriginal != duracionNueva) {
                throw new IllegalArgumentException(
                        "La reserva ya está pagada. La duración debe ser la misma ("
                                + duracionOriginal
                                + " min). Solo puedes cambiar el horario.");
            }
        }

        // Comprobar conflictos en el nuevo horario
        List<SolicitudContratacionDirecta> conflictos =
                solicitudRepository.findConflictingBookingsExcluding(
                        solicitud.getTutor().getId(),
                        nuevoDia,
                        nuevaHoraInicio,
                        nuevaHoraFin,
                        solicitud.getId());
        if (!conflictos.isEmpty()) {
            throw new IllegalArgumentException(
                    "Ya tienes una reserva confirmada en el nuevo horario.");
        }

        // Guardar datos anteriores para el email
        LocalDate diaAnterior = solicitud.getDia();
        LocalTime horaInicioAnterior = solicitud.getHoraInicio();
        LocalTime horaFinAnterior = solicitud.getHoraFin();

        // Recalcular importe
        long minutos = Duration.between(nuevaHoraInicio, nuevaHoraFin).toMinutes();
        BigDecimal horas =
                BigDecimal.valueOf(minutos).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        BigDecimal importeTotal =
                solicitud.getTarifaHora().multiply(horas).setScale(2, RoundingMode.HALF_UP);

        solicitud.setDia(nuevoDia);
        solicitud.setHoraInicio(nuevaHoraInicio);
        solicitud.setHoraFin(nuevaHoraFin);
        solicitud.setImporteTotal(importeTotal);
        solicitudRepository.save(solicitud);

        SolicitudContratacionResponse response = mapToResponse(solicitud);

        // Notificar al alumno por WebSocket
        broker.convertAndSendToUser(
                solicitud.getAlumno().getEmail(),
                "/queue/solicitud_contratacion_respuesta",
                response);

        // Notificar por email
        try {
            emailService.sendBookingRescheduledEmail(
                    solicitud.getAlumno().getEmail(),
                    solicitud.getAlumno().getNombre(),
                    solicitud.getTutor().getUsuario().getNombre(),
                    diaAnterior,
                    horaInicioAnterior,
                    horaFinAnterior,
                    nuevoDia,
                    nuevaHoraInicio,
                    nuevaHoraFin);
        } catch (Exception e) {
            log.warn("No se pudo enviar email de reprogramación: {}", e.getMessage());
        }

        return response;
    }

    private SolicitudContratacionResponse mapToResponse(SolicitudContratacionDirecta solicitud) {
        Usuario alumno = solicitud.getAlumno();
        Tutor tutor = solicitud.getTutor();
        Usuario tutorUsuario = tutor.getUsuario();

        return SolicitudContratacionResponse.builder()
                .id(solicitud.getId())
                .alumnoId(alumno.getId())
                .alumnoNombre(alumno.getNombre())
                .alumnoFoto(alumno.getFoto())
                .tutorId(tutor.getId())
                .tutorNombre(tutorUsuario.getNombre())
                .tutorFoto(tutorUsuario.getFoto())
                .dia(solicitud.getDia())
                .horaInicio(solicitud.getHoraInicio())
                .horaFin(solicitud.getHoraFin())
                .tarifaHora(solicitud.getTarifaHora())
                .importeTotal(solicitud.getImporteTotal())
                .modalidad(solicitud.getModalidad())
                .mensaje(solicitud.getMensaje())
                .estado(solicitud.getEstado().name())
                .motivoRechazo(solicitud.getMotivoRechazo())
                .tutorStripeConfigured(
                        tutor.getStripeAccountId() != null && !tutor.getStripeAccountId().isBlank())
                .createdAt(solicitud.getCreatedAt())
                .updatedAt(solicitud.getUpdatedAt())
                .build();
    }
}
