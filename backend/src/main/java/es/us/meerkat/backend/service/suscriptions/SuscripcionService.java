package es.us.meerkat.backend.service.suscriptions;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.us.meerkat.backend.dto.SubscriptionResponse;
import es.us.meerkat.backend.entity.Institution;
import es.us.meerkat.backend.entity.Suscripcion;
import es.us.meerkat.backend.entity.TipoPlan;
import es.us.meerkat.backend.entity.TipoTransaccion;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.InstitutionRepository;
import es.us.meerkat.backend.repository.SuscripcionRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

/** Servicio para gestionar suscripciones de usuarios. */
@Service
@RequiredArgsConstructor
public class SuscripcionService {

    private final SuscripcionRepository suscripcionRepository;
    private final UsuarioRepository usuarioRepository;
    private final InstitutionRepository institutionRepository;
    private final PaymentService paymentService;

    /**
     * Obtiene todos los planes disponibles.
     *
     * @return Array de tipos de planes
     */
    public TipoPlan[] obtenerPlanesDisponibles() {
        return TipoPlan.values();
    }

    /**
     * Obtiene la suscripción actual de un usuario.
     *
     * @param usuarioId ID del usuario
     * @return Suscripción activa si existe
     */
    public Optional<Suscripcion> obtenerMiSuscripcion(Long usuarioId) {
        return suscripcionRepository.findByUsuarioIdAndActiva(usuarioId, true);
    }

    /**
     * Obtiene la suscripción completa del usuario, incluyendo info de institución.
     *
     * @param usuarioId ID del usuario
     * @return SubscriptionResponse con datos de suscripción e institución
     */
    @Transactional(readOnly = true)
    public SubscriptionResponse obtenerMiSuscripcionCompleta(Long usuarioId) {
        Optional<Suscripcion> suscripcion =
                suscripcionRepository.findByUsuarioIdAndActiva(usuarioId, true);

        SubscriptionResponse response;
        if (suscripcion.isPresent()) {
            response = suscripcion.get().toDTO();
        } else {
            response =
                    SubscriptionResponse.builder()
                            .id(null)
                            .plan(TipoPlan.FREE)
                            .fechaInicio(java.time.LocalDate.now())
                            .fechaFin(java.time.LocalDate.now())
                            .activa(true)
                            .autoRenovar(false)
                            .enPeriodoGracia(false)
                            .build();
        }

        // Cargar info de institución dentro de la transacción
        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
        Institution institution = resolveInstitutionForUser(usuario);
        if (institution != null) {
            response.setInstitutionNombre(institution.getNombre());
            response.setInstitutionId(institution.getId());
            response.setPlanCorporativo(
                    institution.getPlanCorporativo() != null
                            ? institution.getPlanCorporativo().name()
                            : null);
            // Verificar que el plan está activo Y no ha expirado
            boolean planRealmenteActivo =
                    institution.getPlanActivo()
                            && (institution.getFechaFinPlan() == null
                                    || institution.getFechaFinPlan().isAfter(LocalDateTime.now()));
            response.setPlanCorporativoActivo(planRealmenteActivo);
        }

        return response;
    }

    /**
     * Verifica si un usuario tiene un plan institucional activo y vigente.
     *
     * @param usuario Usuario a verificar
     * @return true si tiene plan institucional activo
     */
    public boolean tienePlanInstitucionalActivo(Usuario usuario) {
        Institution institution = resolveInstitutionForUser(usuario);
        if (institution == null) {
            return false;
        }
        return institution.getPlanActivo()
                && (institution.getFechaFinPlan() == null
                        || institution.getFechaFinPlan().isAfter(LocalDateTime.now()));
    }

    private Institution resolveInstitutionForUser(Usuario usuario) {
        if (usuario == null) {
            return null;
        }

        // 1) Priorizar institución activa administrada por el usuario
        Optional<Institution> adminActiveInstitution =
                institutionRepository
                        .findFirstByUsuarioAdminIdAndPlanActivoTrueOrderByFechaFinPlanDesc(
                                usuario.getId());
        if (adminActiveInstitution.isPresent()) {
            return adminActiveInstitution.get();
        }

        // 2) Si el usuario está vinculado directamente, usar esa relación
        if (usuario.getInstitution() != null) {
            return usuario.getInstitution();
        }

        // 3) Fallback: institución activa donde el email del usuario es el contacto
        if (usuario.getEmail() != null && !usuario.getEmail().isBlank()) {
            Optional<Institution> byContactEmail =
                    institutionRepository
                            .findFirstByEmailContactoIgnoreCaseAndPlanActivoTrueOrderByFechaFinPlanDesc(
                                    usuario.getEmail());
            if (byContactEmail.isPresent()) {
                return byContactEmail.get();
            }
        }

        // 4) Último fallback: última institución administrada
        return institutionRepository
                .findFirstByUsuarioAdminIdOrderByCreatedAtDesc(usuario.getId())
                .orElse(null);
    }

    /**
     * Suscribe un usuario a un plan Premium.
     *
     * @param usuarioId ID del usuario
     * @return Suscripción creada
     * @throws IllegalArgumentException si el usuario no existe, ya tiene suscripción activa, o
     *     tiene un plan institucional activo
     */
    @Transactional
    public Suscripcion suscribirse(Long usuarioId) {
        Usuario usuario =
                usuarioRepository
                        .findById(usuarioId)
                        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // Verificar si tiene plan institucional activo
        if (tienePlanInstitucionalActivo(usuario)) {
            throw new IllegalArgumentException(
                    "No puedes suscribirte a un plan individual mientras tengas un plan"
                            + " institucional activo");
        }

        // Verificar si ya tiene una suscripción activa
        Optional<Suscripcion> suscripcionActiva = obtenerMiSuscripcion(usuarioId);
        if (suscripcionActiva.isPresent()) {
            throw new IllegalArgumentException("Ya tienes una suscripción activa");
        }
        Suscripcion suscripcion = Suscripcion.suscribir();
        suscripcion.setUsuario(usuario);

        Suscripcion guardada = suscripcionRepository.save(suscripcion);
        return guardada;
    }

    /**
     * Cancela la suscripción de un usuario.
     *
     * @param usuarioId ID del usuario
     * @return Suscripción cancelada
     * @throws IllegalArgumentException si no tiene suscripción activa
     */
    @Transactional
    public Suscripcion cancelarSuscripcion(Long usuarioId) {
        Suscripcion suscripcion =
                suscripcionRepository
                        .findByUsuarioIdAndActiva(usuarioId, true)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "No tienes una suscripción activa"));

        suscripcion.cancelar();
        Suscripcion cancelada = suscripcionRepository.save(suscripcion);
        return cancelada;
    }

    /**
     * Renueva la suscripción de un usuario.
     *
     * @param usuarioId ID del usuario
     * @return Suscripción renovada
     * @throws IllegalArgumentException si no tiene suscripción
     */
    @Transactional
    public Suscripcion renovarSuscripcion(Long usuarioId) {
        Suscripcion suscripcion =
                suscripcionRepository
                        .findByUsuarioId(usuarioId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "El usuario no tiene suscripción"));

        suscripcion.renovar();
        Suscripcion renovada = suscripcionRepository.save(suscripcion);
        return renovada;
    }

    /**
     * Activa la suscripción individual tras confirmación de pago por Stripe. Crea o reactiva la
     * Suscripcion, registra la TransaccionPago y actualiza el plan del Usuario.
     *
     * @param usuarioId ID del usuario extraído de los metadata de Stripe
     * @param monto monto cobrado (ya convertido de centavos a euros)
     * @param periodo periodo de la suscripción ("mensual" o "anual")
     * @param plan plan individual contratado (PREMIUM o PRO)
     */
    @Transactional
    public void activarSuscripcionTrasStripe(
            Long usuarioId, BigDecimal monto, String periodo, TipoPlan plan) {
        Usuario usuario =
                usuarioRepository
                        .findById(usuarioId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Usuario no encontrado: " + usuarioId));

        // Verificar si tiene plan institucional activo
        if (tienePlanInstitucionalActivo(usuario)) {
            throw new IllegalArgumentException(
                    "No se puede activar suscripción individual con plan institucional activo");
        }

        TipoPlan planSolicitado = (plan == null || plan == TipoPlan.FREE) ? TipoPlan.PREMIUM : plan;

        // 1. Crear o reutilizar suscripción
        Optional<Suscripcion> existente = suscripcionRepository.findByUsuarioId(usuarioId);
        Suscripcion suscripcion;

        if (existente.isPresent()) {
            // Ya tenía una suscripción anterior (cancelada o expirada): reactivar
            suscripcion = existente.get();
            suscripcion.setPeriodo(periodo != null ? periodo.toUpperCase() : "MENSUAL");
            suscripcion.renovar(planSolicitado);

        } else {
            // Primera vez
            suscripcion = Suscripcion.suscribir(periodo, planSolicitado);
            suscripcion.setUsuario(usuario);
        }
        suscripcionRepository.save(suscripcion);

        // 2. Registrar transacción de pago
        paymentService.procesarPagoExitoso(
                usuarioId,
                TipoTransaccion.SUSCRIPCION,
                monto,
                "Suscripción " + planSolicitado.name() + " activada vía Stripe",
                null);

        // 3. Actualizar plan del usuario
        usuario.setPlan(planSolicitado);
        usuarioRepository.save(usuario);
    }

    /** Compatibilidad: activa suscripción asumiendo plan PREMIUM. */
    @Transactional
    public void activarSuscripcionTrasStripe(Long usuarioId, BigDecimal monto, String periodo) {
        activarSuscripcionTrasStripe(usuarioId, monto, periodo, TipoPlan.PREMIUM);
    }

    /** Sobrecarga sin periodo para compatibilidad con webhooks que no pasan periodo. */
    @Transactional
    public void activarSuscripcionTrasStripe(Long usuarioId, BigDecimal monto) {
        activarSuscripcionTrasStripe(usuarioId, monto, "MENSUAL", TipoPlan.PREMIUM);
    }

    /**
     * Renueva la suscripción PREMIUM tras cobro recurrente exitoso de Stripe. Renueva la
     * Suscripcion y registra la TransaccionPago.
     *
     * @param usuarioId ID del usuario extraído de los metadata de Stripe
     * @param monto monto cobrado (ya convertido de centavos a euros)
     */
    @Transactional
    public void renovarSuscripcionTrasStripe(Long usuarioId, BigDecimal monto) {
        renovarSuscripcionTrasStripe(usuarioId, monto, TipoPlan.PREMIUM);
    }

    /**
     * Renueva la suscripción individual tras cobro recurrente exitoso de Stripe para el plan
     * indicado.
     */
    @Transactional
    public void renovarSuscripcionTrasStripe(Long usuarioId, BigDecimal monto, TipoPlan plan) {
        TipoPlan planSolicitado = (plan == null || plan == TipoPlan.FREE) ? TipoPlan.PREMIUM : plan;

        // 1. Renovar suscripción
        Suscripcion suscripcion =
                suscripcionRepository
                        .findByUsuarioId(usuarioId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "No se encontró suscripción para renovar. Usuario: "
                                                        + usuarioId));

        suscripcion.renovar(planSolicitado);
        suscripcionRepository.save(suscripcion);

        Usuario usuario =
                usuarioRepository
                        .findById(usuarioId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Usuario no encontrado: " + usuarioId));
        usuario.setPlan(planSolicitado);
        usuarioRepository.save(usuario);

        // 2. Registrar transacción de pago
        paymentService.procesarPagoExitoso(
                usuarioId,
                TipoTransaccion.SUSCRIPCION,
                monto,
                "Renovación " + planSolicitado.name() + " vía Stripe",
                null);
    }
}
