package es.us.meerkat.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.us.meerkat.backend.dto.AgregarGrabacionRequest;
import es.us.meerkat.backend.dto.GrabacionClaseResponse;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.service.GrabacionClaseService;
import lombok.RequiredArgsConstructor;

/** Controlador REST para la gestión de grabaciones de clase por comunidad. */
@RestController
@RequestMapping("/api/v1/comunidades/{comunidadId}/grabaciones")
@RequiredArgsConstructor
public class GrabacionClaseController {

    private final GrabacionClaseService grabacionClaseService;

    /**
     * GET /api/v1/comunidades/{comunidadId}/grabaciones Devuelve todas las grabaciones de la
     * comunidad ordenadas por fecha descendente.
     */
    @GetMapping
    public ResponseEntity<List<GrabacionClaseResponse>> getGrabaciones(
            @PathVariable Long comunidadId) {
        return ResponseEntity.ok(grabacionClaseService.getGrabaciones(comunidadId));
    }

    /**
     * POST /api/v1/comunidades/{comunidadId}/grabaciones Agrega una grabación a la comunidad y,
     * opcionalmente, la comparte en Classroom.
     */
    @PostMapping
    public ResponseEntity<GrabacionClaseResponse> agregarGrabacion(
            @PathVariable Long comunidadId, @RequestBody AgregarGrabacionRequest req) {
        Usuario usuario = getAuthenticatedUser();
        GrabacionClaseResponse response =
                grabacionClaseService.agregarGrabacion(comunidadId, req, usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private Usuario getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("No autenticado");
        }
        Object principal = auth.getPrincipal();
        if (!(principal instanceof Usuario)) {
            throw new RuntimeException("Usuario inválido");
        }
        return (Usuario) principal;
    }
}
