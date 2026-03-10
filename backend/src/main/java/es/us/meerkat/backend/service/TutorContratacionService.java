package es.us.meerkat.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.us.meerkat.backend.dto.HireTutorRequest;
import es.us.meerkat.backend.dto.PaymentUrlResponse;
import es.us.meerkat.backend.entity.Comunidad;
import es.us.meerkat.backend.entity.EstadoContratacion;
import es.us.meerkat.backend.entity.Tutor;
import es.us.meerkat.backend.entity.TutorContratacion;
import es.us.meerkat.backend.repository.ComunidadRepository;
import es.us.meerkat.backend.repository.TutorContratacionRepository;
import es.us.meerkat.backend.repository.TutorRepository;

/** Servicio para manejar contrataciones de tutores por comunidades. */
@Service
public class TutorContratacionService {

    @Autowired private TutorContratacionRepository tutorContratacionRepository;

    @Autowired private TutorRepository tutorRepository;

    @Autowired private ComunidadRepository comunidadRepository;

    @Autowired private PaymentService paymentService;

    @Autowired private ClassroomLinkRequestService classroomLinkRequestService;
    
    @Autowired private EmailService emailService;
    
    @Autowired private AuthorizationService authorizationService;

    // ===============================
    // OPERACIONES DE CONTRATACIÓN
    // ===============================

    /**
     * Crea una nueva solicitud de contratación de tutor para una comunidad. 
     * La solicitud queda en estado PENDIENTE_APROBACION hasta que el tutor la acepte.
     *
     * @param comunidadId ID de la comunidad
     * @param tutorId ID del tutor a contratar
     * @param request Datos de la contratación
     * @param usuarioId ID del usuario que solicita (debe ser admin de comunidad)
     * @return TutorContratacion creada con estado PENDIENTE_APROBACION
     */
    @Transactional
    public TutorContratacion crearSolicitudContratacion(
            Long comunidadId, Long tutorId, HireTutorRequest request, Long usuarioId) {

        Comunidad comunidad =
                comunidadRepository
                        .findById(comunidadId)
                        .orElseThrow(() -> new IllegalArgumentException("Comunidad no encontrada"));

        if (!authorizationService.isAdminOf(usuarioId, comunidadId)) {
            throw new IllegalArgumentException(
                    "No tienes permisos para contratar tutores en esta comunidad");
        }

        Tutor tutor =
                tutorRepository
                        .findById(tutorId)
                        .orElseThrow(() -> new IllegalArgumentException("Tutor no encontrado"));

        if (tutorContratacionRepository.findActivaByComunidadId(comunidadId).isPresent()) {
            throw new IllegalArgumentException("La comunidad ya tiene un tutor activo");
        }

        if (!tutor.getVerificado()) {
            throw new IllegalArgumentException(
                    "El tutor debe estar verificado para ser contratado");
        }

        TutorContratacion contratacion = new TutorContratacion();
        contratacion.setTutor(tutor);
        contratacion.setComunidad(comunidad);
        contratacion.setModalidad(request.getModalidad());
        contratacion.setDuracion(request.getDuracion());
        contratacion.setTarifaAcordada(request.getTarifaAcordada());
        contratacion.setEstado(EstadoContratacion.PENDIENTE_APROBACION);
        contratacion.setFechaInicio(LocalDate.now().plusWeeks(1)); // Estimado
        contratacion.setFechaFin(LocalDate.now().plusMonths(3));
        contratacion.setCreatedAt(LocalDateTime.now());

        tutorContratacionRepository.save(contratacion);
        
        // Enviar notificación por email al tutor
        try {
            String tutorEmail = tutor.getUsuario().getEmail();
            String tutorNombre = tutor.getUsuario().getNombre();
            String comunidadNombre = comunidad.getNombre();
            
            emailService.sendSimpleEmail(
                tutorEmail,
                "Nueva solicitud de contratación en MeerKatters",
                String.format(
                    "Hola %s,\n\n" +
                    "La comunidad '%s' te ha enviado una solicitud de contratación.\n\n" +
                    "Detalles:\n" +
                    "- Modalidad: %s\n" +
                    "- Duración: %s\n" +
                    "- Tarifa acordada: %.2f€\n\n" +
                    "Puedes revisar y responder a esta solicitud desde tu panel de tutor en MeerKatters.\n\n" +
                    "Saludos,\n" +
                    "El equipo de MeerKatters",
                    tutorNombre, comunidadNombre, 
                    request.getModalidad(), request.getDuracion(), request.getTarifaAcordada()
                )
            );
        } catch (Exception e) {
            // Log del error pero no fallar la operación
            System.err.println("Error al enviar email de notificación: " + e.getMessage());
        }

        return contratacion;
    }
    
    /**
     * El tutor acepta una solicitud de contratación. 
     * Cambia el estado a APROBADA y genera la URL de pago para la comunidad.
     *
     * @param contratacionId ID de la contratación
     * @param tutorId ID del tutor que acepta
     * @return PaymentUrlResponse con URL de pago
     */
    @Transactional
    public PaymentUrlResponse aceptarSolicitud(Long contratacionId, Long tutorId) {
        TutorContratacion contratacion =
                tutorContratacionRepository
                        .findByIdAndTutorId(contratacionId, tutorId)
                        .orElseThrow(() -> new IllegalArgumentException(
                            "Solicitud no encontrada o no pertenece a este tutor"));

        if (!contratacion.getEstado().equals(EstadoContratacion.PENDIENTE_APROBACION)) {
            throw new IllegalArgumentException(
                    "Solo se pueden aceptar solicitudes en estado PENDIENTE_APROBACION");
        }

        contratacion.setEstado(EstadoContratacion.APROBADA);
        contratacion.setUpdatedAt(LocalDateTime.now());
        tutorContratacionRepository.save(contratacion);
        
        // Notificar a la comunidad
        try {
            String adminEmail = contratacion.getComunidad().getCreador().getEmail();
            String adminNombre = contratacion.getComunidad().getCreador().getNombre();
            String tutorNombre = contratacion.getTutor().getUsuario().getNombre();
            String comunidadNombre = contratacion.getComunidad().getNombre();
            
            emailService.sendSimpleEmail(
                adminEmail,
                "¡Solicitud de tutor aceptada! - MeerKatters",
                String.format(
                    "Hola %s,\n\n" +
                    "¡Buenas noticias! El tutor %s ha aceptado tu solicitud para la comunidad '%s'.\n\n" +
                    "El siguiente paso es proceder con el pago. Inicia sesión en MeerKatters para completar el proceso.\n\n" +
                    "Saludos,\n" +
                    "El equipo de MeerKatters",
                    adminNombre, tutorNombre, comunidadNombre
                )
            );
        } catch (Exception e) {
            System.err.println("Error al enviar email de notificación: " + e.getMessage());
        }

        try {
            // Generar URL de pago
            return paymentService.generarPagoContratacionTutor(
                    tutorId, 
                    contratacion.getComunidad().getId(), 
                    contratacion.getTarifaAcordada(), 
                    contratacion.getComunidad().getCreador().getId());
        } catch (com.stripe.exception.StripeException e) {
            // Si Stripe falla revertimos el estado
            contratacion.setEstado(EstadoContratacion.PENDIENTE_APROBACION);
            contratacion.setMotivoCancelacion("Error al generar pago: " + e.getMessage());
            tutorContratacionRepository.save(contratacion);
            throw new RuntimeException("Error al conectar con la pasarela de pago", e);
        }
    }
    
    /**
     * El tutor rechaza una solicitud de contratación.
     *
     * @param contratacionId ID de la contratación
     * @param tutorId ID del tutor que rechaza
     * @param motivo Motivo del rechazo
     */
    @Transactional
    public void rechazarSolicitud(Long contratacionId, Long tutorId, String motivo) {
        TutorContratacion contratacion =
                tutorContratacionRepository
                        .findByIdAndTutorId(contratacionId, tutorId)
                        .orElseThrow(() -> new IllegalArgumentException(
                            "Solicitud no encontrada o no pertenece a este tutor"));

        if (!contratacion.getEstado().equals(EstadoContratacion.PENDIENTE_APROBACION)) {
            throw new IllegalArgumentException(
                    "Solo se pueden rechazar solicitudes en estado PENDIENTE_APROBACION");
        }

        contratacion.setEstado(EstadoContratacion.RECHAZADA);
        contratacion.setMotivoCancelacion(motivo);
        contratacion.setUpdatedAt(LocalDateTime.now());
        tutorContratacionRepository.save(contratacion);
        
        // Notificar a la comunidad
        try {
            String adminEmail = contratacion.getComunidad().getCreador().getEmail();
            String adminNombre = contratacion.getComunidad().getCreador().getNombre();
            String tutorNombre = contratacion.getTutor().getUsuario().getNombre();
            String comunidadNombre = contratacion.getComunidad().getNombre();
            
            emailService.sendSimpleEmail(
                adminEmail,
                "Solicitud de tutor rechazada - MeerKatters",
                String.format(
                    "Hola %s,\n\n" +
                    "El tutor %s ha rechazado tu solicitud para la comunidad '%s'.\n\n" +
                    "Motivo: %s\n\n" +
                    "Puedes buscar otros tutores disponibles en MeerKatters.\n\n" +
                    "Saludos,\n" +
                    "El equipo de MeerKatters",
                    adminNombre, tutorNombre, comunidadNombre, motivo
                )
            );
        } catch (Exception e) {
            System.err.println("Error al enviar email de notificación: " + e.getMessage());
        }
    }
    
    /**
     * Procesa el pago de una contratación aprobada y cambia su estado a PENDIENTE_PAGO.
     *
     * @param comunidadId ID de la comunidad
     * @param tutorId ID del tutor
     * @return PaymentUrlResponse con URL de pago
     */
    @Transactional
    public PaymentUrlResponse generarPagoContratacion(Long comunidadId, Long tutorId, Long usuarioId) {
        // Buscar contratación aprobada
        TutorContratacion contratacion = tutorContratacionRepository
                .findByTutorIdAndComunidadId(tutorId, comunidadId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "No se encontró solicitud de contratación"));

        if (!contratacion.getEstado().equals(EstadoContratacion.APROBADA)) {
            throw new IllegalArgumentException(
                    "La solicitud debe estar en estado APROBADA para proceder al pago");
        }
        
        if (!authorizationService.isAdminOf(usuarioId, comunidadId)) {
            throw new IllegalArgumentException(
                    "No tienes permisos para pagar esta contratación");
        }

        contratacion.setEstado(EstadoContratacion.PENDIENTE_PAGO);
        tutorContratacionRepository.save(contratacion);
        
        try {
            return paymentService.generarPagoContratacionTutor(
                    tutorId, comunidadId, contratacion.getTarifaAcordada(), usuarioId);
        } catch (com.stripe.exception.StripeException e) {
            // Si Stripe falla revertimos el estado
            contratacion.setEstado(EstadoContratacion.APROBADA);
            contratacion.setMotivoCancelacion("Error al generar pago: " + e.getMessage());
            tutorContratacionRepository.save(contratacion);
            throw new RuntimeException("Error al conectar con la pasarela de pago", e);
        }
    }

    /**
     * Obtiene las solicitudes pendientes de aprobación para un tutor.
     *
     * @param tutorId ID del tutor
     * @param pageable Información de paginación
     * @return Página de solicitudes pendientes
     */
    @Transactional(readOnly = true)
    public Page<TutorContratacion> obtenerSolicitudesPendientes(Long tutorId, Pageable pageable) {
        return tutorContratacionRepository.findByTutorIdAndEstadoWithRelations(
                tutorId, EstadoContratacion.PENDIENTE_APROBACION, pageable);
    }

    /**
     * Obtiene las contrataciones de un tutor con paginación.
     *
     * @param tutorId ID del tutor
     * @param pageable Información de paginación
     * @return Página de contrataciones del tutor
     */
    @Transactional(readOnly = true)
    public Page<TutorContratacion> obtenerContratacionesDelTutor(Long tutorId, Pageable pageable) {
        return tutorContratacionRepository.findByTutorId(tutorId, pageable);
    }

    /**
     * Obtiene las contrataciones de una comunidad con paginación.
     *
     * @param comunidadId ID de la comunidad
     * @param pageable Información de paginación
     * @return Página de contrataciones de la comunidad
     */
    @Transactional(readOnly = true)
    public Page<TutorContratacion> obtenerContratacionesDeLaComunidad(
            Long comunidadId, Pageable pageable) {
        return tutorContratacionRepository.findByComunidadId(comunidadId, pageable);
    }

    /**
     * Obtiene la contratación activa (si existe) de una comunidad.
     *
     * @param comunidadId ID de la comunidad
     * @return Contratación activa si existe
     */
    @Transactional(readOnly = true)
    public java.util.Optional<TutorContratacion> obtenerContratacionActivaDeComunidad(
            Long comunidadId) {
        return tutorContratacionRepository.findActivaByComunidadId(comunidadId);
    }

    /**
     * Obtiene una contratación específica.
     *
     * @param contratacionId ID de la contratación
     * @return Contratación si existe
     */
    @Transactional(readOnly = true)
    public java.util.Optional<TutorContratacion> obtenerContratacion(Long contratacionId) {
        return tutorContratacionRepository.findById(contratacionId);
    }

    // ===============================
    // CAMBIOS DE ESTADO
    // ===============================

    /**
     * Activa una contratación después de que el pago se ha completado.
     *
     * @param contratacionId ID de la contratación
     */
    @Transactional
    public void activarContratacion(Long contratacionId) {
        TutorContratacion contratacion =
                tutorContratacionRepository
                        .findById(contratacionId)
                        .orElseThrow(
                                () -> new IllegalArgumentException("Contratación no encontrada"));

        if (!contratacion.getEstado().equals(EstadoContratacion.PENDIENTE_PAGO)) {
            throw new IllegalArgumentException(
                    "La contratación debe estar en estado PENDIENTE_PAGO para activarse");
        }

        contratacion.setEstado(EstadoContratacion.ACTIVA);
        contratacion.setFechaInicio(LocalDate.now());

        tutorContratacionRepository.save(contratacion);

        Long comunidadId = contratacion.getComunidad().getId();
        Long tutorId = contratacion.getTutor().getUsuario().getId();
        classroomLinkRequestService.crearSolicitud(comunidadId, tutorId);
    }

    /**
     * Cancela una contratación.
     *
     * @param contratacionId ID de la contratación
     * @param usuarioId ID del usuario que cancela (debe ser admin de comunidad)
     * @param motivo Motivo de la cancelación
     */
    @Transactional
    public void cancelarContratacion(Long contratacionId, Long usuarioId, String motivo) {
        TutorContratacion contratacion =
                tutorContratacionRepository
                        .findById(contratacionId)
                        .orElseThrow(
                                () -> new IllegalArgumentException("Contratación no encontrada"));

        // Validar que el usuario es admin de la comunidad
        if (!authorizationService.isAdminOf(usuarioId, contratacion.getComunidad().getId())) {
            throw new IllegalArgumentException(
                    "No tienes permisos para cancelar esta contratación");
        }

        contratacion.setEstado(EstadoContratacion.CANCELADA);
        contratacion.setMotivoCancelacion(motivo);
        contratacion.setFechaFin(LocalDate.now());

        tutorContratacionRepository.save(contratacion);
    }

    /**
     * Completa una contratación (al finalizar el período).
     *
     * @param contratacionId ID de la contratación
     */
    @Transactional
    public void completarContratacion(Long contratacionId) {
        TutorContratacion contratacion =
                tutorContratacionRepository
                        .findById(contratacionId)
                        .orElseThrow(
                                () -> new IllegalArgumentException("Contratación no encontrada"));

        contratacion.setEstado(EstadoContratacion.COMPLETADA);
        contratacion.setFechaFin(LocalDate.now());

        tutorContratacionRepository.save(contratacion);
    }

    // ===============================
    // VALIDACIONES
    // ===============================

    /**
     * Verifica si una comunidad tiene un tutor activo.
     *
     * @param comunidadId ID de la comunidad
     * @return true si tiene tutor activo
     */
    @Transactional(readOnly = true)
    public boolean tieneTutorActivo(Long comunidadId) {
        return tutorContratacionRepository.findActivaByComunidadId(comunidadId).isPresent();
    }

    /**
     * Obtiene el tutor activo de una comunidad si existe.
     *
     * @param comunidadId ID de la comunidad
     * @return Tutor activo si existe
     */
    @Transactional(readOnly = true)
    public java.util.Optional<Tutor> obtenerTutorActivoDeComunidad(Long comunidadId) {
        return tutorContratacionRepository
                .findActivaByComunidadId(comunidadId)
                .map(TutorContratacion::getTutor);
    }
}
