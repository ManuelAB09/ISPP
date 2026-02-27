package es.us.meerkat.backend.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.us.meerkat.backend.dto.CorporatePlanRequest;
import es.us.meerkat.backend.dto.CreateInstitutionRequest;
import es.us.meerkat.backend.dto.PaymentUrlResponse;
import es.us.meerkat.backend.dto.UpdateInstitutionRequest;
import es.us.meerkat.backend.entity.Institution;
import es.us.meerkat.backend.entity.TipoPlanCorporativo;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.InstitutionRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;

/** Servicio para manejar operaciones de instituciones educativas y planes corporativos. */
@Service
public class InstitutionService {

    @Autowired private InstitutionRepository institutionRepository;

    @Autowired private UsuarioRepository usuarioRepository;

    @Autowired private PaymentService paymentService;

    // ===============================
    // OPERACIONES CRUD
    // ===============================

    /**
     * Crea una nueva institución.
     *
     * @param usuarioId ID del usuario administrador
     * @param request datos de la institución
     * @return Institución creada
     */
    @Transactional
    public Institution crearInstitucion(Long usuarioId, CreateInstitutionRequest request) {
        Usuario usuario =
                usuarioRepository
                        .findById(usuarioId)
                        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // Validar que no existe otra institución con el mismo dominio de email
        if (institutionRepository.findByDominioEmail(request.getDominioEmail()).isPresent()) {
            throw new IllegalArgumentException(
                    "Ya existe una institución registrada con este dominio");
        }

        Institution institution = new Institution();
        institution.setNombre(request.getNombre());
        institution.setDescripcion(request.getDescripcion());
        institution.setEmailContacto(request.getEmailContacto());
        institution.setTelefonoContacto(request.getTelefonoContacto());
        institution.setDominioEmail(request.getDominioEmail());
        institution.setUbicacion(request.getUbicacion());
        institution.setSitioweb(request.getSitioweb());
        institution.setLogoUrl(request.getLogoUrl());
        institution.setVerificada(false);
        institution.setPlanActivo(false);
        institution.setUsuarioAdmin(usuario);
        institution.setCreatedAt(LocalDateTime.now());

        return institutionRepository.save(institution);
    }

    /**
     * Obtiene los detalles de una institución.
     *
     * @param institutionId ID de la institución
     * @param usuarioId ID del usuario (para verificar permisos de admin)
     * @return Institución
     */
    @Transactional(readOnly = true)
    public Institution obtenerInstitucion(Long institutionId, Long usuarioId) {
        Institution institution =
                institutionRepository
                        .findById(institutionId)
                        .orElseThrow(
                                () -> new IllegalArgumentException("Institución no encontrada"));

        // Verificar que el usuario es administrador de la institución
        if (!institution.getUsuarioAdmin().getId().equals(usuarioId)) {
            throw new IllegalArgumentException(
                    "No tienes permisos para acceder a esta institución");
        }

        return institution;
    }

    /**
     * Obtiene una institución sin checks de permisos (para búsquedas públicas).
     *
     * @param institutionId ID de la institución
     * @return Institución
     */
    @Transactional(readOnly = true)
    public Institution obtenerInstitutionPublica(Long institutionId) {
        return institutionRepository
                .findById(institutionId)
                .orElseThrow(() -> new IllegalArgumentException("Institución no encontrada"));
    }

    /**
     * Actualiza los datos de una institución.
     *
     * @param institutionId ID de la institución
     * @param usuarioId ID del usuario administrador
     * @param request datos actualizados
     * @return Institución actualizada
     */
    @Transactional
    public Institution actualizarInstitucion(
            Long institutionId, Long usuarioId, UpdateInstitutionRequest request) {
        Institution institution = obtenerInstitucion(institutionId, usuarioId);

        if (request.getNombre() != null) {
            institution.setNombre(request.getNombre());
        }
        if (request.getDescripcion() != null) {
            institution.setDescripcion(request.getDescripcion());
        }
        if (request.getEmailContacto() != null) {
            institution.setEmailContacto(request.getEmailContacto());
        }
        if (request.getTelefonoContacto() != null) {
            institution.setTelefonoContacto(request.getTelefonoContacto());
        }
        if (request.getUbicacion() != null) {
            institution.setUbicacion(request.getUbicacion());
        }
        if (request.getSitioweb() != null) {
            institution.setSitioweb(request.getSitioweb());
        }
        if (request.getLogoUrl() != null) {
            institution.setLogoUrl(request.getLogoUrl());
        }

        institution.setUpdatedAt(LocalDateTime.now());
        return institutionRepository.save(institution);
    }

    // ===============================
    // PLANES CORPORATIVOS
    // ===============================

    /**
     * Contrata un plan corporativo para una institución.
     *
     * @param institutionId ID de la institución
     * @param usuarioId ID del usuario administrador
     * @param request datos del plan
     * @return Respuesta con URL de pago
     */
    @Transactional
    public PaymentUrlResponse contratarPlanCorporativo(
            Long institutionId, Long usuarioId, CorporatePlanRequest request) {
        Institution institution = obtenerInstitucion(institutionId, usuarioId);

        // Validar eligibilidad para planes reducidos
        if (request.getTipoPlan() != null
                && (request.getTipoPlan().equals("REDUCIDO_PUBLICA")
                        || request.getTipoPlan().equals("REDUCIDO_PRIVADA"))) {
            validarEligibilidadPlanReducido(institution.getDominioEmail());
        }

        // Calcular monto del plan (simplificado: 15€ por usuario por mes)
        BigDecimal montoUnitario = new BigDecimal("15");
        BigDecimal monto =
                montoUnitario
                        .multiply(new BigDecimal(request.getNumUsuarios()))
                        .multiply(new BigDecimal(request.getDuracionMeses()));

        // Aplicar descuento para planes reducidos
        if (request.getTipoPlan() != null
                && (request.getTipoPlan().equals("REDUCIDO_PUBLICA")
                        || request.getTipoPlan().equals("REDUCIDO_PRIVADA"))) {
            // 40% descuento para planes reducidos
            monto = monto.multiply(new BigDecimal("0.60"));
        }

        // Generar pago
        PaymentUrlResponse paymentUrl =
                paymentService.generarPagoPlanCorporativo(institutionId, monto);

        // Actualizar institución con datos provisionales
        institution.setNumUsuariosPermitidos(request.getNumUsuarios());
        institution.setPlanCorporativo(TipoPlanCorporativo.valueOf(request.getTipoPlan()));
        institutionRepository.save(institution);

        return paymentUrl;
    }

    /**
     * Valida la elegibilidad de una institución para planes reducidos basándose en el dominio de
     * email.
     *
     * @param dominioEmail dominio de email institucional
     * @return true si es elegible
     */
    public boolean validarEligibilidadPlanReducido(String dominioEmail) {
        // Dominios educativos públicos españoles comunes
        String[] dominiosPublicos = {
            ".es.gov",
            ".educacion.es",
            ".junta.es",
            ".xunta.es",
            ".gencat.es",
            ".eus",
            ".infonavit.org.mx"
        };

        for (String dominio : dominiosPublicos) {
            if (dominioEmail.toLowerCase().endsWith(dominio)) {
                return true;
            }
        }

        // Si no es público, rechazar plan reducido
        throw new IllegalArgumentException(
                "Esta institución no es elegible para planes reducidos. Requiere ser una"
                        + " institución pública educativa.");
    }

    /**
     * Activa un plan corporativo después de que el pago se completa.
     *
     * @param institutionId ID de la institución
     * @param duracionMeses Duración del plan en meses
     */
    @Transactional
    public void activarPlanCorporativo(Long institutionId, Integer duracionMeses) {
        Institution institution =
                institutionRepository
                        .findById(institutionId)
                        .orElseThrow(
                                () -> new IllegalArgumentException("Institución no encontrada"));

        institution.setPlanActivo(true);
        institution.setFechaInicioPlan(LocalDateTime.now());
        institution.setFechaFinPlan(LocalDateTime.now().plusMonths(duracionMeses));

        institutionRepository.save(institution);
    }

    /**
     * Cancela un plan corporativo.
     *
     * @param institutionId ID de la institución
     * @param usuarioId ID del usuario administrador
     */
    @Transactional
    public void cancelarPlanCorporativo(Long institutionId, Long usuarioId) {
        Institution institution = obtenerInstitucion(institutionId, usuarioId);

        institution.setPlanActivo(false);
        institution.setPlanCorporativo(null);
        institution.setFechaFinPlan(LocalDateTime.now());

        institutionRepository.save(institution);
    }

    /**
     * Verifica si el plan de una institución está activo y vigente.
     *
     * @param institutionId ID de la institución
     * @return true si el plan está activo y la fecha de fin no ha pasado
     */
    @Transactional(readOnly = true)
    public boolean esPlanActivo(Long institutionId) {
        Institution institution =
                institutionRepository
                        .findById(institutionId)
                        .orElseThrow(
                                () -> new IllegalArgumentException("Institución no encontrada"));

        if (!institution.getPlanActivo()) {
            return false;
        }

        if (institution.getFechaFinPlan() == null) {
            return true;
        }

        return institution.getFechaFinPlan().isAfter(LocalDateTime.now());
    }

    /**
     * Obtiene el número de usuarios permitidos actualmente.
     *
     * @param institutionId ID de la institución
     * @return Número de usuarios permitidos
     */
    @Transactional(readOnly = true)
    public Integer obtenerNumUsuariosPermitidos(Long institutionId) {
        Institution institution =
                institutionRepository
                        .findById(institutionId)
                        .orElseThrow(
                                () -> new IllegalArgumentException("Institución no encontrada"));

        return institution.getNumUsuariosPermitidos() != null
                ? institution.getNumUsuariosPermitidos()
                : 0;
    }
}
