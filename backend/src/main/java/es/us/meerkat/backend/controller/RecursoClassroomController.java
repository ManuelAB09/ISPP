package es.us.meerkat.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.us.meerkat.backend.dto.CompartirRecursoClassroomRequest;
import es.us.meerkat.backend.dto.RecursoClassroomResponse;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.service.RecursoClassroomService;
import lombok.RequiredArgsConstructor;

/** Controlador REST para la gestión de recursos educativos de Google Classroom por comunidad. */
@RestController
@RequestMapping("/api/v1/comunidades/{comunidadId}/recursos")
@RequiredArgsConstructor
public class RecursoClassroomController {

    private final RecursoClassroomService recursoClassroomService;

    /**
     * GET /api/v1/comunidades/{comunidadId}/recursos Devuelve los recursos de la comunidad. Filtra
     * por visible=true para usuarios no admin. Por simplicidad el filtrado se delega al cliente o
     * se puede extender con roles.
     */
    @GetMapping
    public ResponseEntity<List<RecursoClassroomResponse>> getRecursos(
            @PathVariable Long comunidadId) {
        // Devuelve solo los visibles para el contexto general de la comunidad.
        // Los administradores pueden obtener todos los recursos a través del flag
        // soloVisibles=false
        // en la capa de servicio si se añade lógica de roles en el futuro.
        return ResponseEntity.ok(recursoClassroomService.getRecursos(comunidadId, true));
    }

    /**
     * POST /api/v1/comunidades/{comunidadId}/recursos Agrega un recurso a la comunidad y,
     * opcionalmente, lo publica en Classroom.
     */
    @PostMapping
    public ResponseEntity<RecursoClassroomResponse> agregarRecurso(
            @PathVariable Long comunidadId, @RequestBody CompartirRecursoClassroomRequest req) {
        Usuario usuario = getAuthenticatedUser();
        RecursoClassroomResponse response =
                recursoClassroomService.agregarRecurso(comunidadId, req, usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * DELETE /api/v1/comunidades/{comunidadId}/recursos/{recursoId} Elimina un recurso de la
     * comunidad.
     */
    @DeleteMapping("/{recursoId}")
    public ResponseEntity<Void> eliminarRecurso(
            @PathVariable Long comunidadId, @PathVariable Long recursoId) {
        Usuario usuario = getAuthenticatedUser();
        recursoClassroomService.eliminarRecurso(recursoId, usuario);
        return ResponseEntity.noContent().build();
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
