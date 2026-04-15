package es.us.meerkat.backend.controller.communities;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import es.us.meerkat.backend.dto.communities.ApunteResponse;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.service.communities.ApunteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controlador REST para gestionar apuntes en comunidades.
 * Base URL: /api/v1/communities/{communityId}/apuntes
 */
@RestController
@RequestMapping("/api/v1/communities/{communityId}/apuntes")
@Tag(name = "Apuntes", description = "Gestión de apuntes y documentos en comunidades")
@RequiredArgsConstructor
@Slf4j
public class ApunteController {

    private final ApunteService apunteService;

    /**
     * Sube un nuevo apunte a la comunidad.
     * POST /api/v1/communities/{communityId}/apuntes/upload
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir apunte", description = "Sube un nuevo apunte a la comunidad", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApunteResponse> subirApunte(
            @PathVariable Long communityId,
            @AuthenticationPrincipal Usuario usuario,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String titulo,
            @RequestParam(required = false) String descripcion) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            ApunteResponse response = apunteService.subirApunte(communityId, usuario.getId(), titulo, descripcion,
                    file);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.warn("Error al subir apunte: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Obtiene todos los apuntes de una comunidad con paginación.
     * GET /api/v1/communities/{communityId}/apuntes
     */
    @GetMapping
    @Operation(summary = "Listar apuntes", description = "Obtiene los apuntes de una comunidad")
    public ResponseEntity<Page<ApunteResponse>> obtenerApuntes(
            @PathVariable Long communityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ApunteResponse> apuntes = apunteService.obtenerApuntesComunidad(communityId, pageable);
            return ResponseEntity.ok(apuntes);
        } catch (IllegalArgumentException e) {
            log.warn("Error al obtener apuntes: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Obtiene un apunte específico.
     * GET /api/v1/communities/{communityId}/apuntes/{apunteId}
     */
    @GetMapping("/{apunteId}")
    @Operation(summary = "Obtener apunte", description = "Obtiene los detalles de un apunte específico")
    public ResponseEntity<ApunteResponse> obtenerApunte(
            @PathVariable Long communityId,
            @PathVariable Long apunteId) {

        try {
            ApunteResponse apunte = apunteService.obtenerApunte(apunteId);
            // Verificar que pertenece a la comunidad
            if (!apunte.getComunidadId().equals(communityId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            return ResponseEntity.ok(apunte);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Descarga un apunte (devuelve el archivo).
     * GET /api/v1/communities/{communityId}/apuntes/{apunteId}/download
     */
    @GetMapping("/{apunteId}/download")
    @Operation(summary = "Descargar apunte", description = "Descarga el archivo del apunte")
    public ResponseEntity<byte[]> descargarApunte(
            @PathVariable Long communityId,
            @PathVariable Long apunteId) {

        try {
            ApunteResponse apunte = apunteService.obtenerApunte(apunteId);
            // Verificar que pertenece a la comunidad
            if (!apunte.getComunidadId().equals(communityId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            byte[] contenido = apunteService.descargarApunte(apunteId);
            String nombreCodificado = URLEncoder.encode(apunte.getNombreArchivo(), StandardCharsets.UTF_8);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + nombreCodificado)
                    .header(HttpHeaders.CONTENT_TYPE, apunte.getTipoMime())
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(contenido.length))
                    .body(contenido);
        } catch (IllegalArgumentException e) {
            log.warn("Error al descargar apunte: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Busca apuntes por título.
     * GET /api/v1/communities/{communityId}/apuntes/search
     */
    @GetMapping("/search")
    @Operation(summary = "Buscar apuntes", description = "Busca apuntes por título en la comunidad")
    public ResponseEntity<Page<ApunteResponse>> buscarApuntes(
            @PathVariable Long communityId,
            @RequestParam String titulo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ApunteResponse> resultados = apunteService.buscarApuntes(communityId, titulo, pageable);
            return ResponseEntity.ok(resultados);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Elimina un apunte.
     * DELETE /api/v1/communities/{communityId}/apuntes/{apunteId}
     */
    @DeleteMapping("/{apunteId}")
    @Operation(summary = "Eliminar apunte", description = "Elimina un apunte (solo el propietario)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> eliminarApunte(
            @PathVariable Long communityId,
            @PathVariable Long apunteId,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            ApunteResponse apunte = apunteService.obtenerApunte(apunteId);
            // Verificar que pertenece a la comunidad
            if (!apunte.getComunidadId().equals(communityId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            apunteService.eliminarApunte(apunteId, usuario.getId());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Error al eliminar apunte: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
}
