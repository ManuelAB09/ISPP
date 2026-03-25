package es.us.meerkat.backend.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import es.us.meerkat.backend.dto.AvailabilitySlot;
import es.us.meerkat.backend.dto.ConnectClassroomRequest;
import es.us.meerkat.backend.dto.CreateTutorRequest;
import es.us.meerkat.backend.dto.MessageResponse;
import es.us.meerkat.backend.dto.PageInfo;
import es.us.meerkat.backend.dto.PaymentUrlResponse;
import es.us.meerkat.backend.dto.TutorListResponse;
import es.us.meerkat.backend.dto.TutorProfileResponse;
import es.us.meerkat.backend.dto.TutorResponse;
import es.us.meerkat.backend.dto.UbicacionResponse;
import es.us.meerkat.backend.dto.UpdateTutorRequest;
import es.us.meerkat.backend.entity.Tutor;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.SolicitudContratacionDirectaRepository;
import es.us.meerkat.backend.service.GoogleCalendarService;
import es.us.meerkat.backend.service.PaymentService;
import es.us.meerkat.backend.service.TutorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Controlador REST para gestionar perfiles de tutores. */
@Slf4j
@RestController
@RequestMapping("/api/v1/tutors")
@RequiredArgsConstructor
@Tag(name = "Tutores", description = "Gestión de perfiles de tutor y verificación")
public class TutorController {
    /**
     * Obtiene el tutor por el id de usuario asociado.
     *
     * @param usuarioId id del usuario
     * @return TutorResponse con el id del tutor o 404 si no existe
     */
    @GetMapping("/user/{usuarioId}")
    @Operation(
            summary = "Obtener tutor por usuarioId",
            description =
                    "Devuelve el tutor asociado a un usuarioId (id de usuario) o 404 si no existe")
    public ResponseEntity<TutorResponse> getTutorByUsuarioId(@PathVariable Long usuarioId) {
        return tutorService
                .obtenerTutorPorUsuarioId(usuarioId)
                .map(tutor -> ResponseEntity.ok(toTutorResponse(tutor)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private final TutorService tutorService;
    private final PaymentService paymentService;
    private final GoogleCalendarService googleCalendarService;
    private final SolicitudContratacionDirectaRepository solicitudRepository;

    /**
     * Lista tutores disponibles con filtros opcionales.
     *
     * @param especialidad Filtro por especialidad
     * @param verificado Filtro por verificación
     * @param tarifaMin Tarifa mínima
     * @param tarifaMax Tarifa máxima
     * @param page Número de página
     * @param size Tamaño de página
     * @return Lista paginada de tutores
     */
    @GetMapping
    public ResponseEntity<TutorListResponse> listTutors(
            @RequestParam(required = false) String especialidad,
            @RequestParam(required = false) BigDecimal tarifaMin,
            @RequestParam(required = false) BigDecimal tarifaMax,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<TutorProfileResponse> tutores =
                tutorService.obtenerTutoresVerificados(
                        especialidad, tarifaMin, tarifaMax, page, size);

        var pageInfo =
                PageInfo.builder()
                        .number(page)
                        .size(size)
                        .totalElements(tutores.getTotalElements())
                        .totalPages(tutores.getTotalPages())
                        .first(tutores.isFirst())
                        .last(tutores.isLast())
                        .build();

        return ResponseEntity.ok(
                TutorListResponse.builder().content(tutores.getContent()).page(pageInfo).build());
    }

    /**
     * Crea un nuevo perfil de tutor para el usuario autenticado.
     *
     * @param usuario Usuario autenticado
     * @param request Datos del nuevo tutor
     * @return Perfil de tutor creado
     */
    @PostMapping
    @Operation(
            summary = "Crear perfil de tutor",
            description = "El usuario autenticado crea su perfil de tutor")
    public ResponseEntity<?> createTutor(
            @AuthenticationPrincipal final Usuario usuario,
            @Valid @RequestBody CreateTutorRequest request) {
        try {
            Tutor tutor = tutorService.crearPerfil(usuario.getId(), request);
            return ResponseEntity.status(HttpStatus.CREATED).body(toTutorResponse(tutor));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(
                            Map.of(
                                    "error",
                                    e.getMessage() != null
                                            ? e.getMessage()
                                            : "Error al crear el perfil de tutor"));
        }
    }

    /**
     * Obtiene el perfil de tutor del usuario autenticado.
     *
     * @param usuario Usuario autenticado
     * @return Perfil de tutor
     */
    @GetMapping("/me")
    @Operation(
            summary = "Obtener mi perfil de tutor",
            description = "Devuelve el perfil de tutor del usuario autenticado")
    public ResponseEntity<TutorResponse> getMyTutorProfile(
            @AuthenticationPrincipal final Usuario usuario) {
        return tutorService
                .obtenerTutorPorUsuarioId(usuario.getId())
                .map(tutor -> ResponseEntity.ok(toTutorResponse(tutor)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Actualiza el perfil de tutor del usuario autenticado.
     *
     * @param usuario Usuario autenticado
     * @param request Datos actualizados
     * @return Perfil actualizado
     */
    @PutMapping("/me")
    @Operation(
            summary = "Actualizar mi perfil de tutor",
            description = "Actualiza el perfil de tutor del usuario autenticado")
    public ResponseEntity<TutorResponse> updateMyTutorProfile(
            @AuthenticationPrincipal final Usuario usuario,
            @Valid @RequestBody UpdateTutorRequest request) {
        try {
            Tutor tutor = tutorService.actualizarPerfil(usuario.getId(), request);
            return ResponseEntity.ok(toTutorResponse(tutor));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Obtiene el perfil público de un tutor específico.
     *
     * @param tutorId ID del tutor
     * @return Perfil del tutor
     */
    @GetMapping("/{tutorId}")
    @Operation(
            summary = "Obtener perfil de tutor",
            description = "Devuelve el perfil público de un tutor específico")
    public ResponseEntity<TutorResponse> getTutorById(
            @Parameter(description = "ID del tutor") @PathVariable Long tutorId) {
        return tutorService
                .obtenerTutorPorId(tutorId)
                .map(tutor -> ResponseEntity.ok(toTutorResponse(tutor)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Devuelve las franjas de disponibilidad de un tutor en un rango de fechas. */
    @GetMapping("/{tutorId}/availability")
    public ResponseEntity<?> getAvailability(
            @PathVariable Long tutorId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false, defaultValue = "30") int slotMinutes) {

        var tutorOpt = tutorService.obtenerTutorPorId(tutorId);
        if (tutorOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        LocalDate startDate = from != null ? from : LocalDate.now();
        LocalDate endDate = to != null ? to : startDate.plusDays(7);

        // Obtener busy intervals desde Google Calendar si aplica
        LocalDateTime desde = startDate.atTime(0, 0);
        LocalDateTime hasta = endDate.atTime(23, 59);
        List<AvailabilitySlot> busyIntervals = Collections.emptyList();
        try {
            busyIntervals =
                    googleCalendarService.obtenerBusyIntervals(
                            tutorOpt.get().getUsuario().getId(), desde, hasta);
        } catch (Exception e) {
            // Ignorar si no se puede consultar GCal
        }

        List<AvailabilitySlot> slots = new ArrayList<>();

        // Default daily working hours (could be enhanced later)
        LocalTime workStart = LocalTime.of(8, 0);
        LocalTime workEnd = LocalTime.of(20, 0);

        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            LocalTime cursor = workStart;
            while (cursor.plusMinutes(slotMinutes).isBefore(workEnd)
                    || cursor.plusMinutes(slotMinutes).equals(workEnd)) {
                LocalDateTime s = LocalDateTime.of(d, cursor);
                LocalDateTime e = s.plusMinutes(slotMinutes);

                boolean conflict = false;

                // Check DB conflicts (any non-cancelled state)
                try {
                    var conflictos =
                            solicitudRepository.findConflictingBookingsAnyState(
                                    tutorId, d, s.toLocalTime(), e.toLocalTime());
                    conflict = !conflictos.isEmpty();
                } catch (Exception ex) {
                    conflict = false;
                }

                // Check Google busy intervals overlap
                if (!conflict) {
                    for (var b : busyIntervals) {
                        if (!(e.isBefore(b.getStart()) || s.isAfter(b.getEnd()))) {
                            conflict = true;
                            break;
                        }
                    }
                }

                slots.add(new AvailabilitySlot(s, e, !conflict));

                cursor = cursor.plusMinutes(slotMinutes);
            }
        }

        return ResponseEntity.ok(slots);
    }

    /**
     * Verifica directamente el tutor (sin pago, para demo/pruebas).
     *
     * @param usuario Usuario autenticado
     * @return Perfil actualizado con verificado=true
     */
    @PostMapping("/me/verificar")
    @Operation(summary = "Verificar tutor", description = "Activa la verificación del tutor")
    public ResponseEntity<TutorResponse> verificarTutor(
            @AuthenticationPrincipal final Usuario usuario) {
        try {
            Tutor tutor =
                    tutorService
                            .obtenerTutorPorUsuarioId(usuario.getId())
                            .orElseThrow(
                                    () ->
                                            new IllegalArgumentException(
                                                    "No tienes perfil de tutor"));
            tutorService.activarVerificacion(tutor.getId());
            Tutor tutorActualizado = tutorService.obtenerTutorPorId(tutor.getId()).orElseThrow();
            return ResponseEntity.ok(toTutorResponse(tutorActualizado));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Solicita la verificación del tutor mediante pago.
     *
     * @param usuario Usuario autenticado
     * @return URL de pago
     */
    @PostMapping("/me/verification")
    @Operation(
            summary = "Solicitar verificación de tutor",
            description =
                    "Inicia el proceso de verificación del tutor mediante pago en Stripe. "
                            + "Coste: 19.99€. Beneficio: insignia 'Verificado' en el perfil.")
    public ResponseEntity<?> requestTutorVerification(
            @AuthenticationPrincipal final Usuario usuario) {

        // Verificar que tiene perfil de tutor
        var tutorOpt = tutorService.obtenerTutorPorUsuarioId(usuario.getId());
        if (tutorOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Tutor tutor = tutorOpt.get();

        // Ya está verificado
        if (Boolean.TRUE.equals(tutor.getVerificado())) {
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("error", "Tu perfil ya está verificado"));
        }

        // Ya tiene un pago pendiente
        if (tutorService.tienePagoVerificacionPendiente(tutor.getId())) {
            return ResponseEntity.badRequest()
                    .body(
                            java.util.Map.of(
                                    "error", "Ya tienes una solicitud de verificación pendiente"));
        }

        try {
            PaymentUrlResponse paymentUrl =
                    paymentService.generarPagoVerificacionTutor(tutor.getId(), usuario.getId());

            // Devolver URL de pago + resumen del coste y beneficio
            return ResponseEntity.ok(
                    java.util.Map.of(
                            "paymentUrl",
                            paymentUrl.paymentUrl(),
                            "sessionId",
                            paymentUrl.sessionId(),
                            "resumen",
                            java.util.Map.of(
                                    "coste",
                                    "19.99€",
                                    "beneficio",
                                    "Insignia 'Verificado' en tu perfil",
                                    "descripcion",
                                    "Tu perfil aparecerá destacado en los listados de"
                                            + " tutores")));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(
                            java.util.Map.of(
                                    "error",
                                    "Error al conectar con la pasarela de pago. Por favor,"
                                            + " inténtalo de nuevo.",
                                    "detalle",
                                    e.getMessage()));
        }
    }

    /**
     * Conecta Google Classroom al perfil de tutor.
     *
     * @param usuario Usuario autenticado
     * @param request Datos de conexión
     * @return Perfil actualizado
     */
    @PostMapping("/me/classroom")
    @Operation(
            summary = "Conectar Google Classroom",
            description = "Conecta la cuenta de Google Classroom del tutor")
    public ResponseEntity<TutorResponse> connectClassroom(
            @AuthenticationPrincipal final Usuario usuario,
            @Valid @RequestBody ConnectClassroomRequest request) {
        try {
            Tutor tutor = tutorService.conectarClassroom(usuario.getId(), request.getGoogleEmail());
            return ResponseEntity.ok(toTutorResponse(tutor));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Desconecta Google Classroom del perfil de tutor.
     *
     * @param usuario Usuario autenticado
     * @return Mensaje de confirmación
     */
    @DeleteMapping("/me/classroom")
    @Operation(
            summary = "Desconectar Google Classroom",
            description = "Desvincula la cuenta de Google Classroom del tutor")
    public ResponseEntity<MessageResponse> disconnectClassroom(
            @AuthenticationPrincipal final Usuario usuario) {
        try {
            tutorService.desconectarClassroom(usuario.getId());
            return ResponseEntity.ok(
                    MessageResponse.builder()
                            .message("Google Classroom desconectado exitosamente")
                            .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Convierte una entidad Tutor en su DTO de respuesta.
     *
     * @param tutor la entidad tutor
     * @return DTO de tutor
     */
    private TutorResponse toTutorResponse(Tutor tutor) {
        return TutorResponse.builder()
                .id(tutor.getId())
                .usuario(
                        tutor.getUsuario() != null
                                ? TutorResponse.UsuarioDto.builder()
                                        .id(tutor.getUsuario().getId())
                                        .nombre(tutor.getUsuario().getNombre())
                                        .foto(tutor.getUsuario().getFoto())
                                        .build()
                                : null)
                .biografia(tutor.getBio())
                .disponibilidad(tutor.getDisponibilidad())
                .tarifaPorHora(tutor.getTarifaHora())
                .especialidades(tutor.getEspecialidades() != null ? new java.util.ArrayList<>(tutor.getEspecialidades()) : java.util.List.of())
                .verificado(tutor.getVerificado())
                .classroomConectado(tutor.getClassroomConectado())
                .createdAt(tutor.getCreatedAt() != null ? tutor.getCreatedAt().toString() : null)
                .ubicacion(
                        tutor.getUsuario() != null && tutor.getUsuario().getUbicacion() != null
                                ? UbicacionResponse.builder()
                                        .id(tutor.getUsuario().getUbicacion().getId())
                                        .nombre(tutor.getUsuario().getUbicacion().getNombre())
                                        .direccion(tutor.getUsuario().getUbicacion().getDireccion())
                                        .latitud(tutor.getUsuario().getUbicacion().getLatitud())
                                        .longitud(tutor.getUsuario().getUbicacion().getLongitud())
                                        .tipo(tutor.getUsuario().getUbicacion().getTipo())
                                        .coste(tutor.getUsuario().getUbicacion().getCoste())
                                        .build()
                                : null)
                .build();
    }

    @PostMapping("/me/create-verification-payment-intent")
    @Operation(
            summary = "Crear PaymentIntent para verificación de tutor",
            description = "Devuelve el clientSecret para usar con Stripe Elements embebido")
    public ResponseEntity<?> crearVerificationPaymentIntent(
            @AuthenticationPrincipal final Usuario usuario) {

        var tutorOpt = tutorService.obtenerTutorPorUsuarioId(usuario.getId());
        if (tutorOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Tutor tutor = tutorOpt.get();
        if (Boolean.TRUE.equals(tutor.getVerificado())) {
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("error", "Tu perfil ya está verificado"));
        }

        try {
            java.util.Map<String, String> result =
                    paymentService.crearPaymentIntentVerificacion(
                            tutor.getId(), usuario.getId(), usuario.getEmail());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(
                            java.util.Map.of(
                                    "error",
                                    "Error al crear el intent de pago: " + e.getMessage()));
        }
    }

    @PostMapping("/me/confirm-verification-payment")
    @Operation(
            summary = "Confirmar pago de verificación de tutor",
            description =
                    "Confirma el PaymentIntent de verificación y activa la insignia Verificado")
    public ResponseEntity<?> confirmarPagoVerificacion(
            @AuthenticationPrincipal final Usuario usuario,
            @RequestBody java.util.Map<String, String> body) {

        String paymentIntentId = body.get("paymentIntentId");
        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("error", "paymentIntentId requerido"));
        }

        try {
            com.stripe.model.PaymentIntent intent =
                    com.stripe.model.PaymentIntent.retrieve(paymentIntentId);

            if (!"succeeded".equals(intent.getStatus())) {
                return ResponseEntity.badRequest()
                        .body(
                                java.util.Map.of(
                                        "error",
                                        "El pago no se ha completado: " + intent.getStatus()));
            }

            String usuarioIdMeta = intent.getMetadata().get("usuarioId");
            if (!usuario.getId().toString().equals(usuarioIdMeta)) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                        .body(
                                java.util.Map.of(
                                        "error", "PaymentIntent no válido para este usuario"));
            }

            String tutorIdStr = intent.getMetadata().get("tutorId");
            if (tutorIdStr == null) {
                return ResponseEntity.badRequest()
                        .body(java.util.Map.of("error", "PaymentIntent sin tutorId en metadata"));
            }
            Long tutorId = Long.parseLong(tutorIdStr);

            tutorService.activarVerificacion(tutorId);

            java.math.BigDecimal monto =
                    java.math.BigDecimal.valueOf(intent.getAmount())
                            .divide(java.math.BigDecimal.valueOf(100));
            Tutor tutor = tutorService.obtenerTutorPorId(tutorId).orElse(null);
            paymentService.procesarPagoExitoso(
                    usuario.getId(),
                    es.us.meerkat.backend.entity.TipoTransaccion.PAGO_VERIFICACION,
                    monto,
                    "Verificación de tutor completada vía Stripe Elements",
                    tutor);

            return ResponseEntity.ok(
                    java.util.Map.of("mensaje", "Verificación activada correctamente"));

        } catch (com.stripe.exception.StripeException e) {
            return ResponseEntity.internalServerError()
                    .body(java.util.Map.of("error", "Error Stripe: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/me/verify-verification-session")
    @Operation(
            summary = "Verificar sesión de pago de verificación",
            description =
                    "Confirma el pago de verificación del tutor usando el sessionId de Stripe")
    public ResponseEntity<?> verificarSesionVerificacion(
            @AuthenticationPrincipal final Usuario usuario, @RequestBody Map<String, String> body) {

        String sessionId = body.get("sessionId");

        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "sessionId requerido"));
        }

        try {
            // 1. Recuperar la sesión de Stripe y verificar que está completada
            com.stripe.model.checkout.Session session =
                    com.stripe.model.checkout.Session.retrieve(sessionId);

            if (!"complete".equals(session.getStatus())) {
                return ResponseEntity.badRequest()
                        .body(
                                Map.of(
                                        "error",
                                        "El pago no está completado: " + session.getStatus()));
            }

            // 2. Verificar que la sesión pertenece al usuario autenticado
            String usuarioIdEnSession = session.getMetadata().get("usuarioId");
            if (!usuario.getId().toString().equals(usuarioIdEnSession)) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Sesión no válida para este usuario"));
            }

            // 3. Verificar que es una sesión de verificación de tutor
            String tipo = session.getMetadata().get("tipo");
            if (!es.us.meerkat.backend.entity.TipoTransaccion.PAGO_VERIFICACION
                    .name()
                    .equals(tipo)) {
                return ResponseEntity.badRequest()
                        .body(
                                Map.of(
                                        "error",
                                        "La sesión no corresponde a una verificación de tutor"));
            }

            // 4. Obtener el tutorId de la metadata
            String tutorIdStr = session.getMetadata().get("tutorId");
            if (tutorIdStr == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Session sin tutorId en metadata"));
            }
            Long tutorId = Long.parseLong(tutorIdStr);

            // 5. Activar verificación del tutor
            tutorService.activarVerificacion(tutorId);

            // 6. Registrar transacción
            BigDecimal monto =
                    session.getAmountTotal() != null
                            ? BigDecimal.valueOf(session.getAmountTotal())
                                    .divide(BigDecimal.valueOf(100))
                            : new BigDecimal("19.99");

            Tutor tutor = tutorService.obtenerTutorPorId(tutorId).orElse(null);
            paymentService.procesarPagoExitoso(
                    usuario.getId(),
                    es.us.meerkat.backend.entity.TipoTransaccion.PAGO_VERIFICACION,
                    monto,
                    "Verificación de tutor completada",
                    tutor);

            // 7. Devolver el perfil actualizado
            Tutor tutorActualizado = tutorService.obtenerTutorPorId(tutorId).orElseThrow();
            return ResponseEntity.ok(
                    Map.of(
                            "mensaje",
                            "Verificación activada correctamente",
                            "verificado",
                            tutorActualizado.getVerificado()));

        } catch (com.stripe.exception.StripeException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error Stripe: " + e.getMessage()));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getClass().getSimpleName() + ": " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // STRIPE CONNECT — onboarding de cuenta conectada para tutores
    // ═══════════════════════════════════════════════════════════════

    @PostMapping("/me/stripe-connect/onboarding")
    @Operation(
            summary = "Iniciar onboarding Stripe Connect",
            description = "Crea una cuenta Express y devuelve la URL de onboarding")
    public ResponseEntity<?> iniciarOnboardingStripeConnect(
            @AuthenticationPrincipal final Usuario usuario, @RequestBody Map<String, String> body) {
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No autenticado");
        }
        try {
            Tutor tutor =
                    tutorService
                            .obtenerTutorPorUsuarioId(usuario.getId())
                            .orElseThrow(
                                    () ->
                                            new IllegalArgumentException(
                                                    "No tienes un perfil de tutor"));

            // Create connected account if it doesn't exist yet
            String accountId = paymentService.crearCuentaConectadaTutor(tutor);

            String returnUrl =
                    body.getOrDefault(
                            "returnUrl", "http://localhost:3000/profesores/" + tutor.getId());
            String onboardingUrl = paymentService.generarOnboardingLink(accountId, returnUrl);

            return ResponseEntity.ok(
                    Map.of(
                            "url", onboardingUrl,
                            "stripeAccountId", accountId));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (com.stripe.exception.StripeException e) {
            log.error("Error Stripe Connect onboarding: {} - {}", e.getCode(), e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(
                            Map.of(
                                    "error",
                                    "Error Stripe: " + e.getUserMessage(),
                                    "code",
                                    e.getCode() != null ? e.getCode() : "unknown"));
        }
    }

    @GetMapping("/me/stripe-connect/status")
    @Operation(
            summary = "Consultar estado de cuenta Stripe Connect",
            description = "Devuelve si la cuenta conectada está activa y puede recibir pagos")
    public ResponseEntity<?> consultarEstadoStripeConnect(
            @AuthenticationPrincipal final Usuario usuario) {
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No autenticado");
        }
        try {
            Tutor tutor =
                    tutorService
                            .obtenerTutorPorUsuarioId(usuario.getId())
                            .orElseThrow(
                                    () ->
                                            new IllegalArgumentException(
                                                    "No tienes un perfil de tutor"));

            if (tutor.getStripeAccountId() == null || tutor.getStripeAccountId().isBlank()) {
                return ResponseEntity.ok(
                        Map.of(
                                "configured", false,
                                "active", false));
            }

            boolean activa = paymentService.cuentaConectadaActiva(tutor.getStripeAccountId());
            return ResponseEntity.ok(
                    Map.of(
                            "configured",
                            true,
                            "active",
                            activa,
                            "stripeAccountId",
                            tutor.getStripeAccountId()));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (com.stripe.exception.StripeException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error Stripe: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // GANANCIAS DEL TUTOR
    // ═══════════════════════════════════════════════════════════════

    @GetMapping("/me/earnings")
    @Operation(
            summary = "Obtener ganancias del tutor",
            description = "Devuelve el historial de pagos recibidos y totales del tutor")
    public ResponseEntity<?> obtenerGanancias(
            @AuthenticationPrincipal final Usuario usuario,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No autenticado");
        }
        try {
            Tutor tutor =
                    tutorService
                            .obtenerTutorPorUsuarioId(usuario.getId())
                            .orElseThrow(
                                    () ->
                                            new IllegalArgumentException(
                                                    "No tienes un perfil de tutor"));

            // Get all completed tutor transactions
            var todas = paymentService.obtenerTodasGananciasTutor(tutor.getId());
            BigDecimal totalBruto = BigDecimal.ZERO;
            BigDecimal totalComisiones = BigDecimal.ZERO;
            for (var t : todas) {
                totalBruto = totalBruto.add(t.getMonto());
                totalComisiones =
                        totalComisiones.add(
                                t.getComision() != null ? t.getComision() : BigDecimal.ZERO);
            }
            BigDecimal totalNeto = totalBruto.subtract(totalComisiones);

            // Paginated list
            var pageable = org.springframework.data.domain.PageRequest.of(page, size);
            var pagina = paymentService.obtenerGananciasTutor(tutor.getId(), pageable);

            var transacciones =
                    pagina.getContent().stream()
                            .map(
                                    t ->
                                            Map.of(
                                                    "id", t.getId(),
                                                    "monto", t.getMonto(),
                                                    "comision",
                                                            t.getComision() != null
                                                                    ? t.getComision()
                                                                    : BigDecimal.ZERO,
                                                    "montoNeto",
                                                            paymentService.calcularMontoNeto(
                                                                    t.getMonto()),
                                                    "estado", t.getEstado().name(),
                                                    "fecha",
                                                            t.getCompletadoAt() != null
                                                                    ? t.getCompletadoAt().toString()
                                                                    : ""))
                            .toList();

            return ResponseEntity.ok(
                    Map.of(
                            "totalBruto", totalBruto,
                            "totalComisiones", totalComisiones,
                            "totalNeto", totalNeto,
                            "totalTransacciones", todas.size(),
                            "content", transacciones,
                            "totalPages", pagina.getTotalPages(),
                            "currentPage", page));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
