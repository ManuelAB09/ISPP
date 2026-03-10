package es.us.meerkat.backend.controller;

import java.math.BigDecimal;
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

import es.us.meerkat.backend.dto.ConnectClassroomRequest;
import es.us.meerkat.backend.dto.CreateTutorRequest;
import es.us.meerkat.backend.dto.HiringRequestListResponse;
import es.us.meerkat.backend.dto.HiringRequestResponse;
import es.us.meerkat.backend.dto.MessageResponse;
import es.us.meerkat.backend.dto.PageInfo;
import es.us.meerkat.backend.dto.PaymentUrlResponse;
import es.us.meerkat.backend.dto.RejectHiringRequest;
import es.us.meerkat.backend.dto.TutorListResponse;
import es.us.meerkat.backend.dto.TutorProfileResponse;
import es.us.meerkat.backend.dto.TutorResponse;
import es.us.meerkat.backend.dto.UbicacionResponse;
import es.us.meerkat.backend.dto.UpdateTutorRequest;
import es.us.meerkat.backend.dto.UserSimpleResponse;
import es.us.meerkat.backend.entity.Tutor;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.service.PaymentService;
import es.us.meerkat.backend.service.TutorContratacionService;
import es.us.meerkat.backend.service.TutorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Controlador REST para gestionar perfiles de tutores. */
@RestController
@RequestMapping("/api/v1/tutors")
@RequiredArgsConstructor
@Tag(name = "Tutores", description = "Gestión de perfiles de tutor y verificación")
public class TutorController {

    private final TutorService tutorService;
    private final PaymentService paymentService;
    private final TutorContratacionService tutorContratacionService;

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
    public ResponseEntity<TutorResponse> createTutor(
            @AuthenticationPrincipal final Usuario usuario,
            @Valid @RequestBody CreateTutorRequest request) {
        try {
            Tutor tutor = tutorService.crearPerfil(usuario.getId(), request);
            return ResponseEntity.status(HttpStatus.CREATED).body(toTutorResponse(tutor));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
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
                .especialidades(tutor.getEspecialidades())
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

    // =====================================================
    // GESTIÓN DE SOLICITUDES DE CONTRATACIÓN
    // =====================================================

    /**
     * Obtiene las solicitudes de contratación pendientes para el tutor autenticado.
     *
     * @param usuario Usuario autenticado (tutor)
     * @param page Número de página
     * @param size Tamaño de página
     * @return Lista paginada de solicitudes pendientes
     */
    @GetMapping("/me/hiring-requests")
    @Operation(
            summary = "Obtener solicitudes de contratación",
            description = "Lista las solicitudes pendientes de aprobación para el tutor")
    public ResponseEntity<HiringRequestListResponse> getMyHiringRequests(
            @AuthenticationPrincipal final Usuario usuario,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            var tutorOpt = tutorService.obtenerTutorPorUsuarioId(usuario.getId());
            if (tutorOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Tutor tutor = tutorOpt.get();
            org.springframework.data.domain.Pageable pageable =
                    org.springframework.data.domain.PageRequest.of(page, size);

            var contrataciones =
                    tutorContratacionService.obtenerSolicitudesPendientes(tutor.getId(), pageable);

            Page<HiringRequestResponse> response =
                    contrataciones.map(
                            c ->
                                    HiringRequestResponse.builder()
                                            .id(c.getId())
                                            .comunidad(
                                                    HiringRequestResponse.ComunidadSimpleDto
                                                            .builder()
                                                            .id(c.getComunidad().getId())
                                                            .nombre(c.getComunidad().getNombre())
                                                            .descripcion(
                                                                    c.getComunidad()
                                                                            .getDescripcion())
                                                            .foto(c.getComunidad().getImagenUrl())
                                                            .creador(
                                                                    new UserSimpleResponse(
                                                                            c.getComunidad()
                                                                                    .getCreador()
                                                                                    .getId(),
                                                                            c.getComunidad()
                                                                                    .getCreador()
                                                                                    .getNombre(),
                                                                            c.getComunidad()
                                                                                    .getCreador()
                                                                                    .getEmail(),
                                                                            c.getComunidad()
                                                                                    .getCreador()
                                                                                    .getFoto()))
                                                            .build())
                                            .modalidad(c.getModalidad())
                                            .duracion(c.getDuracion())
                                            .tarifaAcordada(c.getTarifaAcordada())
                                            .estado(c.getEstado().name())
                                            .createdAt(c.getCreatedAt())
                                            .build());

            return ResponseEntity.ok(new HiringRequestListResponse(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Acepta una solicitud de contratación.
     *
     * @param usuario Usuario autenticado (tutor)
     * @param requestId ID de la solicitud
     * @return URL de pago para la comunidad
     */
    @PostMapping("/me/hiring-requests/{requestId}/accept")
    @Operation(
            summary = "Aceptar solicitud de contratación",
            description =
                    "El tutor acepta una solicitud de contratación. Se genera URL de pago para"
                            + " la comunidad.")
    public ResponseEntity<?> acceptHiringRequest(
            @AuthenticationPrincipal final Usuario usuario, @PathVariable Long requestId) {
        try {
            var tutorOpt = tutorService.obtenerTutorPorUsuarioId(usuario.getId());
            if (tutorOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Tutor tutor = tutorOpt.get();
            PaymentUrlResponse paymentUrl =
                    tutorContratacionService.aceptarSolicitud(requestId, tutor.getId());

            return ResponseEntity.ok(
                    Map.of(
                            "mensaje",
                            "Solicitud aceptada correctamente",
                            "paymentUrl",
                            paymentUrl.paymentUrl(),
                            "sessionId",
                            paymentUrl.sessionId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error al aceptar la solicitud: " + e.getMessage()));
        }
    }

    /**
     * Rechaza una solicitud de contratación.
     *
     * @param usuario Usuario autenticado (tutor)
     * @param requestId ID de la solicitud
     * @param request Motivo del rechazo
     * @return Mensaje de confirmación
     */
    @PostMapping("/me/hiring-requests/{requestId}/reject")
    @Operation(
            summary = "Rechazar solicitud de contratación",
            description = "El tutor rechaza una solicitud de contratación")
    public ResponseEntity<?> rejectHiringRequest(
            @AuthenticationPrincipal final Usuario usuario,
            @PathVariable Long requestId,
            @Valid @RequestBody RejectHiringRequest request) {
        try {
            var tutorOpt = tutorService.obtenerTutorPorUsuarioId(usuario.getId());
            if (tutorOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Tutor tutor = tutorOpt.get();
            tutorContratacionService.rechazarSolicitud(
                    requestId, tutor.getId(), request.getMotivo());

            return ResponseEntity.ok(
                    MessageResponse.builder().message("Solicitud rechazada correctamente").build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error al rechazar la solicitud: " + e.getMessage()));
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
}
