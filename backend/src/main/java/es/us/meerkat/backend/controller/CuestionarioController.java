package es.us.meerkat.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import es.us.meerkat.backend.dto.CreateCuestionarioRequest;
import es.us.meerkat.backend.dto.SubmitAttemptRequest;
import es.us.meerkat.backend.entity.Cuestionario;
import es.us.meerkat.backend.entity.CuestionarioIntento;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.service.CuestionarioService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/cuestionarios")
@Tag(name = "Cuestionarios", description = "Gestión de cuestionarios y preguntas personalizadas")
@RequiredArgsConstructor
public class CuestionarioController {

    private final CuestionarioService cuestionarioService;

    /**
     * Crea un cuestionario con preguntas personalizadas (test, verdadero/falso, respuesta corta).
     * Endpoint protegido: alumnos y profesores pueden usarlo.
     */
    @PostMapping
    public ResponseEntity<?> createCuestionario(
            @Valid @RequestBody CreateCuestionarioRequest request,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Cuestionario created = cuestionarioService.createFromDto(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** Obtiene un cuestionario por id. */
    @GetMapping("/{id}")
    public ResponseEntity<Cuestionario> getById(@PathVariable Long id) {
        return cuestionarioService
                .findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Envia un intento del cuestionario por el alumno autenticado y devuelve la puntuación. POST
     * /api/v1/cuestionarios/{id}/submit
     */
    @PostMapping("/{id}/submit")
    public ResponseEntity<?> submitAttempt(
            @PathVariable Long id,
            @RequestBody SubmitAttemptRequest request,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            CuestionarioIntento intento = cuestionarioService.submitAttempt(id, request, usuario);
            return ResponseEntity.ok(intento);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** Publica un cuestionario (cambia de BORRADOR a PUBLICADO). */
    @PutMapping("/{id}/publish")
    public ResponseEntity<?> publishCuestionario(
            @PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        if (usuario == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            Cuestionario updated = cuestionarioService.updatePublicado(id, true);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** Vuelve a poner el cuestionario en borrador. */
    @PutMapping("/{id}/draft")
    public ResponseEntity<?> draftCuestionario(
            @PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        if (usuario == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            Cuestionario updated = cuestionarioService.updatePublicado(id, false);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
