package es.us.meerkat.backend.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import es.us.meerkat.backend.dto.*;
import es.us.meerkat.backend.entity.Categoria;
import es.us.meerkat.backend.entity.Comunidad;
import es.us.meerkat.backend.entity.EstadoSolicitud;
import es.us.meerkat.backend.entity.Evento;
import es.us.meerkat.backend.entity.MiembroComunidad;
import es.us.meerkat.backend.entity.SolicitudComunidad;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.service.AuthorizationService;
import es.us.meerkat.backend.service.CategoryService;
import es.us.meerkat.backend.service.CommunityService;
import es.us.meerkat.backend.service.EventoService;
import es.us.meerkat.backend.service.MemberService;
import es.us.meerkat.backend.service.RequestService;
import es.us.meerkat.backend.service.TutorContratacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controlador REST para la gestión de comunidades de estudio. Implementa todos los endpoints
 * relacionados con comunidades definidos en el OpenAPI. Base URL: /api/v1/communities
 */
@RestController
@RequestMapping("/api/v1/communities")
@Tag(name = "Comunidades", description = "Gestión de comunidades de estudio")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;
    private final MemberService memberService;
    private final RequestService requestService;
    private final CategoryService categoryService;
    private final AuthorizationService authorizationService;
    private final TutorContratacionService tutorContratacionService;
    private final EventoService eventoService;

    // =====================================================
    // COMUNIDADES - LISTADO Y CRUD BÁSICO
    // =====================================================

    /** Lista las comunidades públicas con filtros opcionales. GET /api/v1/communities */
    @GetMapping
    @Operation(
            summary = "Explorar comunidades",
            description = "Lista comunidades públicas con opciones de búsqueda" + " y filtrado")
    @ApiResponse(responseCode = "200", description = "Lista de comunidades obtenida correctamente")
    public ResponseEntity<CommunityListResponse> listCommunities(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Usuario usuario) {

        Long userId = usuario != null ? usuario.getId() : null;
        Pageable pageable = PageRequest.of(page, size);
        Page<Comunidad> comunidades = communityService.listPublicCommunities(search, pageable);
        Page<CommunityDetailResponse> response =
                comunidades.map(c -> entityToDetailResponse(c, userId));
        return ResponseEntity.ok(new CommunityListResponse(response));
    }

    /** Lista las comunidades de las que el usuario autenticado es miembro. */
    @GetMapping("/members/me")
    @Operation(
            summary = "Listar mis comunidades",
            description = "Lista las comunidades donde el usuario autenticado" + " tiene membresía",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de comunidades obtenida"),
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado")
    })
    public ResponseEntity<CommunityListResponse> listMyCommunities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<MiembroComunidad> memberships =
                memberService.listUserMemberships(usuario.getId(), pageable);
        Page<CommunityDetailResponse> response =
                memberships.map(
                        membership ->
                                entityToDetailResponse(membership.getComunidad(), usuario.getId()));

        return ResponseEntity.ok(new CommunityListResponse(response));
    }

    /** Crea una nueva comunidad. POST /api/v1/communities */
    @PostMapping
    @Operation(
            summary = "Crear comunidad",
            description =
                    "Crea una nueva comunidad. El creador se convierte"
                            + " automáticamente en administrador.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Comunidad creada correctamente"),
        @ApiResponse(
                responseCode = "400",
                description = "Datos inválidos o límite de comunidades gratuitas" + " alcanzado"),
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado")
    })
    public ResponseEntity<CommunityDetailResponse> createCommunity(
            @Valid @RequestBody CreateCommunityRequest request,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Comunidad comunidad =
                    communityService.createCommunity(
                            usuario.getId(),
                            request.nombre(),
                            request.descripcion(),
                            request.tipoGrupo() != null
                                    ? es.us.meerkat.backend.entity.TipoGrupo.valueOf(
                                            request.tipoGrupo())
                                    : es.us.meerkat.backend.entity.TipoGrupo.COMUNIDAD_PUBLICA,
                            request.imagenUrl());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(entityToDetailResponse(comunidad, usuario.getId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /** Obtiene los detalles de una comunidad. GET /api/v1/communities/{communityId} */
    @GetMapping("/{communityId}")
    @Operation(
            summary = "Obtener detalle de comunidad",
            description = "Devuelve los detalles completos de una comunidad")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Comunidad encontrada"),
        @ApiResponse(responseCode = "404", description = "Comunidad no encontrada")
    })
    public ResponseEntity<CommunityDetailResponse> getCommunityById(
            @Parameter(description = "ID de la comunidad") @PathVariable Long communityId,
            @AuthenticationPrincipal Usuario usuario) {

        Long userId = usuario != null ? usuario.getId() : null;
        Comunidad comunidad = communityService.getCommunityById(communityId, userId);
        return ResponseEntity.ok(entityToDetailResponse(comunidad, userId));
    }

    /** Actualiza una comunidad. PUT /api/v1/communities/{communityId} */
    @PutMapping("/{communityId}")
    @Operation(
            summary = "Actualizar comunidad",
            description = "Actualiza los datos de la comunidad (solo admin)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Comunidad actualizada correctamente"),
        @ApiResponse(
                responseCode = "403",
                description = "No tienes permisos para actualizar esta comunidad"),
        @ApiResponse(responseCode = "404", description = "Comunidad no encontrada")
    })
    public ResponseEntity<CommunityDetailResponse> updateCommunity(
            @PathVariable Long communityId,
            @Valid @RequestBody UpdateCommunityRequest request,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!authorizationService.isAdminOf(usuario.getId(), communityId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Comunidad comunidad =
                communityService.updateCommunity(
                        usuario.getId(),
                        communityId,
                        request.nombre(),
                        request.descripcion(),
                        request.imagenUrl());
        return ResponseEntity.ok(entityToDetailResponse(comunidad, usuario.getId()));
    }

    /** Elimina una comunidad. DELETE /api/v1/communities/{communityId} */
    @DeleteMapping("/{communityId}")
    @Operation(
            summary = "Eliminar comunidad",
            description = "Elimina la comunidad y todo su contenido (solo" + " admin)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Comunidad eliminada correctamente"),
        @ApiResponse(
                responseCode = "403",
                description = "No tienes permisos para eliminar esta comunidad"),
        @ApiResponse(responseCode = "404", description = "Comunidad no encontrada")
    })
    public ResponseEntity<Void> deleteCommunity(
            @PathVariable Long communityId, @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!authorizationService.isAdminOf(usuario.getId(), communityId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        communityService.deleteCommunity(usuario.getId(), communityId);
        return ResponseEntity.noContent().build();
    }

    /** Actualiza la privacidad de una comunidad. PUT /api/v1/communities/{communityId}/privacy */
    @PutMapping("/{communityId}/privacy")
    @Operation(
            summary = "Configurar privacidad",
            description = "Cambia si la comunidad es pública o privada (solo" + " admin)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Privacidad actualizada correctamente"),
        @ApiResponse(responseCode = "403", description = "No tienes permisos"),
        @ApiResponse(responseCode = "404", description = "Comunidad no encontrada")
    })
    public ResponseEntity<CommunityDetailResponse> updateCommunityPrivacy(
            @PathVariable Long communityId,
            @Valid @RequestBody PrivacyRequest request,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!authorizationService.isAdminOf(usuario.getId(), communityId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Comunidad comunidad =
                communityService.updatePrivacy(
                        usuario.getId(),
                        communityId,
                        es.us.meerkat.backend.entity.TipoGrupo.valueOf(request.tipoGrupo()));
        return ResponseEntity.ok(entityToDetailResponse(comunidad, usuario.getId()));
    }

    /** Mejora una comunidad a Premium. POST /api/v1/communities/{communityId}/upgrade */
    @PostMapping("/{communityId}/upgrade")
    @Operation(
            summary = "Mejorar comunidad a Premium",
            description = "Inicia el proceso de pago para convertir la" + " comunidad a Premium",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Comunidad mejorada a Premium"),
        @ApiResponse(responseCode = "400", description = "La comunidad ya es Premium"),
        @ApiResponse(responseCode = "403", description = "No tienes permisos")
    })
    public ResponseEntity<CommunityDetailResponse> upgradeCommunity(
            @PathVariable Long communityId,
            @Valid @RequestBody UpgradeCommunityRequest request,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!authorizationService.isAdminOf(usuario.getId(), communityId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            Comunidad comunidad = communityService.upgradeToPremium(usuario.getId(), communityId);
            return ResponseEntity.ok(entityToDetailResponse(comunidad, usuario.getId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /** Contrata un tutor para la comunidad. POST /api/v1/communities/{communityId}/tutor */
    @PostMapping("/{communityId}/tutor")
    @Operation(
            summary = "Contratar tutor",
            description =
                    "Inicia el proceso de pago para contratar un tutor"
                            + " (solo para comunidades privadas)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tutor contratado exitosamente"),
        @ApiResponse(
                responseCode = "400",
                description = "No se puede contratar en comunidad pública"),
        @ApiResponse(responseCode = "403", description = "No tienes permisos")
    })
    public ResponseEntity<MessageResponse> hireTutor(
            @PathVariable Long communityId,
            @Valid @RequestBody HireTutorRequest request,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(
                MessageResponse.builder()
                        .message("Funcionalidad de contratación de tutores en implementación")
                        .build());
    }

    // =====================================================
    // MIEMBROS
    // =====================================================

    /** Lista los miembros de una comunidad. GET /api/v1/communities/{communityId}/members */
    @GetMapping("/{communityId}/members")
    @Operation(
            summary = "Listar miembros",
            description = "Devuelve la lista de miembros de la comunidad")
    @ApiResponse(responseCode = "200", description = "Lista de miembros obtenida")
    public ResponseEntity<MemberListResponse> listCommunityMembers(
            @PathVariable Long communityId,
            @RequestParam(required = false) String rol,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<MiembroComunidad> miembros = memberService.listMembers(communityId, pageable);
        Page<MemberResponse> response = miembros.map(this::entityToMemberResponse);
        return ResponseEntity.ok(new MemberListResponse(response));
    }

    /** Se une a una comunidad pública. POST /api/v1/communities/{communityId}/members */
    @PostMapping("/{communityId}/members")
    @Operation(
            summary = "Unirse a comunidad pública",
            description = "Se une a una comunidad pública sin necesidad de" + " aprobación",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "Te has unido a la comunidad correctamente"),
        @ApiResponse(
                responseCode = "400",
                description = "No puedes unirte (privada, llena, ya eres miembro)"),
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado")
    })
    public ResponseEntity<MemberResponse> joinPublicCommunity(
            @PathVariable Long communityId, @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            MiembroComunidad miembro =
                    memberService.joinPublicCommunity(usuario.getId(), communityId);
            return ResponseEntity.status(HttpStatus.CREATED).body(entityToMemberResponse(miembro));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Obtiene la membresía del usuario autenticado en una comunidad. GET
     * /api/v1/communities/{communityId}/members/me
     */
    @GetMapping("/{communityId}/members/me")
    @Operation(
            summary = "Obtener mi rol en la comunidad",
            description =
                    "Devuelve el rol y membresía del usuario autenticado" + " en esta comunidad",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Membresía obtenida"),
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
        @ApiResponse(responseCode = "404", description = "No eres miembro de esta comunidad")
    })
    public ResponseEntity<MemberResponse> getMyMembership(
            @PathVariable Long communityId, @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            MiembroComunidad miembro = memberService.getMyMembership(usuario.getId(), communityId);
            return ResponseEntity.ok(entityToMemberResponse(miembro));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /** Abandona una comunidad. DELETE /api/v1/communities/{communityId}/members/me */
    @DeleteMapping("/{communityId}/members/me")
    @Operation(
            summary = "Abandonar comunidad",
            description = "Abandona la comunidad (si eres admin único, debe" + " designar sucesor)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Has abandonado la comunidad"),
        @ApiResponse(responseCode = "400", description = "No puedes abandonar siendo único admin"),
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
        @ApiResponse(responseCode = "404", description = "No eres miembro de esta comunidad")
    })
    public ResponseEntity<Void> leaveCommunity(
            @PathVariable Long communityId, @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            memberService.leaveCommunity(usuario.getId(), communityId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Expulsa a un miembro de la comunidad. DELETE
     * /api/v1/communities/{communityId}/members/{userId}
     */
    @DeleteMapping("/{communityId}/members/{userId}")
    @Operation(
            summary = "Expulsar miembro",
            description = "Expulsa a un miembro de la comunidad (solo admin)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Miembro expulsado correctamente"),
        @ApiResponse(responseCode = "403", description = "No tienes permisos"),
        @ApiResponse(responseCode = "404", description = "Miembro no encontrado")
    })
    public ResponseEntity<Void> expelMember(
            @PathVariable Long communityId,
            @PathVariable Long userId,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!authorizationService.isAdminOf(usuario.getId(), communityId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            memberService.expelMember(usuario.getId(), communityId, userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Transfiere la administración de la comunidad. POST
     * /api/v1/communities/{communityId}/admin/transfer
     */
    @PostMapping("/{communityId}/admin/transfer")
    @Operation(
            summary = "Transferir administración",
            description = "Transfiere el rol de admin a otro miembro (solo" + " admin actual)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Administración transferida"),
        @ApiResponse(responseCode = "403", description = "No tienes permisos"),
        @ApiResponse(responseCode = "404", description = "Usuario target no encontrado")
    })
    public ResponseEntity<MemberResponse> transferAdmin(
            @PathVariable Long communityId,
            @Valid @RequestBody TransferAdminRequest request,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!authorizationService.isAdminOf(usuario.getId(), communityId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            MiembroComunidad newAdmin =
                    memberService.transferAdmin(
                            usuario.getId(), communityId, request.nuevoAdminId());
            return ResponseEntity.ok(entityToMemberResponse(newAdmin));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // =====================================================
    // SOLICITUDES
    // =====================================================

    /**
     * Lista las solicitudes de acceso a una comunidad. GET
     * /api/v1/communities/{communityId}/requests
     */
    @GetMapping("/{communityId}/requests")
    @Operation(
            summary = "Listar solicitudes de acceso",
            description =
                    "Lista las solicitudes pendientes de acceso a la" + " comunidad (solo admin)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de solicitudes obtenida"),
        @ApiResponse(responseCode = "403", description = "No tienes permisos")
    })
    public ResponseEntity<RequestListResponse> listRequests(
            @PathVariable Long communityId,
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!authorizationService.isAdminOf(usuario.getId(), communityId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        EstadoSolicitud estadoFilter = estado != null ? EstadoSolicitud.valueOf(estado) : null;
        Pageable pageable = PageRequest.of(page, size);
        Page<SolicitudComunidad> solicitudes =
                requestService.listRequests(usuario.getId(), communityId, estadoFilter, pageable);

        Page<RequestResponse> response = solicitudes.map(this::entityToRequestResponse);
        return ResponseEntity.ok(new RequestListResponse(response));
    }

    /** Solicita acceso a una comunidad privada. POST /api/v1/communities/{communityId}/requests */
    @PostMapping("/{communityId}/requests")
    @Operation(
            summary = "Solicitar acceso",
            description = "Solicita acceso a una comunidad privada",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Solicitud creada correctamente"),
        @ApiResponse(
                responseCode = "400",
                description =
                        "No puedes solicitar (es pública, ya eres miembro,"
                                + " solicitud pendiente)"),
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado")
    })
    public ResponseEntity<RequestResponse> requestAccess(
            @PathVariable Long communityId,
            @Valid @RequestBody AccessRequestBody request,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            SolicitudComunidad solicitud =
                    requestService.requestAccess(usuario.getId(), communityId, request.mensaje());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(entityToRequestResponse(solicitud));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Responde a una solicitud de acceso. PUT
     * /api/v1/communities/{communityId}/requests/{requestId}
     */
    @PutMapping("/{communityId}/requests/{requestId}")
    @Operation(
            summary = "Responder solicitud",
            description = "Acepta o rechaza una solicitud de acceso (solo" + " admin)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Solicitud respondida"),
        @ApiResponse(responseCode = "403", description = "No tienes permisos"),
        @ApiResponse(responseCode = "404", description = "Solicitud no encontrada")
    })
    public ResponseEntity<RequestResponse> respondToRequest(
            @PathVariable Long communityId,
            @PathVariable Long requestId,
            @Valid @RequestBody RespondRequestBody request,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!authorizationService.isAdminOf(usuario.getId(), communityId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            SolicitudComunidad solicitud =
                    requestService.respondToRequest(
                            usuario.getId(), communityId, requestId, request.aceptado());
            return ResponseEntity.ok(entityToRequestResponse(solicitud));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // =====================================================
    // CATEGORÍAS
    // =====================================================

    /** Lista las categorías de una comunidad. GET /api/v1/communities/{communityId}/categories */
    @GetMapping("/{communityId}/categories")
    @Operation(
            summary = "Listar categorías",
            description = "Devuelve las categorías temáticas de la comunidad")
    @ApiResponse(responseCode = "200", description = "Lista de categorías obtenida")
    public ResponseEntity<CategoryListResponse> listCategories(@PathVariable Long communityId) {

        List<Categoria> categorias = categoryService.listCategories(communityId);
        List<CategoryResponse> response =
                categorias.stream()
                        .map(this::entityToCategoryResponse)
                        .collect(Collectors.toList());

        return ResponseEntity.ok(new CategoryListResponse(response));
    }

    /** Crea una nueva categoría. POST /api/v1/communities/{communityId}/categories */
    @PostMapping("/{communityId}/categories")
    @Operation(
            summary = "Crear categoría",
            description = "Crea una nueva categoría temática (solo admin)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Categoría creada correctamente"),
        @ApiResponse(responseCode = "403", description = "No tienes permisos")
    })
    public ResponseEntity<CategoryResponse> createCategory(
            @PathVariable Long communityId,
            @Valid @RequestBody CreateCategoryRequest request,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!authorizationService.isAdminOf(usuario.getId(), communityId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Categoria categoria =
                categoryService.createCategory(
                        usuario.getId(), communityId, request.nombre(), request.descripcion());
        return ResponseEntity.status(HttpStatus.CREATED).body(entityToCategoryResponse(categoria));
    }

    /** Actualiza una categoría. PUT /api/v1/communities/{communityId}/categories/{categoryId} */
    @PutMapping("/{communityId}/categories/{categoryId}")
    @Operation(
            summary = "Actualizar categoría",
            description = "Actualiza los datos de una categoría (solo admin)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Categoría actualizada"),
        @ApiResponse(responseCode = "403", description = "No tienes permisos"),
        @ApiResponse(responseCode = "404", description = "Categoría no encontrada")
    })
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable Long communityId,
            @PathVariable Long categoryId,
            @Valid @RequestBody UpdateCategoryRequest request,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!authorizationService.isAdminOf(usuario.getId(), communityId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            Categoria categoria =
                    categoryService.updateCategory(
                            usuario.getId(),
                            communityId,
                            categoryId,
                            request.nombre(),
                            request.descripcion());
            return ResponseEntity.ok(entityToCategoryResponse(categoria));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /** Elimina una categoría. DELETE /api/v1/communities/{communityId}/categories/{categoryId} */
    @DeleteMapping("/{communityId}/categories/{categoryId}")
    @Operation(
            summary = "Eliminar categoría",
            description = "Elimina una categoría (solo admin)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Categoría eliminada"),
        @ApiResponse(responseCode = "403", description = "No tienes permisos"),
        @ApiResponse(responseCode = "404", description = "Categoría no encontrada")
    })
    public ResponseEntity<Void> deleteCategory(
            @PathVariable Long communityId,
            @PathVariable Long categoryId,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!authorizationService.isAdminOf(usuario.getId(), communityId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            categoryService.deleteCategory(usuario.getId(), communityId, categoryId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Reordena las categorías de una comunidad. PUT
     * /api/v1/communities/{communityId}/categories/reorder
     */
    @PutMapping("/{communityId}/categories/reorder")
    @Operation(
            summary = "Reordenar categorías",
            description = "Cambia el orden de las categorías (solo admin)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Categorías reordenadas"),
        @ApiResponse(responseCode = "403", description = "No tienes permisos")
    })
    public ResponseEntity<CategoryListResponse> reorderCategories(
            @PathVariable Long communityId,
            @Valid @RequestBody ReorderCategoriesRequest request,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!authorizationService.isAdminOf(usuario.getId(), communityId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        categoryService.reorderCategories(usuario.getId(), communityId, request.categoryIds());
        List<Categoria> categorias = categoryService.listCategories(communityId);
        List<CategoryResponse> response =
                categorias.stream()
                        .map(this::entityToCategoryResponse)
                        .collect(Collectors.toList());

        return ResponseEntity.ok(new CategoryListResponse(response));
    }

    // =====================================================
    // EVENTOS DE COMUNIDAD
    // =====================================================

    /** Crea un nuevo evento en una comunidad. POST /api/v1/communities/{communityId}/events */
    @PostMapping("/{communityId}/events")
    @Operation(
            summary = "Crear evento en comunidad",
            description = "Crea un nuevo evento asociado a una comunidad",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Evento creado correctamente"),
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
        @ApiResponse(responseCode = "404", description = "Comunidad no encontrada")
    })
    public ResponseEntity<EventDetailResponse> createEvent(
            @PathVariable Long communityId,
            @Valid @RequestBody CreateEventRequest request,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            communityService.getCommunityById(communityId, null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        try {
            Evento evento =
                    eventoService.crearEvento(
                            usuario.getId(),
                            communityId,
                            request.getTitulo(),
                            request.getDescripcion(),
                            request.getFechaHora(),
                            request.getFechaFin(),
                            request.getAforo(),
                            request.getQueLlevar(),
                            request.getEsVirtual(),
                            false, // privado por defecto
                            request.getEnlaceVirtual(),
                            request.getVisibleEnMapa(),
                            request.getUbicacionId());

            return ResponseEntity.status(HttpStatus.CREATED).body(evento.toDTO());
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("no perteneces")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            throw e;
        }
    }

    /** Lista los eventos de una comunidad. GET /api/v1/communities/{communityId}/events */
    @GetMapping("/{communityId}/events")
    @Operation(
            summary = "Listar eventos de comunidad",
            description = "Devuelve los eventos asociados a una comunidad")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de eventos obtenida"),
        @ApiResponse(responseCode = "404", description = "Comunidad no encontrada")
    })
    public ResponseEntity<List<EventSummaryResponse>> listCommunityEvents(
            @PathVariable Long communityId,
            @RequestParam(name = "cancelados", defaultValue = "false") boolean cancelados) {

        try {
            communityService.getCommunityById(communityId, null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        List<Evento> eventos = eventoService.obtenerEventosPorComunidad(communityId, cancelados);
        List<EventSummaryResponse> response =
                eventos.stream().map(Evento::toSummaryDTO).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    // =====================================================
    // HELPER METHODS FOR DTO CONVERSION
    // =====================================================

    private CommunityDetailResponse entityToDetailResponse(Comunidad comunidad, Long userId) {
        Long miembrosActuales = communityService.countMembers(comunidad.getId());
        String miRol = null;
        Boolean esMiembro = false;

        if (userId != null) {
            miRol = authorizationService.getUserRoleInCommunityAsString(userId, comunidad.getId());
            esMiembro = miRol != null;
        }

        return new CommunityDetailResponse(
                comunidad.getId(),
                comunidad.getNombre(),
                comunidad.getDescripcion(),
                comunidad.getTipoGrupo().name(),
                comunidad.getTipoPlan().name(),
                comunidad.getMaxMiembros(),
                miembrosActuales,
                convertUserToSimple(comunidad.getCreador()),
                comunidad.getEstado().name(),
                esMiembro,
                miRol,
                comunidad.getCreatedAt(),
                comunidad.getUpdatedAt(),
                comunidad.getImagenUrl());
    }

    private MemberResponse entityToMemberResponse(MiembroComunidad miembro) {
        return new MemberResponse(
                miembro.getId(),
                convertUserToSimple(miembro.getUsuario()),
                miembro.getRol().name(),
                miembro.getFechaIngreso());
    }

    private RequestResponse entityToRequestResponse(SolicitudComunidad solicitud) {
        return new RequestResponse(
                solicitud.getId(),
                convertUserToSimple(solicitud.getSolicitante()),
                solicitud.getEstado().name(),
                solicitud.getMensaje(),
                solicitud.getFechaSolicitud(),
                solicitud.getRespondidaPor() != null
                        ? convertUserToSimple(solicitud.getRespondidaPor())
                        : null,
                solicitud.getFechaRespuesta());
    }

    private CategoryResponse entityToCategoryResponse(Categoria categoria) {
        return new CategoryResponse(
                categoria.getId(),
                categoria.getNombre(),
                categoria.getDescripcion(),
                categoria.getOrden());
    }

    // =====================================================
    // CONTRATACIÓN DE TUTORES
    // =====================================================

    /**
     * Contrata un tutor para la comunidad. Inicializa el pago y retorna URL de pago.
     *
     * <p>POST /api/v1/communities/{communityId}/tutor/{tutorId}
     *
     * @param communityId ID de la comunidad
     * @param tutorId ID del tutor a contratar
     * @param request Datos de la contratación
     * @param usuario Usuario autenticado (debe ser admin de comunidad)
     * @return PaymentUrlResponse con URL para procesar pago
     */
    @PostMapping("/{communityId}/tutor/{tutorId}")
    @Operation(
            summary = "Contratar tutor",
            description = "Contrata un tutor para la comunidad. Retorna URL de pago.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Tutor contratación iniciada, URL de pago generada"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos o validación fallida"),
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
        @ApiResponse(
                responseCode = "403",
                description = "No tienes permisos para contratar en esta comunidad"),
        @ApiResponse(responseCode = "404", description = "Comunidad o tutor no encontrado")
    })
    public ResponseEntity<PaymentUrlResponse> hireTutor(
            @PathVariable Long communityId,
            @PathVariable Long tutorId,
            @Valid @RequestBody HireTutorRequest request,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            PaymentUrlResponse paymentUrl =
                    tutorContratacionService.crearContratacion(
                            communityId, tutorId, request, usuario.getId());
            return ResponseEntity.ok(paymentUrl);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Cancela la contratación de un tutor por la comunidad.
     *
     * <p>DELETE /api/v1/communities/{communityId}/tutor
     *
     * @param communityId ID de la comunidad
     * @param usuario Usuario autenticado (debe ser admin de comunidad)
     * @return MessageResponse con confirmación
     */
    @DeleteMapping("/{communityId}/tutor")
    @Operation(
            summary = "Cancelar tutor",
            description = "Cancela la contratación del tutor activo de la comunidad.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Contratación cancelada exitosamente"),
        @ApiResponse(responseCode = "400", description = "No hay tutor contratado"),
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
        @ApiResponse(responseCode = "403", description = "No tienes permisos para cancelar"),
        @ApiResponse(responseCode = "404", description = "Comunidad no encontrada")
    })
    public ResponseEntity<?> cancelarTutor(
            @PathVariable Long communityId,
            @RequestParam String motivo,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            var contratacion =
                    tutorContratacionService.obtenerContratacionActivaDeComunidad(communityId);

            if (contratacion.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(
                                MessageResponse.builder()
                                        .message("No hay tutor contratado en esta comunidad")
                                        .build());
            }

            tutorContratacionService.cancelarContratacion(
                    contratacion.get().getId(), usuario.getId(), motivo);

            return ResponseEntity.ok()
                    .body(
                            MessageResponse.builder()
                                    .message("Contratación cancelada exitosamente")
                                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(MessageResponse.builder().message(e.getMessage()).build());
        }
    }

    /**
     * Obtiene el tutor actualmente contratado por la comunidad.
     *
     * <p>GET /api/v1/communities/{communityId}/tutor
     *
     * @param communityId ID de la comunidad
     * @return TutorResponse del tutor activo o 404 si no hay
     */
    @GetMapping("/{communityId}/tutor")
    @Operation(
            summary = "Obtener tutor activo",
            description = "Obtiene el tutor actualmente contratado por la comunidad")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tutor encontrado"),
        @ApiResponse(
                responseCode = "404",
                description = "Comunidad no encontrada o sin tutor activo")
    })
    public ResponseEntity<?> obtenerTutorActivo(@PathVariable Long communityId) {
        try {
            var tutor = tutorContratacionService.obtenerTutorActivoDeComunidad(communityId);

            if (tutor.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(tutor.get());
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    private UserSimpleResponse convertUserToSimple(Usuario usuario) {
        return new UserSimpleResponse(
                usuario.getId(), usuario.getNombre(), usuario.getEmail(), usuario.getFoto());
    }
}
