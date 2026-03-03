package es.us.meerkat.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import es.us.meerkat.backend.dto.CorporatePlanRequest;
import es.us.meerkat.backend.dto.CreateInstitutionRequest;
import es.us.meerkat.backend.dto.InstitutionResponse;
import es.us.meerkat.backend.dto.PaymentUrlResponse;
import es.us.meerkat.backend.dto.UpdateInstitutionRequest;
import es.us.meerkat.backend.entity.Institution;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.service.InstitutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

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
                .totalComunidades(
                        (int) institutionService.contarComunidades(institution.getId())) // TODO:
                // calcular
                // desde
                // comunidades
                .createdAt(institution.getCreatedAt())
                .updatedAt(institution.getUpdatedAt())
                .build();
    }
}
