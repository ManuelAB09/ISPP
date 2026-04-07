package es.us.meerkat.backend.service.communities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.us.meerkat.backend.dto.communities.CreateInstitutionRequest;
import es.us.meerkat.backend.dto.communities.UpdateInstitutionRequest;
import es.us.meerkat.backend.dto.subscriptions.CorporatePlanRequest;
import es.us.meerkat.backend.dto.subscriptions.PaymentUrlResponse;
import es.us.meerkat.backend.entity.communities.Institution;
import es.us.meerkat.backend.entity.subscriptions.TipoPlanCorporativo;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.communities.InstitutionRepository;
import es.us.meerkat.backend.repository.users.UsuarioRepository;
import es.us.meerkat.backend.service.emails.EmailService;
import es.us.meerkat.backend.service.subscriptions.PaymentService;
import lombok.extern.slf4j.Slf4j;

/**
 * Servicio para manejar operaciones de instituciones educativas y planes
 * corporativos.
 */
@Slf4j
@Service
public class InstitutionService {

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    // ===============================
    // OPERACIONES CRUD
    // ===============================

    /**
     * Crea una nueva institución.
     *
     * @param usuarioId ID del usuario administrador
     * @param request   datos de la institución
     * @return Institución creada
     */
    @Transactional
    public Institution crearInstitucion(Long usuarioId, CreateInstitutionRequest request) {
        Usuario usuario = usuarioRepository
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
     * @param usuarioId     ID del usuario (para verificar permisos de admin)
     * @return Institución
     */
    @Transactional(readOnly = true)
    public Institution obtenerInstitucion(Long institutionId, Long usuarioId) {
        Institution institution = institutionRepository
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
     * @param usuarioId     ID del usuario administrador
     * @param request       datos actualizados
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

    /**
     * Lista todas las instituciones para filtros y desplegables públicos.
     *
     * @return instituciones ordenadas alfabéticamente
     */
    @Transactional(readOnly = true)
    public List<Institution> listarInstituciones() {
        return institutionRepository.findAll(Sort.by(Sort.Direction.ASC, "nombre"));
    }

    // ===============================
    // PLANES CORPORATIVOS
    // ===============================

    /**
     * Contrata un plan corporativo para una institución.
     *
     * @param institutionId ID de la institución
     * @param usuarioId     ID del usuario administrador
     * @param request       datos del plan
     * @return Respuesta con URL de pago
     */
    @Transactional
    public PaymentUrlResponse contratarPlanCorporativo(
            Long institutionId, Long usuarioId, CorporatePlanRequest request) {
        Institution institution = obtenerInstitucion(institutionId, usuarioId);

        // Verificar si la institución ya tiene un plan activo y vigente
        if (institution.getPlanActivo()
                && (institution.getFechaFinPlan() == null
                        || institution.getFechaFinPlan().isAfter(LocalDateTime.now()))) {
            throw new IllegalArgumentException(
                    "Esta institución ya tiene un plan corporativo activo. "
                            + "Espera a que expire o cancélalo antes de contratar uno nuevo.");
        }

        TipoPlanCorporativo tipoPlan = TipoPlanCorporativo.valueOf(request.getTipoPlan());

        if (tipoPlan == TipoPlanCorporativo.REDUCIDO_PUBLICA
                || tipoPlan == TipoPlanCorporativo.REDUCIDO_PRIVADA) {
            validarEligibilidadPlanReducido(institution.getDominioEmail());
        }

        // El monto ya no se calcula aquí — Stripe usa el priceId fijo
        // Solo se usa para registrar la transacción interna si hace falta
        BigDecimal montoEstimado = calcularMontoEstimado(tipoPlan, request.getPeriodo());

        PaymentUrlResponse paymentUrl;
        try {
            paymentUrl = paymentService.generarPagoPlanCorporativo(
                    institutionId,
                    tipoPlan,
                    montoEstimado,
                    request.getPeriodo(),
                    institution.getEmailContacto()); // ← email para prerellenar Stripe
        } catch (com.stripe.exception.StripeException e) {
            throw new RuntimeException("Error al conectar con la pasarela de pago", e);
        }

        institution.setNumUsuariosPermitidos(request.getNumUsuarios());
        institution.setPlanCorporativo(tipoPlan);
        institutionRepository.save(institution);

        return paymentUrl;
    }

    /**
     * Persiste la configuración del plan corporativo para el flujo de PaymentIntent
     * embebido.
     *
     * @param institutionId ID de la institución
     * @param usuarioId     ID del usuario administrador
     * @param request       datos del plan
     * @return institución actualizada
     */
    @Transactional
    public Institution preconfigurarPlanCorporativo(
            Long institutionId, Long usuarioId, CorporatePlanRequest request) {
        Institution institution = obtenerInstitucion(institutionId, usuarioId);

        TipoPlanCorporativo tipoPlan = TipoPlanCorporativo.valueOf(request.getTipoPlan());

        if (tipoPlan == TipoPlanCorporativo.REDUCIDO_PUBLICA
                || tipoPlan == TipoPlanCorporativo.REDUCIDO_PRIVADA) {
            validarEligibilidadPlanReducido(institution.getDominioEmail());
        }

        institution.setNumUsuariosPermitidos(request.getNumUsuarios());
        institution.setPlanCorporativo(tipoPlan);
        return institutionRepository.save(institution);
    }

    // Helper para calcular monto estimado según plan y periodo
    private BigDecimal calcularMontoEstimado(TipoPlanCorporativo tipoPlan, String periodo) {
        boolean esAnual = "anual".equalsIgnoreCase(periodo);
        return switch (tipoPlan) {
            case BASICO -> esAnual ? new BigDecimal("499.99") : new BigDecimal("49.99");
            case ESTANDAR -> esAnual ? new BigDecimal("999.99") : new BigDecimal("99.99");
            case PREMIUM -> esAnual ? new BigDecimal("1999.99") : new BigDecimal("199.99");
            default -> BigDecimal.ZERO;
        };
    }

    /**
     * Valida la elegibilidad de una institución para planes reducidos basándose en
     * el dominio de
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
     * Activa un plan corporativo después de que el pago se completa. También
     * gestiona la creación o
     * vinculación de la cuenta del email de contacto.
     *
     * @param institutionId ID de la institución
     * @param duracionMeses Duración del plan en meses
     * @param emailContacto Email del contacto institucional
     */
    @Transactional
    public void activarPlanCorporativo(
            Long institutionId, Integer duracionMeses, String emailContacto) {
        activarPlanCorporativo(institutionId, duracionMeses, emailContacto, null);
    }

    @Transactional
    public void activarPlanCorporativo(
            Long institutionId,
            Integer duracionMeses,
            String emailContacto,
            TipoPlanCorporativo tipoPlanCorporativo) {
        Institution institution = institutionRepository
                .findById(institutionId)
                .orElseThrow(
                        () -> new IllegalArgumentException("Institución no encontrada"));

        if (tipoPlanCorporativo != null) {
            institution.setPlanCorporativo(tipoPlanCorporativo);
        }

        institution.setPlanActivo(true);
        institution.setFechaInicioPlan(LocalDateTime.now());
        institution.setFechaFinPlan(LocalDateTime.now().plusMonths(duracionMeses));

        institutionRepository.save(institution);

        // Garantiza que el admin que contrató también quede vinculado a la institución.
        Usuario usuarioAdmin = institution.getUsuarioAdmin();
        if (usuarioAdmin != null
                && (usuarioAdmin.getInstitution() == null
                        || !institution.getId().equals(usuarioAdmin.getInstitution().getId()))) {
            usuarioAdmin.setInstitution(institution);
            usuarioRepository.save(usuarioAdmin);
        }

        // Gestionar la cuenta del email de contacto
        if (emailContacto != null && !emailContacto.isBlank()) {
            asignarPlanInstitucionalAUsuario(emailContacto, institution);
        }
    }

    /** Sobrecarga sin emailContacto para compatibilidad. */
    @Transactional
    public void activarPlanCorporativo(Long institutionId, Integer duracionMeses) {
        Institution institution = institutionRepository
                .findById(institutionId)
                .orElseThrow(
                        () -> new IllegalArgumentException("Institución no encontrada"));
        activarPlanCorporativo(institutionId, duracionMeses, institution.getEmailContacto(), null);
    }

    /** Busca o crea un usuario por email y le asigna el plan institucional. */
    private void asignarPlanInstitucionalAUsuario(String email, Institution institution) {
        Optional<Usuario> existente = usuarioRepository.findByEmail(email);
        boolean isNewAccount = existente.isEmpty();
        Usuario usuario;

        if (isNewAccount) {
            // Crear cuenta nueva
            usuario = new Usuario();
            usuario.setEmail(email);
            usuario.setNombre("Usuario Institucional");
            usuario.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            usuario.setEmailVerificado(false);
            usuario.setVisibleEnListados(true);
            usuario.setEsTutor(false);
            usuario.setAutenticacionDosFactores(false);
            usuario.setNotificacionesPush(false);

            String verificationToken = UUID.randomUUID().toString();
            usuario.setVerificationToken(verificationToken);
            usuario.setTokenExpiration(LocalDateTime.now().plusHours(48));
        } else {
            usuario = existente.get();
        }

        // Vincular a la institución (NO cambiar a PREMIUM de estudiante)
        usuario.setInstitution(institution);
        usuarioRepository.save(usuario);

        // Enviar email de invitación
        String planName = institution.getPlanCorporativo() != null
                ? institution.getPlanCorporativo().name()
                : "Institucional";
        try {
            emailService.sendInstitutionInviteEmail(
                    email,
                    institution.getNombre(),
                    planName,
                    isNewAccount,
                    isNewAccount ? usuario.getVerificationToken() : null,
                    frontendUrl);
        } catch (Exception e) {
            log.error(
                    "Error enviando email de invitación institucional a {}: {}",
                    email,
                    e.getMessage());
        }
    }

    /**
     * Cancela un plan corporativo.
     *
     * @param institutionId ID de la institución
     * @param usuarioId     ID del usuario administrador
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
        Institution institution = institutionRepository
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
        Institution institution = institutionRepository
                .findById(institutionId)
                .orElseThrow(
                        () -> new IllegalArgumentException("Institución no encontrada"));

        return institution.getNumUsuariosPermitidos() != null
                ? institution.getNumUsuariosPermitidos()
                : 0;
    }

    @Transactional(readOnly = true)
    public long contarUsuarios(Long institutionId) {
        Institution institution = institutionRepository
                .findById(institutionId)
                .orElseThrow(
                        () -> new IllegalArgumentException("Institución no encontrada"));
        return institutionRepository.countUsuariosByDominioEmail(institution.getDominioEmail());
    }

    @Transactional(readOnly = true)
    public long contarComunidades(Long institutionId) {
        return institutionRepository.countComunidadesByInstitutionId(institutionId);
    }
}
