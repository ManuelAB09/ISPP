package es.us.meerkat.backend.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import es.us.meerkat.backend.dto.SolicitudContratacionRequest;
import es.us.meerkat.backend.dto.SolicitudContratacionResponse;
import es.us.meerkat.backend.entity.Tutor;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.service.EmailService;
import es.us.meerkat.backend.service.PaymentService;
import es.us.meerkat.backend.service.SolicitudContratacionService;
import es.us.meerkat.backend.service.TutorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Controlador REST para solicitudes de contratación directa alumno-tutor. */
@RestController
@RequestMapping("/api/v1/solicitudes-contratacion")
@RequiredArgsConstructor
public class SolicitudContratacionController {

    private final SolicitudContratacionService solicitudService;
    private final PaymentService paymentService;
    private final TutorService tutorService;
    private final EmailService emailService;

    /** Crear una solicitud de contratación directa a un tutor. */
    @PostMapping("/tutor/{tutorId}")
    public ResponseEntity<?> crearSolicitud(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable Long tutorId,
            @Valid @RequestBody SolicitudContratacionRequest request) {
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
        }
        try {
            SolicitudContratacionResponse response =
                    solicitudService.crearSolicitud(usuario.getId(), tutorId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Aceptar una solicitud (solo el tutor destinatario). */
    @PostMapping("/{solicitudId}/aceptar")
    public ResponseEntity<?> aceptarSolicitud(
            @AuthenticationPrincipal Usuario usuario, @PathVariable Long solicitudId) {
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
        }
        try {
            SolicitudContratacionResponse response =
                    solicitudService.aceptarSolicitud(solicitudId, usuario.getId());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Rechazar una solicitud (solo el tutor destinatario). */
    @PostMapping("/{solicitudId}/rechazar")
    public ResponseEntity<?> rechazarSolicitud(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable Long solicitudId,
            @RequestBody(required = false) Map<String, String> body) {
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
        }
        try {
            String motivo = body != null ? body.get("motivo") : null;
            SolicitudContratacionResponse response =
                    solicitudService.rechazarSolicitud(solicitudId, usuario.getId(), motivo);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Marcar una solicitud como pagada (solo el alumno). */
    @PostMapping("/{solicitudId}/pagar")
    public ResponseEntity<?> marcarComoPagada(
            @AuthenticationPrincipal Usuario usuario, @PathVariable Long solicitudId) {
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
        }
        try {
            SolicitudContratacionResponse response =
                    solicitudService.marcarComoPagada(solicitudId, usuario.getId());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Obtener solicitudes pendientes del tutor autenticado. */
    @GetMapping("/tutor/pendientes")
    public ResponseEntity<?> obtenerPendientesDelTutor(@AuthenticationPrincipal Usuario usuario) {
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
        }
        try {
            List<SolicitudContratacionResponse> lista =
                    solicitudService.obtenerSolicitudesPendientesDelTutor(usuario.getId());
            return ResponseEntity.ok(lista);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Obtener todas las solicitudes del tutor autenticado. */
    @GetMapping("/tutor")
    public ResponseEntity<?> obtenerSolicitudesDelTutor(@AuthenticationPrincipal Usuario usuario) {
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
        }
        try {
            List<SolicitudContratacionResponse> lista =
                    solicitudService.obtenerSolicitudesDelTutor(usuario.getId());
            return ResponseEntity.ok(lista);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Obtener solicitudes del alumno autenticado. */
    @GetMapping("/alumno")
    public ResponseEntity<?> obtenerSolicitudesDelAlumno(@AuthenticationPrincipal Usuario usuario) {
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
        }
        try {
            List<SolicitudContratacionResponse> lista =
                    solicitudService.obtenerSolicitudesDelAlumno(usuario.getId());
            return ResponseEntity.ok(lista);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Cancelar una reserva (tutor). Notifica al alumno automáticamente. */
    @PostMapping("/{solicitudId}/cancelar")
    public ResponseEntity<?> cancelarSolicitud(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable Long solicitudId,
            @RequestBody(required = false) Map<String, String> body) {
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
        }
        try {
            String motivo = body != null ? body.get("motivo") : null;
            SolicitudContratacionResponse response =
                    solicitudService.cancelarSolicitud(solicitudId, usuario.getId(), motivo);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Reprogramar una reserva (tutor). Cambia fecha/hora y notifica al alumno. */
    @PostMapping("/{solicitudId}/reprogramar")
    public ResponseEntity<?> reprogramarSolicitud(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable Long solicitudId,
            @RequestBody Map<String, String> body) {
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
        }
        try {
            LocalDate nuevoDia = LocalDate.parse(body.get("dia"));
            LocalTime nuevaHoraInicio = LocalTime.parse(body.get("horaInicio"));
            LocalTime nuevaHoraFin = LocalTime.parse(body.get("horaFin"));
            SolicitudContratacionResponse response =
                    solicitudService.reprogramarSolicitud(
                            solicitudId, usuario.getId(), nuevoDia, nuevaHoraInicio, nuevaHoraFin);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Crea un PaymentIntent de Stripe para pagar una solicitud aceptada. */
    @PostMapping("/{solicitudId}/create-payment-intent")
    public ResponseEntity<?> crearPaymentIntent(
            @AuthenticationPrincipal Usuario usuario, @PathVariable Long solicitudId) {
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
        }
        try {
            SolicitudContratacionResponse solicitud =
                    solicitudService.obtenerSolicitudParaPago(solicitudId, usuario.getId());

            // Verify tutor has Stripe Connect configured
            Tutor tutor =
                    tutorService
                            .obtenerTutorPorId(solicitud.getTutorId())
                            .orElseThrow(() -> new IllegalArgumentException("Tutor no encontrado"));
            if (tutor.getStripeAccountId() == null || tutor.getStripeAccountId().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(
                                Map.of(
                                        "error",
                                        "El profesor aún no ha configurado sus datos bancarios para"
                                                + " recibir pagos."));
            }

            Map<String, String> result =
                    paymentService.crearPaymentIntentContratacionTutor(
                            solicitud.getTutorId(),
                            null, // no community
                            solicitud.getImporteTotal(),
                            usuario.getId(),
                            usuario.getEmail());

            // Include solicitudId in the response so frontend can call confirm later
            result.put("solicitudId", solicitudId.toString());
            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error al crear el intent de pago: " + e.getMessage()));
        }
    }

    /** Confirma el pago de Stripe y marca la solicitud como PAGADA. */
    @PostMapping("/{solicitudId}/confirm-payment")
    public ResponseEntity<?> confirmarPago(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable Long solicitudId,
            @RequestBody Map<String, String> body) {
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
        }

        String paymentIntentId = body.get("paymentIntentId");
        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "paymentIntentId requerido"));
        }

        try {
            // Verify payment actually succeeded at Stripe
            com.stripe.model.PaymentIntent intent =
                    com.stripe.model.PaymentIntent.retrieve(paymentIntentId);

            if (!"succeeded".equals(intent.getStatus())) {
                return ResponseEntity.badRequest()
                        .body(
                                Map.of(
                                        "error",
                                        "El pago no se ha completado: " + intent.getStatus()));
            }

            // Verify it's the right user
            String metaUserId = intent.getMetadata().get("usuarioId");
            if (!usuario.getId().toString().equals(metaUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "PaymentIntent no corresponde a este usuario"));
            }

            // Mark solicitud as paid (validates state + ownership)
            SolicitudContratacionResponse response =
                    solicitudService.marcarComoPagada(solicitudId, usuario.getId());

            // Record transaction with actual tutor reference
            java.math.BigDecimal monto =
                    java.math.BigDecimal.valueOf(intent.getAmount())
                            .divide(java.math.BigDecimal.valueOf(100));

            String tutorIdStr = intent.getMetadata().get("tutorId");
            Tutor tutor =
                    tutorIdStr != null
                            ? tutorService
                                    .obtenerTutorPorId(Long.parseLong(tutorIdStr))
                                    .orElse(null)
                            : null;

            paymentService.procesarPagoExitoso(
                    usuario.getId(),
                    es.us.meerkat.backend.entity.TipoTransaccion.PAGO_TUTOR,
                    monto,
                    "Pago contratación directa de tutor",
                    tutor);

            // Enviar emails de confirmación al alumno y al tutor
            try {
                String tutorEmail = tutor != null ? tutor.getUsuario().getEmail() : null;
                String tutorNombre = tutor != null ? tutor.getUsuario().getNombre() : "Tutor";
                if (tutorEmail != null) {
                    emailService.sendBookingConfirmationEmail(
                            usuario.getEmail(),
                            usuario.getNombre(),
                            tutorEmail,
                            tutorNombre,
                            response.getDia(),
                            response.getHoraInicio(),
                            response.getHoraFin(),
                            response.getModalidad() != null ? response.getModalidad() : "ONLINE",
                            response.getImporteTotal());
                }
            } catch (Exception emailEx) {
                // No falla el pago si el email falla
            }

            return ResponseEntity.ok(response);

        } catch (com.stripe.exception.StripeException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error Stripe: " + e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
