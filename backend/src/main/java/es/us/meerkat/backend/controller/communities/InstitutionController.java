package es.us.meerkat.backend.controller.communities;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.bind.annotation.RestController;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;

import es.us.meerkat.backend.dto.CorporatePlanRequest;
import es.us.meerkat.backend.dto.CreateInstitutionRequest;
import es.us.meerkat.backend.dto.InstitutionResponse;
import es.us.meerkat.backend.dto.PaymentUrlResponse;
import es.us.meerkat.backend.dto.UpdateInstitutionRequest;
import es.us.meerkat.backend.entity.Institution;
import es.us.meerkat.backend.entity.TipoPlanCorporativo;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.service.communities.InstitutionService;
import es.us.meerkat.backend.service.suscriptions.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
/**
 * Controlador para manejar operaciones con instituciones educativas y planes corporativos.
 *
 * <p>Base path: /api/v1/institutions
 */
@RestController
@RequestMapping("/api/v1/institutions")
@Tag(
        name = "Institutions",
        description = "Gestión de instituciones educativas y planes corporativos")
@SecurityRequirement(name = "bearer")
public class InstitutionController {

    @Autowired private InstitutionService institutionService;
    @Autowired private PaymentService paymentService;

    // ===============================
    // CRUD OPERATIONS
    // ===============================

    /**
     * Crea una nueva institución.
     *
     * @param usuario Usuario autenticado (administrador)
     * @param request Datos de la institución
     * @return InstitutionResponse creada
     */
    @PostMapping
    @Operation(summary = "Crear nueva institución")
    public ResponseEntity<?> crearInstitucion(
            @AuthenticationPrincipal Usuario usuario,
            @Valid @RequestBody CreateInstitutionRequest request) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Institution institution = institutionService.crearInstitucion(usuario.getId(), request);
            InstitutionResponse response = toInstitutionResponse(institution);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("dominio")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(java.util.Map.of("error", msg));
            }
            return ResponseEntity.badRequest().body(java.util.Map.of("error", msg));
        }
    }

    /**
     * Obtiene los detalles de una institución.
     *
     * @param institutionId ID de la institución
     * @param usuario Usuario autenticado
     * @return InstitutionResponse con detalles
     */
    @GetMapping("/{institutionId}")
    @Operation(summary = "Obtener detalles de una institución")
    public ResponseEntity<InstitutionResponse> obtenerInstitucion(
            @PathVariable Long institutionId, @AuthenticationPrincipal Usuario usuario) {

        Institution institution =
                institutionService.obtenerInstitucion(institutionId, usuario.getId());
        InstitutionResponse response = toInstitutionResponse(institution);

        return ResponseEntity.ok(response);
    }

    /**
     * Actualiza los datos de una institución.
     *
     * @param institutionId ID de la institución
     * @param usuario Usuario autenticado (debe ser administrador)
     * @param request Datos actualizados
     * @return InstitutionResponse actualizada
     */
    @PutMapping("/{institutionId}")
    @Operation(summary = "Actualizar datos de una institución")
    public ResponseEntity<InstitutionResponse> actualizarInstitucion(
            @PathVariable Long institutionId,
            @AuthenticationPrincipal Usuario usuario,
            @Valid @RequestBody UpdateInstitutionRequest request) {

        Institution institution =
                institutionService.actualizarInstitucion(institutionId, usuario.getId(), request);
        InstitutionResponse response = toInstitutionResponse(institution);

        return ResponseEntity.ok(response);
    }

    // ===============================
    // CORPORATE PLANS
    // ===============================

    /**
     * Contrata un plan corporativo para una institución. Genera una URL de pago que el cliente debe
     * procesar.
     *
     * @param institutionId ID de la institución
     * @param usuario Usuario autenticado (administrador)
     * @param request Datos del plan a contratar
     * @return PaymentUrlResponse con URL de pago
     */
    @PostMapping("/{institutionId}/plan")
    @Operation(summary = "Contratar plan corporativo")
    public ResponseEntity<?> contratarPlanCorporativo(
            @PathVariable Long institutionId,
            @AuthenticationPrincipal Usuario usuario,
            @Valid @RequestBody CorporatePlanRequest request) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            PaymentUrlResponse paymentUrl =
                    institutionService.contratarPlanCorporativo(
                            institutionId, usuario.getId(), request);
            return ResponseEntity.ok(paymentUrl);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Error al conectar con la pasarela de pago"));
        }
    }

    /**
     * Cancela el plan corporativo de una institución. Requiere autenticación como administrador de
     * la institución.
     *
     * @param institutionId ID de la institución
     * @param usuario Usuario autenticado (administrador)
     * @return InstitutionResponse actualizada
     */
    @DeleteMapping("/{institutionId}/plan")
    @Operation(summary = "Cancelar plan corporativo")
    public ResponseEntity<InstitutionResponse> cancelarPlanCorporativo(
            @PathVariable Long institutionId, @AuthenticationPrincipal Usuario usuario) {

        institutionService.cancelarPlanCorporativo(institutionId, usuario.getId());

        Institution institution =
                institutionService.obtenerInstitucion(institutionId, usuario.getId());
        InstitutionResponse response = toInstitutionResponse(institution);

        return ResponseEntity.ok(response);
    }

    /**
     * Verifica si el plan corporativo de una institución está activo.
     *
     * @param institutionId ID de la institución
     * @param usuario Usuario autenticado
     * @return Mapa con estado del plan
     */
    @GetMapping("/{institutionId}/plan/status")
    @Operation(summary = "Verificar estado del plan corporativo")
    public ResponseEntity<?> verificarEstadoPlan(
            @PathVariable Long institutionId, @AuthenticationPrincipal Usuario usuario) {

        Institution institution =
                institutionService.obtenerInstitucion(institutionId, usuario.getId());
        boolean planActivo = institutionService.esPlanActivo(institutionId);

        return ResponseEntity.ok(
                new java.util.HashMap<String, Object>() {
                    {
                        put("planActivo", planActivo);
                        put("tipoPlan", institution.getPlanCorporativo());
                        put("fechaInicio", institution.getFechaInicioPlan());
                        put("fechaFin", institution.getFechaFinPlan());
                        put("usuariosPermitidos", institution.getNumUsuariosPermitidos());
                    }
                });
    }

    // ===============================
    // HELPER METHODS
    // ===============================

    /**
     * Convierte una entidad Institution a InstitutionResponse DTO.
     *
     * @param institution Entidad de institución
     * @return InstitutionResponse
     */
    private InstitutionResponse toInstitutionResponse(Institution institution) {
        return InstitutionResponse.builder()
                .id(institution.getId())
                .nombre(institution.getNombre())
                .descripcion(institution.getDescripcion())
                .emailContacto(institution.getEmailContacto())
                .telefonoContacto(institution.getTelefonoContacto())
                .dominioEmail(institution.getDominioEmail())
                .ubicacion(institution.getUbicacion())
                .sitioweb(institution.getSitioweb())
                .logoUrl(institution.getLogoUrl())
                .verificada(institution.getVerificada())
                .planCorporativo(
                        institution.getPlanCorporativo() != null
                                ? institution.getPlanCorporativo().name()
                                : null)
                .planActivo(institution.getPlanActivo())
                .totalUsuarios((int) institutionService.contarUsuarios(institution.getId()))
                .totalComunidades((int) institutionService.contarComunidades(institution.getId()))
                .createdAt(institution.getCreatedAt())
                .updatedAt(institution.getUpdatedAt())
                .build();
    }

    @PostMapping("/{institutionId}/create-plan-payment-intent")
    @Operation(
            summary = "Crear PaymentIntent para plan corporativo",
            description = "Devuelve el clientSecret para usar con Stripe Elements embebido")
    public ResponseEntity<?> crearPlanPaymentIntent(
            @PathVariable Long institutionId,
            @AuthenticationPrincipal Usuario usuario,
            @Valid @RequestBody CorporatePlanRequest request) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Institution institution =
                    institutionService.preconfigurarPlanCorporativo(
                            institutionId, usuario.getId(), request);

            TipoPlanCorporativo tipoPlan = institution.getPlanCorporativo();
            String periodo = request.getPeriodo();
            Integer duracionMeses = request.getDuracionMeses();

            Map<String, String> result =
                    paymentService.crearPaymentIntentPlanCorporativo(
                            institutionId,
                            tipoPlan,
                            periodo,
                            institution.getEmailContacto(),
                            duracionMeses);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error creando PaymentIntent corporativo: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error al crear el intent de pago: " + e.getMessage()));
        }
    }

    @PostMapping("/confirm-plan-payment")
    @Operation(
            summary = "Confirmar pago de plan corporativo con Stripe Elements",
            description = "Verifica el PaymentIntent y activa el plan institucional")
    public ResponseEntity<?> confirmarPagoPlan(@RequestBody Map<String, String> body) {

        String paymentIntentId = body.get("paymentIntentId");
        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "paymentIntentId requerido"));
        }

        try {
            com.stripe.model.PaymentIntent intent =
                    com.stripe.model.PaymentIntent.retrieve(paymentIntentId);

            if (!"succeeded".equals(intent.getStatus())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "El pago no está completado: " + intent.getStatus()));
            }

            Map<String, String> metadata = intent.getMetadata();
            String institucionIdStr = metadata.get("institucionId");
            String duracionStr = metadata.get("duracionMeses");
            String emailContacto = metadata.get("emailContacto");
            String tipoPlanCorporativoStr = metadata.get("tipoPlanCorporativo");

            if (institucionIdStr == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "PaymentIntent sin institucionId en metadata"));
            }

            Long institucionId = Long.parseLong(institucionIdStr);
            Integer duracionMeses = duracionStr != null ? Integer.parseInt(duracionStr) : 12;
            TipoPlanCorporativo tipoPlanCorporativo =
                    tipoPlanCorporativoStr != null
                            ? TipoPlanCorporativo.valueOf(tipoPlanCorporativoStr)
                            : null;

            institutionService.activarPlanCorporativo(
                    institucionId, duracionMeses, emailContacto, tipoPlanCorporativo);
            log.info(
                    "Plan corporativo activado para institución {} vía Stripe Elements",
                    institucionId);

            return ResponseEntity.ok(
                    Map.of("mensaje", "Plan institucional activado correctamente"));

        } catch (StripeException e) {
            log.error("StripeException confirmando pago corporativo: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error Stripe: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error activando plan corporativo: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/verify-session")
    public ResponseEntity<?> verificarSesionCorporativa(@RequestBody Map<String, String> body) {

        String sessionId = body.get("sessionId");
        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "sessionId requerido"));
        }

        try {
            Session session = Session.retrieve(sessionId);
            log.info(
                    "Verificando sesión institucional: {} status: {}",
                    sessionId,
                    session.getStatus());

            if (!"complete".equals(session.getStatus())) {
                return ResponseEntity.badRequest()
                        .body(
                                Map.of(
                                        "error",
                                        "El pago no está completado: " + session.getStatus()));
            }

            // Obtener datos de los metadata de Stripe
            Map<String, String> metadata = session.getMetadata();
            String institucionIdStr = metadata.get("institucionId");
            String duracionStr = metadata.get("duracionMeses");
            String emailContacto = metadata.get("emailContacto");
            String tipoPlanCorporativoStr = metadata.get("tipoPlanCorporativo");

            if (institucionIdStr == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Sesión sin institucionId en metadata"));
            }

            Long institucionId = Long.parseLong(institucionIdStr);
            Integer duracionMeses = duracionStr != null ? Integer.parseInt(duracionStr) : 12;
            TipoPlanCorporativo tipoPlanCorporativo =
                    tipoPlanCorporativoStr != null
                            ? TipoPlanCorporativo.valueOf(tipoPlanCorporativoStr)
                            : null;

            institutionService.activarPlanCorporativo(
                    institucionId, duracionMeses, emailContacto, tipoPlanCorporativo);
            log.info("Plan corporativo activado para institución {}", institucionId);

            return ResponseEntity.ok(
                    Map.of("mensaje", "Plan institucional activado correctamente"));

        } catch (StripeException e) {
            log.error("StripeException verificando sesión institucional: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error Stripe: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error activando plan corporativo: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
