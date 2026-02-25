package es.us.meerkat.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.us.meerkat.backend.dto.TutorProfileRequest;
import es.us.meerkat.backend.dto.TutorProfileResponse;
import es.us.meerkat.backend.entity.Tutor;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.service.TutorService;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import lombok.RequiredArgsConstructor;

/** Controlador para manejar las operaciones relacionadas con los tutores. */
@RestController
@RequestMapping("/api/v1/tutors")
@RequiredArgsConstructor
public final class TutorController {

    /** Servicio para operaciones de tutor. */
    private final TutorService tutorService;

    /**
     * Lista todos los tutores verificados.
     *
     * @return Lista de tutores verificados.
     */
    @GetMapping()
    public List<Tutor> listarTutoresVerificados() {
        return tutorService.obtenerTutoresVerificados();
    }

    /**
     * Crea un perfil de tutor para un usuario dado.
     *
     * @param usuario Usuario autenticado.
     * @param request Datos para crear el perfil de tutor.
     * @return Perfil de tutor creado.
     */
    @PostMapping()
    public ResponseEntity<TutorProfileResponse> crearPerfil(
            @AuthenticationPrincipal final Usuario usuario,
            @RequestBody final TutorProfileRequest request) {

        return ResponseEntity.ok(tutorService.crearPerfil(usuario.getId(), request));
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
    public ResponseEntity<TutorProfileResponse> editarPerfil(
            @AuthenticationPrincipal final Usuario usuario,
            @PathVariable final Long tutorId,
            @RequestBody final TutorProfileRequest request) {

        return ResponseEntity.ok(tutorService.editarPerfil(usuario.getId(), tutorId, request));
    }

    /**
     * Devuelve el perfil público de un tutor.
     *
     * @param tutorId Identificador del tutor.
     * @return Perfil público del tutor.
     */
    @GetMapping("/{tutorId}")
    public ResponseEntity<TutorProfileResponse> verPerfilPublico(@PathVariable final Long tutorId) {

        return ResponseEntity.ok(tutorService.obtenerPerfilPublico(tutorId));
    }

    /**
     * Obtiene los perfiles de tutor del usuario autenticado.
     *
     * @param usuario Usuario autenticado.
     * @return Lista de perfiles de tutor.
     */
    @GetMapping("/me")
    public ResponseEntity<List<TutorProfileResponse>> obtenerMisPerfiles(
            @AuthenticationPrincipal final Usuario usuario) {

        return ResponseEntity.ok(tutorService.obtenerPerfilesPorUsuario(usuario.getId()));
    }

    /**
     * Obtiene un perfil de tutor específico del usuario autenticado.
     *
     * @param usuario Usuario autenticado.
     * @param tutorId ID del tutor.
     * @return Perfil de tutor.
     */
    @GetMapping("/me/{tutorId}")
    public ResponseEntity<TutorProfileResponse> obtenerMiPerfil(
            @AuthenticationPrincipal final Usuario usuario, @PathVariable final Long tutorId) {

        return ResponseEntity.ok(tutorService.obtenerPerfilDelUsuario(usuario.getId(), tutorId));
    }

    /**
     * Permite a un tutor solicitar la verificación de su perfil.
     *
     * @param tutorId Identificador del tutor.
     * @return Tutor que solicitó la verificación.
     */
    @PostMapping("/me/{tutorId}/verification")
    public ResponseEntity<Void> solicitarVerificacion(
            @AuthenticationPrincipal final Usuario usuario, @PathVariable final Long tutorId) {

        tutorService.solicitarVerificacion(usuario.getId(), tutorId);
        return ResponseEntity.ok().build();
    }

    /**
     * Consulta el estado de verificación de un tutor.
     *
     * @param tutorId Identificador del tutor.
     * @return Estado de verificación del tutor.
     */
    @GetMapping("/me/{tutorId}/verification-status")
    public ResponseEntity<String> obtenerEstadoVerificacion(
            @AuthenticationPrincipal final Usuario usuario, @PathVariable final Long tutorId) {

        return ResponseEntity.ok(tutorService.obtenerEstadoVerificacion(usuario.getId(), tutorId));
    }
}
