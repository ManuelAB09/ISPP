package es.us.meerkat.backend.controller;

import java.util.List;

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

import es.us.meerkat.backend.dto.CreateDisponibilidadRequest;
import es.us.meerkat.backend.dto.DisponibilidadTutorResponse;
import es.us.meerkat.backend.entity.Tutor;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.TutorRepository;
import es.us.meerkat.backend.service.DisponibilidadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/disponibilidad")
@RequiredArgsConstructor
public class DisponibilidadController {

    private final DisponibilidadService disponibilidadService;
    private final TutorRepository tutorRepository;

    /**
     * GET /api/v1/disponibilidad/tutor/{tutorId} — lista todas las disponibilidades activas del
     * tutor (público).
     */
    @GetMapping("/tutor/{tutorId}")
    public ResponseEntity<List<DisponibilidadTutorResponse>> getDisponibilidades(
            @PathVariable Long tutorId) {
        return ResponseEntity.ok(disponibilidadService.getDisponibilidades(tutorId));
    }

    /** POST /api/v1/disponibilidad — crea una nueva franja horaria para el tutor autenticado. */
    @PostMapping
    public ResponseEntity<DisponibilidadTutorResponse> crear(
            @AuthenticationPrincipal Usuario usuario,
            @Valid @RequestBody CreateDisponibilidadRequest request) {
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Tutor tutor =
                tutorRepository
                        .findByUsuarioId(usuario.getId())
                        .orElseThrow(
                                () -> new IllegalArgumentException("No tienes perfil de tutor"));
        DisponibilidadTutorResponse response =
                disponibilidadService.crearDisponibilidad(tutor.getId(), request, usuario.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** PUT /api/v1/disponibilidad/{id} — actualiza una franja horaria del tutor autenticado. */
    @PutMapping("/{id}")
    public ResponseEntity<DisponibilidadTutorResponse> actualizar(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable Long id,
            @Valid @RequestBody CreateDisponibilidadRequest request) {
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        DisponibilidadTutorResponse response =
                disponibilidadService.actualizarDisponibilidad(id, request, usuario.getId());
        return ResponseEntity.ok(response);
    }

    /** DELETE /api/v1/disponibilidad/{id} — desactiva una franja horaria del tutor autenticado. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(
            @AuthenticationPrincipal Usuario usuario, @PathVariable Long id) {
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        disponibilidadService.desactivarDisponibilidad(id, usuario.getId());
        return ResponseEntity.noContent().build();
    }
}
