package es.us.meerkat.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.us.meerkat.backend.dto.DisponibilidadTutorResponse;
import es.us.meerkat.backend.dto.HorarioOcupadoResponse;
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

/**
 * Servicio auxiliar que determina si la fecha/hora de una solicitud ya ha pasado. Compara el día +
 * hora de inicio con el momento actual.
 */
// Helper inline, kept as a static utility to avoid pulling in more dependencies

/** Servicio para gestionar las solicitudes de contratación directa alumno-tutor. */
@Slf4j
@Service
@RequiredArgsConstructor
public class SolicitudContratacionService {

    private static final Set<String> MODALIDADES_VALIDAS = Set.of("ONLINE", "PRESENCIAL");

    private final SolicitudContratacionDirectaRepository solicitudRepository;
    private final TutorRepository tutorRepository;
    private final UsuarioRepository usuarioRepository;
    private final SimpMessagingTemplate broker;
    private final EmailService emailService;
    private final GoogleCalendarService googleCalendarService;
    private final DisponibilidadService disponibilidadService;
    private final PaymentService paymentService;
    private final ZoomIntegrationService zoomIntegrationService;

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
                    "Modalidad no válida. Valores permitidos: ONLINE, PRESENCIAL");
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

        // Validar que el horario solicitado esté dentro de la disponibilidad del tutor
        validarDisponibilidadTutor(
                tutor.getId(), request.getDia(), request.getHoraInicio(), request.getHoraFin());

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
                        .diaOriginal(request.getDia())
                        .horaInicio(request.getHoraInicio())
                        .horaFin(request.getHoraFin())
                        .tarifaHora(tarifaHora)
                        .importeTotal(importeTotal)
                        .modalidad(modalidad)
                        .mensaje(request.getMensaje())
                        .ubicacionClase(request.getUbicacionClase())
                        .estado(EstadoSolicitudContratacion.PENDIENTE)
                        .createdAt(LocalDateTime.now())
                        .build();

        solicitudRepository.save(solicitud);

        SolicitudContratacionResponse response = mapToResponse(solicitud);

        // Notificar al tutor por WebSocket
        broker.convertAndSendToUser(
                tutor.getUsuario().getId().toString(), "/queue/solicitud_contratacion", response);

        return response;
    }

    /**
     * Reserva directa desde el perfil del alumno: crea la solicitud en estado ACEPTADA y bloquea la
     * franja (sin pasar por PENDIENTE). Sincroniza con Google Calendar si procede.
     */
    @Transactional
    public SolicitudContratacionResponse reservarDirecta(
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

        // Comprobar conflictos de horario (evitar dobles reservas) — considerar cualquier estado
        List<SolicitudContratacionDirecta> conflictos =
                solicitudRepository.findConflictingBookingsAnyState(
                        tutor.getId(),
                        request.getDia(),
                        request.getHoraInicio(),
                        request.getHoraFin());
        if (!conflictos.isEmpty()) {
            throw new IllegalArgumentException(
                    "El profesor ya tiene una reserva en ese horario. Elige otro momento.");
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
                        .diaOriginal(request.getDia())
                        .horaInicio(request.getHoraInicio())
                        .horaFin(request.getHoraFin())
                        .tarifaHora(tarifaHora)
                        .importeTotal(importeTotal)
                        .modalidad(modalidad)
                        .mensaje(request.getMensaje())
                        .estado(EstadoSolicitudContratacion.ACEPTADA)
                        .createdAt(LocalDateTime.now())
                        .build();

        solicitudRepository.save(solicitud);

        SolicitudContratacionResponse response = mapToResponse(solicitud);

        // Notificar al tutor por WebSocket
        broker.convertAndSendToUser(
                tutor.getUsuario().getId().toString(), "/queue/solicitud_contratacion", response);

        // Sincronizar con Google Calendar (si alumno / tutor tienen Calendar conectado y activado)
        try {
            googleCalendarService.sincronizarBookingParaUsuario(solicitud, alumno);
            googleCalendarService.sincronizarBookingParaUsuario(solicitud, tutor.getUsuario());
        } catch (Exception e) {
            log.warn("Error al sincronizar Google Calendar (reserva directa): {}", e.getMessage());
        }

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
                solicitud.getAlumno().getId().toString(),
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
                solicitud.getAlumno().getId().toString(),
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
    public SolicitudContratacionResponse marcarComoPagada(
            Long solicitudId, Long alumnoId, String stripePaymentIntentId) {

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

        // Validar que la fecha de la clase no haya pasado
        LocalDateTime fechaHoraClase =
                LocalDateTime.of(solicitud.getDia(), solicitud.getHoraInicio());
        if (fechaHoraClase.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "La fecha de la clase ya ha pasado. No se puede realizar el pago.");
        }

        solicitud.setEstado(EstadoSolicitudContratacion.PAGADA);
        solicitud.setStripePaymentIntentId(stripePaymentIntentId);
        solicitudRepository.save(solicitud);
        try {
            googleCalendarService.sincronizarBookingParaUsuario(solicitud, solicitud.getAlumno());
            googleCalendarService.sincronizarBookingParaUsuario(
                    solicitud, solicitud.getTutor().getUsuario());
        } catch (Exception e) {
            log.warn("Error al sincronizar Google Calendar: {}", e.getMessage());
        }

        SolicitudContratacionResponse response = mapToResponse(solicitud);

        // Notificar al tutor que el pago se completó
        broker.convertAndSendToUser(
                solicitud.getTutor().getUsuario().getId().toString(),
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

        // Validar que la fecha de la clase no haya pasado
        LocalDateTime fechaHoraClase =
                LocalDateTime.of(solicitud.getDia(), solicitud.getHoraInicio());
        if (fechaHoraClase.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "La fecha de la clase ya ha pasado. No se puede realizar el pago.");
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

        // Si está pagada, reembolsar al alumno
        if (solicitud.getEstado() == EstadoSolicitudContratacion.PAGADA) {
            try {
                paymentService.reembolsarPago(
                        solicitud.getStripePaymentIntentId(),
                        solicitud.getAlumno(),
                        solicitud.getTutor(),
                        solicitud.getImporteTotal());
            } catch (Exception e) {
                log.error(
                        "Error al reembolsar pago de solicitud {}: {}",
                        solicitudId,
                        e.getMessage());
            }
        }

        solicitud.setEstado(EstadoSolicitudContratacion.CANCELADA_TUTOR);
        solicitud.setMotivoRechazo(motivo);
        solicitudRepository.save(solicitud);

        // Desincronizar Google Calendar si estaba sincronizado
        try {
            googleCalendarService.desincronizarBooking(solicitud);
        } catch (Exception e) {
            log.warn("Error al desincronizar Google Calendar: {}", e.getMessage());
        }

        SolicitudContratacionResponse response = mapToResponse(solicitud);

        // Notificar al alumno por WebSocket
        broker.convertAndSendToUser(
                solicitud.getAlumno().getId().toString(),
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
     * El tutor reprograma una reserva. Si ACEPTADA: cambio directo. Si PAGADA: solicitud al alumno.
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

        // La nueva fecha no puede ser en el pasado
        if (nuevoDia.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("No se puede reprogramar a una fecha pasada");
        }

        // Validaciones específicas para reservas PAGADAS
        if (solicitud.getEstado() == EstadoSolicitudContratacion.PAGADA) {
            if (nuevoDia.isBefore(solicitud.getDia())) {
                throw new IllegalArgumentException(
                        "Las reservas pagadas no se pueden adelantar. "
                                + "Solo se puede aplazar la fecha de la clase.");
            }
            if (nuevoDia.isAfter(solicitud.getDia().plusDays(2))) {
                throw new IllegalArgumentException(
                        "Solo se puede aplazar un máximo de 2 días desde la fecha actual"
                                + " de la clase.");
            }
            long duracionOriginal =
                    Duration.between(solicitud.getHoraInicio(), solicitud.getHoraFin()).toMinutes();
            long duracionNueva = Duration.between(nuevaHoraInicio, nuevaHoraFin).toMinutes();
            if (duracionNueva != duracionOriginal) {
                throw new IllegalArgumentException(
                        "No se puede cambiar la duración de una clase ya pagada. "
                                + "La clase debe mantener su duración original de "
                                + duracionOriginal
                                + " minutos.");
            }
        }

        // Validar disponibilidad del tutor para la nueva fecha/hora
        validarDisponibilidadTutor(
                solicitud.getTutor().getId(), nuevoDia, nuevaHoraInicio, nuevaHoraFin);

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

        // En lugar de sobreescribir la fecha y hora original directamente, guardamos
        // la propuesta en los campos de reprogramación y pasamos al estado pendiente para
        // que el alumno lo acepte explícitamente.
        solicitud.setEstadoAnterior(solicitud.getEstado());
        solicitud.setEstado(EstadoSolicitudContratacion.REPROGRAMACION_PENDIENTE);
        solicitud.setReprogramacionDia(nuevoDia);
        solicitud.setReprogramacionHoraInicio(nuevaHoraInicio);
        solicitud.setReprogramacionHoraFin(nuevaHoraFin);

        solicitudRepository.save(solicitud);

        SolicitudContratacionResponse response = mapToResponse(solicitud);

        // Notificar al alumno por WebSocket
        broker.convertAndSendToUser(
                solicitud.getAlumno().getId().toString(),
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

    /** El alumno aprueba la reprogramación propuesta por el tutor. */
    @Transactional
    public SolicitudContratacionResponse aprobarReprogramacion(Long solicitudId, Long alumnoId) {
        SolicitudContratacionDirecta solicitud =
                solicitudRepository
                        .findById(solicitudId)
                        .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));

        if (!solicitud.getAlumno().getId().equals(alumnoId)) {
            throw new IllegalArgumentException("No tienes permiso para esta operación");
        }

        if (solicitud.getEstado() != EstadoSolicitudContratacion.REPROGRAMACION_PENDIENTE) {
            throw new IllegalArgumentException(
                    "No hay reprogramación pendiente para esta solicitud");
        }

        // Aplicar los nuevos datos
        LocalDate diaAnterior = solicitud.getDia();
        LocalTime horaInicioAnterior = solicitud.getHoraInicio();
        LocalTime horaFinAnterior = solicitud.getHoraFin();

        solicitud.setDia(solicitud.getReprogramacionDia());
        solicitud.setHoraInicio(solicitud.getReprogramacionHoraInicio());
        solicitud.setHoraFin(solicitud.getReprogramacionHoraFin());

        // Recalcular importe
        long minutos =
                Duration.between(solicitud.getHoraInicio(), solicitud.getHoraFin()).toMinutes();
        BigDecimal horas =
                BigDecimal.valueOf(minutos).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        solicitud.setImporteTotal(
                solicitud.getTarifaHora().multiply(horas).setScale(2, RoundingMode.HALF_UP));

        // Limpiar campos de reprogramación y restaurar estado
        solicitud.setReprogramacionDia(null);
        solicitud.setReprogramacionHoraInicio(null);
        solicitud.setReprogramacionHoraFin(null);
        solicitud.setEstado(
                solicitud.getEstadoAnterior() != null
                        ? solicitud.getEstadoAnterior()
                        : EstadoSolicitudContratacion.PAGADA);
        solicitud.setEstadoAnterior(null);
        solicitudRepository.save(solicitud);

        SolicitudContratacionResponse response = mapToResponse(solicitud);

        // Notificar al tutor
        broker.convertAndSendToUser(
                solicitud.getTutor().getUsuario().getId().toString(),
                "/queue/solicitud_contratacion_respuesta",
                response);

        // Enviar email
        try {
            emailService.sendBookingRescheduledEmail(
                    solicitud.getAlumno().getEmail(),
                    solicitud.getAlumno().getNombre(),
                    solicitud.getTutor().getUsuario().getNombre(),
                    diaAnterior,
                    horaInicioAnterior,
                    horaFinAnterior,
                    solicitud.getDia(),
                    solicitud.getHoraInicio(),
                    solicitud.getHoraFin());
        } catch (Exception e) {
            log.warn("No se pudo enviar email de reprogramación: {}", e.getMessage());
        }

        return response;
    }

    /** El alumno rechaza la reprogramación propuesta por el tutor. */
    @Transactional
    public SolicitudContratacionResponse rechazarReprogramacion(Long solicitudId, Long alumnoId) {
        SolicitudContratacionDirecta solicitud =
                solicitudRepository
                        .findById(solicitudId)
                        .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));

        if (!solicitud.getAlumno().getId().equals(alumnoId)) {
            throw new IllegalArgumentException("No tienes permiso para esta operación");
        }

        if (solicitud.getEstado() != EstadoSolicitudContratacion.REPROGRAMACION_PENDIENTE) {
            throw new IllegalArgumentException(
                    "No hay reprogramación pendiente para esta solicitud");
        }

        // Limpiar campos de reprogramación y restaurar estado
        solicitud.setReprogramacionDia(null);
        solicitud.setReprogramacionHoraInicio(null);
        solicitud.setReprogramacionHoraFin(null);
        solicitud.setEstado(
                solicitud.getEstadoAnterior() != null
                        ? solicitud.getEstadoAnterior()
                        : EstadoSolicitudContratacion.PAGADA);
        solicitud.setEstadoAnterior(null);
        solicitudRepository.save(solicitud);

        SolicitudContratacionResponse response = mapToResponse(solicitud);

        // Notificar al tutor
        broker.convertAndSendToUser(
                solicitud.getTutor().getUsuario().getId().toString(),
                "/queue/solicitud_contratacion_respuesta",
                response);

        return response;
    }

    /** El alumno cancela una solicitud pagada o aceptada (con regla de 24h si pagada). */
    @Transactional
    public SolicitudContratacionResponse cancelarPorAlumno(
            Long solicitudId, Long alumnoId, String motivo) {

        SolicitudContratacionDirecta solicitud =
                solicitudRepository
                        .findById(solicitudId)
                        .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));

        if (!solicitud.getAlumno().getId().equals(alumnoId)) {
            throw new IllegalArgumentException("No tienes permiso para esta operación");
        }

        if (solicitud.getEstado() != EstadoSolicitudContratacion.PAGADA
                && solicitud.getEstado() != EstadoSolicitudContratacion.ACEPTADA
                && solicitud.getEstado() != EstadoSolicitudContratacion.PENDIENTE) {
            throw new IllegalArgumentException(
                    "Esta solicitud no se puede cancelar en su estado actual");
        }

        // Si está pagada, verificar regla de 24h
        if (solicitud.getEstado() == EstadoSolicitudContratacion.PAGADA) {
            if (!solicitud.puedeSerCanceladaPorAlumno()) {
                throw new IllegalArgumentException(
                        "No puedes cancelar con menos de 24 horas de antelación. "
                                + "Contacta directamente con el tutor.");
            }
            // Reembolsar el pago
            try {
                paymentService.reembolsarPago(
                        solicitud.getStripePaymentIntentId(),
                        solicitud.getAlumno(),
                        solicitud.getTutor(),
                        solicitud.getImporteTotal());
            } catch (Exception e) {
                log.error(
                        "Error al reembolsar pago de solicitud {}: {}",
                        solicitudId,
                        e.getMessage());
                throw new IllegalArgumentException(
                        "Error al procesar el reembolso. Inténtalo de nuevo.");
            }
        }

        solicitud.setEstado(EstadoSolicitudContratacion.CANCELADA_ALUMNO);
        solicitud.setMotivoRechazo(motivo);
        solicitudRepository.save(solicitud);

        // Desincronizar Google Calendar
        try {
            googleCalendarService.desincronizarBooking(solicitud);
        } catch (Exception e) {
            log.warn("Error al desincronizar Google Calendar: {}", e.getMessage());
        }

        SolicitudContratacionResponse response = mapToResponse(solicitud);

        // Notificar al tutor
        broker.convertAndSendToUser(
                solicitud.getTutor().getUsuario().getId().toString(),
                "/queue/solicitud_contratacion_respuesta",
                response);

        // Enviar email de confirmación al alumno
        try {
            emailService.sendAlumnoCancelledConfirmationEmail(
                    solicitud.getAlumno().getEmail(),
                    solicitud.getAlumno().getNombre(),
                    solicitud.getTutor().getUsuario().getNombre(),
                    solicitud.getDia(),
                    solicitud.getHoraInicio(),
                    solicitud.getHoraFin(),
                    motivo);
        } catch (Exception e) {
            log.warn("No se pudo enviar email de confirmación al alumno: {}", e.getMessage());
        }

        // Enviar email de notificación al tutor
        try {
            emailService.sendTutorNotificationAlumnoCancelledEmail(
                    solicitud.getTutor().getUsuario().getEmail(),
                    solicitud.getTutor().getUsuario().getNombre(),
                    solicitud.getAlumno().getNombre(),
                    solicitud.getDia(),
                    solicitud.getHoraInicio(),
                    solicitud.getHoraFin(),
                    motivo);
        } catch (Exception e) {
            log.warn("No se pudo enviar email de cancelación al tutor: {}", e.getMessage());
        }

        return response;
    }

    /** Califica una clase completada (alumno). */
    @Transactional
    public SolicitudContratacionResponse calificarSolicitud(
            Long solicitudId, Long alumnoId, Integer calificacion, String comentario) {

        SolicitudContratacionDirecta solicitud =
                solicitudRepository
                        .findById(solicitudId)
                        .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));

        if (!solicitud.getAlumno().getId().equals(alumnoId)) {
            throw new IllegalArgumentException("Solo el alumno puede calificar esta clase");
        }

        // Se puede calificar si el estado es COMPLETADA o si ya pasó la clase y está PAGADA
        if (solicitud.getEstado() != EstadoSolicitudContratacion.COMPLETADA
                && !(solicitud.getEstado() == EstadoSolicitudContratacion.PAGADA
                        && solicitud.yaEnPasado())) {
            throw new IllegalArgumentException(
                    "Solo se puede calificar una clase completada o ya pasada");
        }

        if (calificacion == null || calificacion < 1 || calificacion > 5) {
            throw new IllegalArgumentException("La calificación debe estar entre 1 y 5");
        }

        solicitud.setCalificacion(calificacion);
        solicitud.setComentarioAlumno(comentario);
        solicitud.setEstado(EstadoSolicitudContratacion.COMPLETADA);
        solicitudRepository.save(solicitud);

        return mapToResponse(solicitud);
    }

    /**
     * Obtiene los horarios ocupados de un tutor para una fecha (franjas con contrataciones
     * activas).
     */
    @Transactional(readOnly = true)
    public List<HorarioOcupadoResponse> getHorariosOcupados(Long tutorId, LocalDate fecha) {
        List<SolicitudContratacionDirecta> activas =
                solicitudRepository.findActiveBookingsByTutorAndDate(tutorId, fecha);
        return activas.stream()
                .map(
                        s ->
                                new HorarioOcupadoResponse(
                                        s.getHoraInicio().toString(), s.getHoraFin().toString()))
                .toList();
    }

    /** Valida que el rango horario solicitado caiga dentro de la disponibilidad del tutor. */
    private void validarDisponibilidadTutor(
            Long tutorId, LocalDate dia, LocalTime horaInicio, LocalTime horaFin) {
        List<DisponibilidadTutorResponse> disponibilidades =
                disponibilidadService.getDisponibilidadesPorFecha(tutorId, dia);

        if (disponibilidades.isEmpty()) {
            throw new IllegalArgumentException(
                    "El tutor no tiene disponibilidad configurada para el día seleccionado ("
                            + dia.getDayOfWeek()
                            + ").");
        }

        boolean dentroDeDisponibilidad =
                disponibilidades.stream()
                        .anyMatch(
                                d ->
                                        !horaInicio.isBefore(d.getHoraInicio())
                                                && !horaFin.isAfter(d.getHoraFin()));

        if (!dentroDeDisponibilidad) {
            throw new IllegalArgumentException(
                    "El horario seleccionado no está dentro de la disponibilidad del tutor. "
                            + "Consulta sus franjas horarias disponibles.");
        }
    }

    /**
     * Crea una reunión Zoom para una clase ONLINE ya pagada. Solo el tutor puede crear la reunión.
     */
    @Transactional
    public SolicitudContratacionResponse crearZoomParaClase(Long solicitudId, Long tutorUsuarioId) {

        SolicitudContratacionDirecta solicitud =
                solicitudRepository
                        .findById(solicitudId)
                        .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));

        if (!solicitud.getTutor().getUsuario().getId().equals(tutorUsuarioId)) {
            throw new IllegalArgumentException("No tienes permiso para esta operación");
        }

        if (solicitud.getEstado() != EstadoSolicitudContratacion.PAGADA) {
            throw new IllegalArgumentException(
                    "Solo se puede crear reunión Zoom para clases pagadas");
        }

        if (!"ONLINE".equalsIgnoreCase(solicitud.getModalidad())) {
            throw new IllegalArgumentException(
                    "Solo se puede crear reunión Zoom para clases online");
        }

        if (solicitud.getZoomJoinUrl() != null) {
            return mapToResponse(solicitud);
        }

        long durationMinutes =
                Duration.between(solicitud.getHoraInicio(), solicitud.getHoraFin()).toMinutes();

        String topic =
                "Clase con " + solicitud.getAlumno().getNombre() + " – " + solicitud.getDia();

        Map<String, Object> zoom =
                zoomIntegrationService.crearReunionSimple(topic, (int) durationMinutes);

        solicitud.setZoomJoinUrl((String) zoom.get("join_url"));
        solicitud.setZoomStartUrl((String) zoom.get("start_url"));
        solicitudRepository.save(solicitud);

        SolicitudContratacionResponse response = mapToResponse(solicitud);

        // Notificar al alumno que el enlace Zoom está disponible
        broker.convertAndSendToUser(
                solicitud.getAlumno().getId().toString(),
                "/queue/solicitud_contratacion_respuesta",
                response);

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
                .diaOriginal(
                        solicitud.getDiaOriginal() != null
                                ? solicitud.getDiaOriginal()
                                : solicitud.getDia())
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
                .calificacion(solicitud.getCalificacion())
                .comentarioAlumno(solicitud.getComentarioAlumno())
                .puedeSerCanceladaPorAlumno(solicitud.puedeSerCanceladaPorAlumno())
                .reprogramacionDia(
                        solicitud.getReprogramacionDia() != null
                                ? solicitud.getReprogramacionDia().toString()
                                : null)
                .reprogramacionHoraInicio(
                        solicitud.getReprogramacionHoraInicio() != null
                                ? solicitud.getReprogramacionHoraInicio().toString()
                                : null)
                .reprogramacionHoraFin(
                        solicitud.getReprogramacionHoraFin() != null
                                ? solicitud.getReprogramacionHoraFin().toString()
                                : null)
                .estadoAnterior(
                        solicitud.getEstadoAnterior() != null
                                ? solicitud.getEstadoAnterior().name()
                                : null)
                .ubicacionClase(solicitud.getUbicacionClase())
                .zoomJoinUrl(solicitud.getZoomJoinUrl())
                .zoomStartUrl(solicitud.getZoomStartUrl())
                .createdAt(solicitud.getCreatedAt())
                .updatedAt(solicitud.getUpdatedAt())
                .build();
    }
}
