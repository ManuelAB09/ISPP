package es.us.meerkat.backend.service;

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
import es.us.meerkat.backend.repository.SuscripcionRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

/** Servicio para gestionar suscripciones de usuarios. */
@Service
@RequiredArgsConstructor
public class SuscripcionService {

    private final SuscripcionRepository suscripcionRepository;
    private final UsuarioRepository usuarioRepository;
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
        if (usuario != null && usuario.getInstitution() != null) {
            Institution institution = usuario.getInstitution();
            response.setInstitutionNombre(institution.getNombre());
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
        if (usuario.getInstitution() == null) {
            return false;
        }
        Institution institution = usuario.getInstitution();
        return institution.getPlanActivo()
                && (institution.getFechaFinPlan() == null
                        || institution.getFechaFinPlan().isAfter(LocalDateTime.now()));
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
     * Activa la suscripción PREMIUM tras confirmación de pago por Stripe. Crea la Suscripcion,
     * registra la TransaccionPago y actualiza el plan del Usuario.
     *
     * @param usuarioId ID del usuario extraído de los metadata de Stripe
     * @param monto monto cobrado (ya convertido de centavos a euros)
     * @param periodo periodo de la suscripción ("mensual" o "anual")
     */
    @Transactional
    public void activarSuscripcionTrasStripe(Long usuarioId, BigDecimal monto, String periodo) {
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

        // 1. Crear o reutilizar suscripción
        Optional<Suscripcion> existente = suscripcionRepository.findByUsuarioId(usuarioId);
        Suscripcion suscripcion;

        if (existente.isPresent()) {
            // Ya tenía una suscripción anterior (cancelada o expirada): reactivar
            suscripcion = existente.get();
            suscripcion.setPeriodo(periodo != null ? periodo.toUpperCase() : "MENSUAL");
            suscripcion.renovar();

        } else {
            // Primera vez
            suscripcion = Suscripcion.suscribir(periodo);
            suscripcion.setUsuario(usuario);
        }
        suscripcionRepository.save(suscripcion);

        // 2. Registrar transacción de pago
        paymentService.procesarPagoExitoso(
                usuarioId,
                TipoTransaccion.SUSCRIPCION,
                monto,
                "Suscripción PREMIUM activada vía Stripe",
                null);

        // 3. Actualizar plan del usuario
        usuario.setPlan(TipoPlan.PREMIUM);
        usuarioRepository.save(usuario);
    }

    /** Sobrecarga sin periodo para compatibilidad con webhooks que no pasan periodo. */
    @Transactional
    public void activarSuscripcionTrasStripe(Long usuarioId, BigDecimal monto) {
        activarSuscripcionTrasStripe(usuarioId, monto, "MENSUAL");
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
        // 1. Renovar suscripción
        Suscripcion suscripcion =
                suscripcionRepository
                        .findByUsuarioId(usuarioId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "No se encontró suscripción para renovar. Usuario: "
                                                        + usuarioId));

        suscripcion.renovar();
        suscripcionRepository.save(suscripcion);

        // 2. Registrar transacción de pago
        paymentService.procesarPagoExitoso(
                usuarioId,
                TipoTransaccion.SUSCRIPCION,
                monto,
                "Renovación PREMIUM vía Stripe",
                null);
    }
}
