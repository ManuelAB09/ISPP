package es.us.meerkat.backend.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import es.us.meerkat.backend.dto.TutorProfileRequest;
import es.us.meerkat.backend.dto.TutorProfileResponse;
import es.us.meerkat.backend.entity.Tutor;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.service.TutorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestBody;

/** Controlador para manejar las operaciones relacionadas con los tutores. */
@RestController
@RequestMapping("/api/v1/tutors")
@RequiredArgsConstructor
public final class TutorController {

    /** Servicio para operaciones de tutor. */
    private final TutorService tutorService;

    @GetMapping()
    public ResponseEntity<?> listarTutoresVerificados(
            @RequestParam(required = false) String especialidad,
            @RequestParam(required = false) BigDecimal tarifaMin,
            @RequestParam(required = false) BigDecimal tarifaMax,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<Tutor> tutores =
                    tutorService.obtenerTutoresVerificados(
                            especialidad, tarifaMin, tarifaMax, page, size);
            return ResponseEntity.ok(tutores);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al listar tutores verificados: " + e.getMessage());
        }
    }

    /**
     * Crea un perfil de tutor para un usuario dado.
     *
     * @param usuario Usuario autenticado.
     * @param request Datos para crear el perfil de tutor.
     * @return Perfil de tutor creado.
     */
    @PostMapping()
    public ResponseEntity<?> crearPerfil(
            @AuthenticationPrincipal final Usuario usuario,
            @RequestBody final TutorProfileRequest request) {
        try {
            TutorProfileResponse perfil = tutorService.crearPerfil(usuario.getId(), request);
            return ResponseEntity.ok(perfil);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al crear perfil de tutor: " + e.getMessage());
        }
    }

    /**
     * Edita el perfil de un tutor existente.
     *
     * @param usuario Usuario autenticado.
     * @param tutorId tutor al que editar.
     * @param request Datos actualizados del perfil.
     * @return Perfil de tutor actualizado.
     */
    @PutMapping("/{tutorId}")
    public ResponseEntity<?> editarPerfil(
            @AuthenticationPrincipal final Usuario usuario,
            @PathVariable final Long tutorId,
            @RequestBody final TutorProfileRequest request) {
        try {
            TutorProfileResponse perfilActualizado =
                    tutorService.editarPerfil(usuario.getId(), tutorId, request);
            return ResponseEntity.ok(perfilActualizado);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al editar perfil de tutor: " + e.getMessage());
        }
    }

    /**
     * Devuelve el perfil público de un tutor.
     *
     * @param tutorId Identificador del tutor.
     * @return Perfil público del tutor.
     */
    @GetMapping("/{tutorId}")
    public ResponseEntity<?> verPerfilPublico(@PathVariable final Long tutorId) {
        try {
            TutorProfileResponse perfil = tutorService.obtenerPerfilPublico(tutorId);
            return ResponseEntity.ok(perfil);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al obtener perfil público: " + e.getMessage());
        }
    }

    /**
     * Obtiene los perfiles de tutor del usuario autenticado.
     *
     * @param usuario Usuario autenticado.
     * @return Lista de perfiles de tutor.
     */
    @GetMapping("/me")
    public ResponseEntity<?> obtenerMisPerfiles(@AuthenticationPrincipal final Usuario usuario) {
        try {
            List<TutorProfileResponse> perfiles =
                    tutorService.obtenerPerfilesPorUsuario(usuario.getId());
            return ResponseEntity.ok(perfiles);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al obtener mis perfiles: " + e.getMessage());
        }
    }

    /**
     * Obtiene un perfil de tutor específico del usuario autenticado.
     *
     * @param usuario Usuario autenticado.
     * @param tutorId ID del tutor.
     * @return Perfil de tutor.
     */
    @GetMapping("/me/{tutorId}")
    public ResponseEntity<?> obtenerMiPerfil(
            @AuthenticationPrincipal final Usuario usuario, @PathVariable final Long tutorId) {
        try {
            TutorProfileResponse perfil =
                    tutorService.obtenerPerfilDelUsuario(usuario.getId(), tutorId);
            return ResponseEntity.ok(perfil);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al obtener mi perfil: " + e.getMessage());
        }
    }

    /**
     * Permite a un tutor solicitar la verificación de su perfil.
     *
     * @param tutorId Identificador del tutor.
     * @return Tutor que solicitó la verificación.
     */
    @PostMapping("/me/{tutorId}/verification")
    public ResponseEntity<?> solicitarVerificacion(
            @AuthenticationPrincipal final Usuario usuario, @PathVariable final Long tutorId) {
        try {
            tutorService.solicitarVerificacion(usuario.getId(), tutorId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al solicitar verificación: " + e.getMessage());
        }
    }

    /**
     * Consulta el estado de verificación de un tutor.
     *
     * @param tutorId Identificador del tutor.
     * @return Estado de verificación del tutor.
     */
    @GetMapping("/me/{tutorId}/verification-status")
    public ResponseEntity<?> obtenerEstadoVerificacion(
            @AuthenticationPrincipal final Usuario usuario, @PathVariable final Long tutorId) {
        try {
            String estado = tutorService.obtenerEstadoVerificacion(usuario.getId(), tutorId);
            return ResponseEntity.ok(estado);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al obtener estado de verificación: " + e.getMessage());
        }
    }
}
