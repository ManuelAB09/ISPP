package es.us.meerkat.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.us.meerkat.backend.dto.CalificarReservaRequest;
import es.us.meerkat.backend.dto.CreateReservaClaseRequest;
import es.us.meerkat.backend.dto.HorarioOcupadoResponse;
import es.us.meerkat.backend.dto.PaymentUrlResponse;
import es.us.meerkat.backend.dto.ReservaClaseDetailResponse;
import es.us.meerkat.backend.entity.EstadoReserva;
import es.us.meerkat.backend.entity.ReservaClase;
import es.us.meerkat.backend.entity.Tutor;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.ReservaClaseRepository;
import es.us.meerkat.backend.repository.TutorRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Servicio para gestionar reservas de clases directas con tutores.
 *
 * <p>Maneja creación, confirmación, cancelación, calificación y seguimiento de reservas.
 */
@Service
@Slf4j
public class ReservaClaseService {

    @Autowired private ReservaClaseRepository reservaRepository;
    @Autowired private TutorRepository tutorRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PaymentService paymentService;
    @Autowired private JavaMailSender mailSender;
    @Autowired private DisponibilidadService disponibilidadService;

    /** Minutos de anticipación requeridos para cancelar una reserva */
    private static final Integer MINUTOS_ANTICIPACION_CANCELACION = 24 * 60;

    /**
     * Crea una nueva reserva de clase.
     *
     * @param tutorId ID del tutor
     * @param request Datos de la reserva
     * @param alumnoId ID del alumno que realiza la reserva
     * @return PaymentUrlResponse para that el alumno pueda pagar
     */
    @Transactional
    public PaymentUrlResponse crearReserva(
            Long tutorId, CreateReservaClaseRequest request, Long alumnoId) {

        // Validar tutor existe
        Tutor tutor =
                tutorRepository
                        .findById(tutorId)
                        .orElseThrow(() -> new IllegalArgumentException("Tutor no encontrado"));

        // Validar alumno existe
        Usuario alumno =
                usuarioRepository
                        .findById(alumnoId)
                        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // Validar que no sea reserva consigo mismo
        if (tutor.getUsuario().getId().equals(alumnoId)) {
            throw new IllegalArgumentException("No puedes reservar una clase contigo mismo");
        }

        // Validar que la fecha/hora sea en el futuro
        if (request.getFechaHora().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("La fecha de la clase debe ser en el futuro");
        }

        // Validar no haya solapamientos con otras reservas del tutor
        LocalDateTime fechaFin = request.getFechaHora().plusMinutes(request.getDuracionMinutos());
        List<ReservaClase> conflictos =
                reservaRepository.findReservasConflictivas(
                        tutorId, request.getFechaHora(), fechaFin);
        if (!conflictos.isEmpty()) {
            throw new IllegalArgumentException(
                    "El tutor no tiene disponibilidad en ese horario. Conflicto con otra reserva.");
        }

        // Crear la reserva
        ReservaClase reserva =
                ReservaClase.builder()
                        .alumno(alumno)
                        .tutor(tutor)
                        .fechaHora(request.getFechaHora())
                        .duracionMinutos(request.getDuracionMinutos())
                        .modalidad(request.getModalidad())
                        .ubicacion(request.getUbicacion())
                        .tema(request.getTema())
                        .descripcion(request.getDescripcion())
                        .tarifa(tutor.getTarifaHora())
                        .moneda("EUR")
                        .estado(EstadoReserva.PENDIENTE)
                        .build();

        reserva = reservaRepository.save(reserva);

        // Realizar cobro
        // TODO: Implementar integración con PaymentService.generarPagoReservaClase
        // Por ahora se devuelve una URL de pago simulada
        PaymentUrlResponse paymentUrl =
                new PaymentUrlResponse(
                        "https://checkout.stripe.com/pay/cs_test_" + reserva.getId(),
                        "session_" + reserva.getId());

        // Enviar email de notificación al tutor
        try {
            enviarEmailNuevaReserva(tutor.getUsuario(), alumno, reserva);
        } catch (Exception e) {
            log.warn("Error al enviar email de nueva reserva: {}", e.getMessage());
        }

        return paymentUrl;
    }

    /** Confirma una reserva pendiente (por parte del tutor). */
    @Transactional
    public ReservaClaseDetailResponse confirmarReserva(Long reservaId, Long tutorId) {

        ReservaClase reserva =
                reservaRepository
                        .findById(reservaId)
                        .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));

        // Verificar que el tutor sea el dueño
        if (!reserva.getTutor().getId().equals(tutorId)) {
            throw new IllegalArgumentException("No tienes permisos para confirmar esta reserva");
        }

        // Solo se puede confirmar si está en PENDIENTE
        if (!EstadoReserva.PENDIENTE.equals(reserva.getEstado())) {
            throw new IllegalArgumentException(
                    "Solo puedes confirmar reservas en estado PENDIENTE");
        }

        reserva.setEstado(EstadoReserva.CONFIRMADA);
        reserva.setFechaConfirmacion(LocalDateTime.now());
        reserva = reservaRepository.save(reserva);

        // Enviar email de confirmación al alumno
        enviarEmailConfirmacionReserva(reserva.getAlumno(), reserva);

        return mapToResponse(reserva);
    }

    /** Cancela una reserva. */
    @Transactional
    public void cancelarReserva(Long reservaId, Long usuarioId, String motivo) {

        ReservaClase reserva =
                reservaRepository
                        .findById(reservaId)
                        .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));

        // Verificar que sea el dueño (alumno o tutor)
        boolean esAlumno = reserva.getAlumno().getId().equals(usuarioId);
        boolean esTutor = reserva.getTutor().getUsuario().getId().equals(usuarioId);

        if (!esAlumno && !esTutor) {
            throw new IllegalArgumentException("No tienes permisos para cancelar esta reserva");
        }

        // No se puede cancelar si ya pasó
        if (reserva.yaEnPasado()) {
            throw new IllegalArgumentException("No se puede cancelar una reserva que ya pasó");
        }

        // Si es el alumno, verificar política de cancelación (24h anticipación)
        if (esAlumno && !reserva.puedeSerCancelada(MINUTOS_ANTICIPACION_CANCELACION)) {
            throw new IllegalArgumentException(
                    "Debe haber al menos 24 horas de anticipación para cancelar");
        }

        // Cancelar
        EstadoReserva nuevoEstado =
                esAlumno ? EstadoReserva.CANCELADA_ALUMNO : EstadoReserva.CANCELADA_TUTOR;
        reserva.setEstado(nuevoEstado);
        reserva.setMotivoCancelacion(motivo);
        reserva.setQuienCancelo(esAlumno ? "ALUMNO" : "TUTOR");

        reservaRepository.save(reserva);

        // Reembolsar si es necesario
        // TODO: Implementar integración con PaymentService.reembolsarPago
        // if (reserva.getTransaccion() != null) {
        //     try {
        //         paymentService.reembolsarPago(reserva.getTransaccion().getId());
        //     } catch (Exception e) {
        //         // Log error pero no fallar
        //         System.err.println("Error al reembolsar: " + e.getMessage());
        //     }
        // }

        // Notificar por email
        if (esAlumno) {
            enviarEmailCancelacionReserva(reserva.getTutor().getUsuario(), reserva, "alumno");
        } else {
            enviarEmailCancelacionReserva(reserva.getAlumno(), reserva, "tutor");
        }
    }

    /** Califica una clase completada. */
    @Transactional
    public ReservaClaseDetailResponse calificarReserva(
            Long reservaId, CalificarReservaRequest request, Long usuarioId) {

        ReservaClase reserva =
                reservaRepository
                        .findById(reservaId)
                        .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));

        // Verificar que sea el alumno
        if (!reserva.getAlumno().getId().equals(usuarioId)) {
            throw new IllegalArgumentException("Solo el alumno puede calificar esta reserva");
        }

        // Verificar que la clase ya pasó o está completada
        if (!reserva.yaEnPasado() && !EstadoReserva.COMPLETADA.equals(reserva.getEstado())) {
            throw new IllegalArgumentException("Solo se puede calificar una clase completada");
        }

        reserva.setCalificacion(request.getCalificacion());
        reserva.setComentarioAlumno(request.getComentario());
        reserva.setEstado(EstadoReserva.COMPLETADA);
        reserva = reservaRepository.save(reserva);

        return mapToResponse(reserva);
    }

    /** Obtiene las reservas de un alumno. */
    @Transactional(readOnly = true)
    public Page<ReservaClaseDetailResponse> getMisReservasAlumno(Long alumnoId, Pageable pageable) {
        return reservaRepository.findByAlumnoId(alumnoId, pageable).map(this::mapToResponse);
    }

    /** Obtiene las reservas de un tutor. */
    @Transactional(readOnly = true)
    public Page<ReservaClaseDetailResponse> getMisReservasTutor(Long tutorId, Pageable pageable) {
        return reservaRepository.findByTutorId(tutorId, pageable).map(this::mapToResponse);
    }

    /** Obtiene detalles de una reserva específica. */
    @Transactional(readOnly = true)
    public ReservaClaseDetailResponse getReservaDetalle(Long reservaId, Long usuarioId) {
        ReservaClase reserva =
                reservaRepository
                        .findById(reservaId)
                        .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));

        // Verificar que el usuario sea parte de la reserva
        if (!reserva.getAlumno().getId().equals(usuarioId)
                && !reserva.getTutor().getUsuario().getId().equals(usuarioId)) {
            throw new IllegalArgumentException("No tienes permisos para ver esta reserva");
        }

        return mapToResponse(reserva);
    }

    /** Devuelve las franjas horarias ocupadas de un tutor en un día concreto. */
    @Transactional(readOnly = true)
    public List<HorarioOcupadoResponse> getHorariosOcupados(Long tutorId, LocalDate fecha) {
        LocalDateTime inicioDia = fecha.atStartOfDay();
        LocalDateTime finDia = fecha.plusDays(1).atStartOfDay();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
        return reservaRepository.findReservasByTutorAndDate(tutorId, inicioDia, finDia).stream()
                .map(
                        r ->
                                new HorarioOcupadoResponse(
                                        r.getFechaHora().format(fmt),
                                        r.getFechaHora()
                                                .plusMinutes(r.getDuracionMinutos())
                                                .format(fmt)))
                .collect(Collectors.toList());
    }

    /** Obtiene próximas reservas de un tutor. */
    @Transactional(readOnly = true)
    public List<ReservaClaseDetailResponse> getProximasReservasTutor(
            Long tutorId, Integer diasAnticipacion) {
        LocalDateTime desde = LocalDateTime.now();
        LocalDateTime hasta = desde.plusDays(diasAnticipacion);
        return reservaRepository.findReservasProximas(tutorId, desde, hasta).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ==================== Métodos Privados ====================

    private ReservaClaseDetailResponse mapToResponse(ReservaClase reserva) {
        return ReservaClaseDetailResponse.builder()
                .id(reserva.getId())
                .alumnoId(reserva.getAlumno().getId())
                .alumnoNombre(reserva.getAlumno().getNombre())
                .tutorId(reserva.getTutor().getId())
                .tutorNombre(reserva.getTutor().getUsuario().getNombre())
                .fechaHora(reserva.getFechaHora())
                .duracionMinutos(reserva.getDuracionMinutos())
                .modalidad(reserva.getModalidad())
                .ubicacion(reserva.getUbicacion())
                .enlaceVirtual(reserva.getEnlaceVirtual())
                .tema(reserva.getTema())
                .descripcion(reserva.getDescripcion())
                .tarifa(reserva.getTarifa())
                .moneda(reserva.getMoneda())
                .estado(reserva.getEstado().toString())
                .calificacion(reserva.getCalificacion())
                .comentarioAlumno(reserva.getComentarioAlumno())
                .calificacionTutor(reserva.getCalificacionTutor())
                .comentarioTutor(reserva.getComentarioTutor())
                .urlGrabacion(reserva.getUrlGrabacion())
                .notasTutela(reserva.getNotasTutela())
                .createdAt(reserva.getCreatedAt())
                .fechaConfirmacion(reserva.getFechaConfirmacion())
                .puedeSerCancelada(reserva.puedeSerCancelada(MINUTOS_ANTICIPACION_CANCELACION))
                .motivoCancelacion(reserva.getMotivoCancelacion())
                .build();
    }

    // ==================== Métodos de Email ====================

    private void enviarEmailNuevaReserva(Usuario tutor, Usuario alumno, ReservaClase reserva) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(tutor.getEmail());
            message.setSubject(
                    "Nueva reserva de clase - "
                            + reserva.getFechaHora()
                                    .format(
                                            java.time.format.DateTimeFormatter.ofPattern(
                                                    "dd/MM/yyyy HH:mm")));
            message.setText(
                    "Hola "
                            + tutor.getNombre()
                            + ",\n\n"
                            + "El alumno "
                            + alumno.getNombre()
                            + " ("
                            + alumno.getEmail()
                            + ") ha solicitado una clase de "
                            + reserva.getTema()
                            + " el "
                            + reserva.getFechaHora()
                                    .format(
                                            java.time.format.DateTimeFormatter.ofPattern(
                                                    "dd/MM/yyyy HH:mm"))
                            + ".\n\n"
                            + "Por favor, confirma o rechaza la reserva en tu panel de control.\n\n"
                            + "Saludos,\nEl equipo de MeerKatters");
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Error enviando email: " + e.getMessage());
        }
    }

    private void enviarEmailConfirmacionReserva(Usuario alumno, ReservaClase reserva) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(alumno.getEmail());
            message.setSubject("Clase confirmada - " + reserva.getTema());
            message.setText(
                    "Hola "
                            + alumno.getNombre()
                            + ",\n\n"
                            + "Tu clase con "
                            + reserva.getTutor().getUsuario().getNombre()
                            + " sobre "
                            + reserva.getTema()
                            + " ha sido confirmada para el "
                            + reserva.getFechaHora()
                                    .format(
                                            java.time.format.DateTimeFormatter.ofPattern(
                                                    "dd/MM/yyyy HH:mm"))
                            + ".\n\n"
                            + (reserva.getModalidad().equals("VIRTUAL")
                                    ? "Enlace de la clase: "
                                            + (reserva.getEnlaceVirtual() != null
                                                    ? reserva.getEnlaceVirtual()
                                                    : "El tutor te enviará el enlace próximamente.")
                                            + "\n\n"
                                    : "Ubicación: "
                                            + (reserva.getUbicacion() != null
                                                    ? reserva.getUbicacion()
                                                    : "Por confirmar con el tutor.")
                                            + "\n\n")
                            + "Saludos,\nEl equipo de MeerKatters");
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Error enviando email: " + e.getMessage());
        }
    }

    private void enviarEmailCancelacionReserva(
            Usuario destinatario, ReservaClase reserva, String quienCancelo) {
        try {
            String nombreQuienCancelo =
                    quienCancelo.equals("tutor")
                            ? reserva.getTutor().getUsuario().getNombre()
                            : reserva.getAlumno().getNombre();
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(destinatario.getEmail());
            message.setSubject(
                    "Clase cancelada - "
                            + reserva.getFechaHora()
                                    .format(
                                            java.time.format.DateTimeFormatter.ofPattern(
                                                    "dd/MM/yyyy HH:mm")));
            message.setText(
                    "Hola "
                            + destinatario.getNombre()
                            + ",\n\n"
                            + "La clase sobre "
                            + reserva.getTema()
                            + " ha sido cancelada por "
                            + nombreQuienCancelo
                            + ".\n\n"
                            + (reserva.getMotivoCancelacion() != null
                                    ? "Motivo: " + reserva.getMotivoCancelacion() + "\n\n"
                                    : "")
                            + "Saludos,\nEl equipo de MeerKatters");
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Error enviando email: " + e.getMessage());
        }
    }
}
