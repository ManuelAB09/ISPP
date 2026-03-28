package es.us.meerkat.backend.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import es.us.meerkat.backend.dto.CreateInvitacionRequest;
import es.us.meerkat.backend.dto.InvitacionListResponse;
import es.us.meerkat.backend.dto.InvitacionResponse;
import es.us.meerkat.backend.dto.MessageResponse;
import es.us.meerkat.backend.entity.InvitacionMiembro;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.service.communities.InvitacionMiembroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controlador REST para la gestión de invitaciones a comunidades. Base URL:
 * /api/v1/communities/{communityId}/invitations
 */
@RestController
@RequestMapping("/api/v1/communities/{communityId}/invitations")
@Tag(name = "Invitaciones", description = "Gestión de invitaciones a miembros en comunidades")
@RequiredArgsConstructor
public class InvitacionMiembroController {

    private final InvitacionMiembroService invitacionService;

    /**
     * Obtiene las invitaciones de una comunidad (solo para administradores).
     *
     * @param communityId ID de la comunidad
     * @param page número de página
     * @param size tamaño de la página
     * @param usuario usuario autenticado
     * @return página de invitaciones
     */
    @GetMapping
    @Operation(
            summary = "Listar invitaciones de la comunidad",
            description =
                    "Devuelve las invitaciones pendientes y procesadas de una comunidad. Solo"
                            + " administradores.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de invitaciones obtenida"),
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
        @ApiResponse(
                responseCode = "403",
                description = "Solo administradores pueden ver invitaciones"),
        @ApiResponse(responseCode = "404", description = "Comunidad no encontrada")
    })
    public ResponseEntity<InvitacionListResponse> listInvitaciones(
            @Parameter(description = "ID de la comunidad") @PathVariable Long communityId,
            @Parameter(description = "Número de página") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "20")
                    int size,
            @AuthenticationPrincipal Usuario usuario) {
        Pageable pageable = PageRequest.of(page, size);
        Page<InvitacionMiembro> invitaciones =
                invitacionService.getInvitacionesByCommunity(
                        usuario.getId(), communityId, pageable);
        InvitacionListResponse response =
                new InvitacionListResponse(
                        invitaciones.getContent().stream()
                                .map(invitacionService::toResponse)
                                .toList(),
                        (int) invitaciones.getTotalElements(),
                        page,
                        size);
        return ResponseEntity.ok(response);
    }

    /**
     * Crea una nueva invitación a un miembro.
     *
     * @param communityId ID de la comunidad
     * @param request datos de la invitación
     * @param usuario usuario autenticado (debe ser admin)
     * @return la invitación creada
     */
    @PostMapping
    @Operation(
            summary = "Crear invitación",
            description =
                    "Crea una nueva invitación a un miembro mediante email. Solo administradores"
                            + " pueden invitar.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Invitación creada y email enviado"),
        @ApiResponse(responseCode = "400", description = "Email inválido o ya existe invitación"),
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
        @ApiResponse(responseCode = "403", description = "Solo administradores pueden invitar"),
        @ApiResponse(responseCode = "404", description = "Comunidad no encontrada")
    })
    public ResponseEntity<InvitacionResponse> createInvitacion(
            @Parameter(description = "ID de la comunidad") @PathVariable Long communityId,
            @Valid @RequestBody CreateInvitacionRequest request,
            @AuthenticationPrincipal Usuario usuario) {
        InvitacionMiembro invitacion =
                invitacionService.createInvitacion(usuario.getId(), communityId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(invitacionService.toResponse(invitacion));
    }

    /**
     * Obtiene una invitación específica por código.
     *
     * @param codigo código único de la invitación
     * @return la invitación
     */
    @GetMapping("/codigo/{codigo}")
    @Operation(
            summary = "Obtener invitación por código",
            description = "Obtiene los detalles de una invitación usando su código único")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Invitación encontrada"),
        @ApiResponse(responseCode = "404", description = "Invitación no encontrada")
    })
    public ResponseEntity<InvitacionResponse> getInvitacionByCodigo(
            @Parameter(description = "Código único de la invitación") @PathVariable String codigo) {
        InvitacionMiembro invitacion = invitacionService.getInvitacionByCodigo(codigo);
        return ResponseEntity.ok(invitacionService.toResponse(invitacion));
    }

    /**
     * Acepta una invitación y agrega al usuario a la comunidad.
     *
     * @param codigo código de la invitación
     * @param usuario usuario autenticado
     * @return mensaje de confirmación
     */
    @PostMapping("/codigo/{codigo}/aceptar")
    @Operation(
            summary = "Aceptar invitación",
            description =
                    "El usuario acepta la invitación y se añade automáticamente a la comunidad con"
                            + " el rol asignado.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Invitación aceptada exitosamente"),
        @ApiResponse(responseCode = "400", description = "Invitación expirada o email no coincide"),
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
        @ApiResponse(responseCode = "404", description = "Invitación no encontrada")
    })
    public ResponseEntity<MessageResponse> aceptarInvitacion(
            @Parameter(description = "Código único de la invitación") @PathVariable String codigo,
            @AuthenticationPrincipal Usuario usuario) {
        invitacionService.aceptarInvitacion(codigo, usuario.getId());
        return ResponseEntity.ok(
                MessageResponse.builder()
                        .message(
                                "Invitación aceptada. Has sido añadido a la comunidad"
                                        + " exitosamente.")
                        .build());
    }

    /**
     * Rechaza una invitación.
     *
     * @param codigo código de la invitación
     * @return mensaje de confirmación
     */
    @PostMapping("/codigo/{codigo}/rechazar")
    @Operation(summary = "Rechazar invitación", description = "El usuario rechaza la invitación")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Invitación rechazada"),
        @ApiResponse(responseCode = "400", description = "Invitación ya fue procesada"),
        @ApiResponse(responseCode = "404", description = "Invitación no encontrada")
    })
    public ResponseEntity<MessageResponse> rechazarInvitacion(
            @Parameter(description = "Código único de la invitación") @PathVariable String codigo) {
        invitacionService.rechazarInvitacion(codigo);
        return ResponseEntity.ok(
                MessageResponse.builder().message("Invitación rechazada.").build());
    }
}
