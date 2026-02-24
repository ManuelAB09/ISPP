package es.us.meerkat.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.us.meerkat.backend.dto.TutorProfileRequest;
import es.us.meerkat.backend.dto.TutorProfileResponse;
import es.us.meerkat.backend.entity.Tutor;
import es.us.meerkat.backend.service.TutorService;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import lombok.RequiredArgsConstructor;

/** Controlador para manejar las operaciones relacionadas con los tutores. */
@RestController
@RequestMapping("/api/tutors")
@RequiredArgsConstructor
public final class TutorController {

    /** Servicio para operaciones de tutor. */
    private final TutorService tutorService;

    /**
     * Crea un perfil de tutor para un usuario dado.
     *
     * @param usuarioId Identificador del usuario.
     * @param request Datos para crear el perfil de tutor.
     * @return Perfil de tutor creado.
     */
    @PostMapping("/{usuarioId}/perfil")
    public ResponseEntity<TutorProfileResponse> crearPerfil(
            @PathVariable final Long usuarioId, @RequestBody final TutorProfileRequest request) {

        return ResponseEntity.ok(tutorService.crearPerfil(usuarioId, request));
    }

    /**
     * Edita el perfil de un tutor existente.
     *
     * @param usuarioId Identificador del usuario.
     * @param request Datos actualizados del perfil.
     * @return Perfil de tutor actualizado.
     */
    @PutMapping("/{usuarioId}/perfil")
    public ResponseEntity<TutorProfileResponse> editarPerfil(
            @PathVariable final Long usuarioId, @RequestBody final TutorProfileRequest request) {

        return ResponseEntity.ok(tutorService.editarPerfil(usuarioId, request));
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
     * Lista todos los tutores verificados.
     *
     * @return Lista de tutores verificados.
     */
    @GetMapping("/verificados")
    public List<Tutor> listarTutoresVerificados() {
        return tutorService.obtenerTutoresVerificados();
    }

    /**
     * Permite a un tutor solicitar la verificación de su perfil.
     *
     * @param tutorId Identificador del tutor.
     * @return Tutor que solicitó la verificación.
     */
    @PostMapping("/{tutorId}/solicitar-verificacion")
    public Tutor solicitarVerificacion(@PathVariable final Long tutorId) {
        return tutorService.solicitarVerificacion(tutorId);
    }

    /**
     * Consulta el estado de verificación de un tutor.
     *
     * @param tutorId Identificador del tutor.
     * @return Estado de verificación del tutor.
     */
    @GetMapping("/{tutorId}/estado-verificacion")
    public String estadoVerificacion(@PathVariable final Long tutorId) {
        return tutorService.consultarEstadoVerificacion(tutorId);
    }
}
