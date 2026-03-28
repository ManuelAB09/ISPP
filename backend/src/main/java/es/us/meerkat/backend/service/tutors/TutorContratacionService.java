package es.us.meerkat.backend.service.tutors;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.us.meerkat.backend.dto.suscriptions.PaymentUrlResponse;
import es.us.meerkat.backend.dto.tutors.HireTutorRequest;
import es.us.meerkat.backend.entity.Comunidad;
import es.us.meerkat.backend.entity.EstadoContratacion;
import es.us.meerkat.backend.entity.Tutor;
import es.us.meerkat.backend.entity.TutorContratacion;
import es.us.meerkat.backend.repository.communities.ComunidadRepository;
import es.us.meerkat.backend.repository.tutors.TutorContratacionRepository;
import es.us.meerkat.backend.repository.tutors.TutorRepository;
import es.us.meerkat.backend.service.google.ClassroomLinkRequestService;
import es.us.meerkat.backend.service.suscriptions.PaymentService;

/** Servicio para manejar contrataciones de tutores por comunidades. */
@Service
public class TutorContratacionService {

    @Autowired private TutorContratacionRepository tutorContratacionRepository;

    @Autowired private TutorRepository tutorRepository;

    @Autowired private ComunidadRepository comunidadRepository;

    @Autowired private PaymentService paymentService;

    @Autowired private ClassroomLinkRequestService classroomLinkRequestService;

    // ===============================
    // OPERACIONES DE CONTRATACIÓN
    // ===============================

    /**
     * Crea una nueva contratación de tutor para una comunidad. Inicializa el pago y retorna la URL
     * de pago para que la comunidad procese el pago.
     *
     * @param comunidadId ID de la comunidad
     * @param tutorId ID del tutor a contratar
     * @param request Datos de la contratación
     * @param usuarioId ID del usuario que solicita (debe ser admin de comunidad)
     * @return PaymentUrlResponse con URL de pago
     */
    @Transactional
    public PaymentUrlResponse crearContratacion(
            Long comunidadId, Long tutorId, HireTutorRequest request, Long usuarioId) {

        Comunidad comunidad =
                comunidadRepository
                        .findById(comunidadId)
                        .orElseThrow(() -> new IllegalArgumentException("Comunidad no encontrada"));

        if (!comunidad.getCreador().getId().equals(usuarioId)) {
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
        contratacion.setEstado(EstadoContratacion.PENDIENTE_PAGO);
        contratacion.setFechaInicio(LocalDate.now());
        contratacion.setFechaFin(LocalDate.now().plusMonths(3));
        contratacion.setCreatedAt(LocalDateTime.now());

        tutorContratacionRepository.save(contratacion);

        try {
            return paymentService.generarPagoContratacionTutor(
                    tutorId, comunidadId, request.getTarifaAcordada(), usuarioId);
        } catch (com.stripe.exception.StripeException e) {
            // Si Stripe falla revertimos el estado de la contratación
            contratacion.setEstado(EstadoContratacion.CANCELADA);
            contratacion.setMotivoCancelacion("Error al generar pago: " + e.getMessage());
            tutorContratacionRepository.save(contratacion);
            throw new RuntimeException("Error al conectar con la pasarela de pago", e);
        }
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
        if (!contratacion.getComunidad().getCreador().getId().equals(usuarioId)) {
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
