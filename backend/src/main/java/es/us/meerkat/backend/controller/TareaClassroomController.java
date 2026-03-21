package es.us.meerkat.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.us.meerkat.backend.dto.CalificarEntregaRequest;
import es.us.meerkat.backend.dto.CrearTareaClassroomRequest;
import es.us.meerkat.backend.dto.EntregaTareaResponse;
import es.us.meerkat.backend.dto.EntregarTareaRequest;
import es.us.meerkat.backend.dto.TareaClassroomResponse;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.service.TareaClassroomService;
import lombok.RequiredArgsConstructor;

/** Controlador REST para la gestión de tareas de Google Classroom por comunidad. */
@RestController
@RequestMapping("/api/v1/comunidades/{comunidadId}/tareas")
@RequiredArgsConstructor
public class TareaClassroomController {

    private final TareaClassroomService tareaClassroomService;

    /** GET /api/v1/comunidades/{comunidadId}/tareas Devuelve todas las tareas de la comunidad. */
    @GetMapping
    public ResponseEntity<List<TareaClassroomResponse>> getTareas(@PathVariable Long comunidadId) {
        return ResponseEntity.ok(tareaClassroomService.getTareas(comunidadId));
    }

    /**
     * POST /api/v1/comunidades/{comunidadId}/tareas Crea una nueva tarea y, opcionalmente, la
     * publica en Google Classroom.
     */
    @PostMapping
    public ResponseEntity<TareaClassroomResponse> crearTarea(
            @PathVariable Long comunidadId, @RequestBody CrearTareaClassroomRequest req) {
        Usuario usuario = getAuthenticatedUser();
        TareaClassroomResponse response =
                tareaClassroomService.crearTarea(comunidadId, req, usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/v1/comunidades/{comunidadId}/tareas/{tareaId}/entregas Devuelve todas las entregas
     * de una tarea (uso de admin/tutor).
     */
    @GetMapping("/{tareaId}/entregas")
    public ResponseEntity<List<EntregaTareaResponse>> getEntregas(
            @PathVariable Long comunidadId, @PathVariable Long tareaId) {
        return ResponseEntity.ok(tareaClassroomService.getEntregas(tareaId));
    }

    /**
     * POST /api/v1/comunidades/{comunidadId}/tareas/{tareaId}/entregas El alumno autenticado
     * entrega su tarea.
     */
    @PostMapping("/{tareaId}/entregas")
    public ResponseEntity<EntregaTareaResponse> entregarTarea(
            @PathVariable Long comunidadId,
            @PathVariable Long tareaId,
            @RequestBody EntregarTareaRequest req) {
        Usuario usuario = getAuthenticatedUser();
        EntregaTareaResponse response = tareaClassroomService.entregarTarea(tareaId, req, usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/v1/comunidades/{comunidadId}/tareas/{tareaId}/entregas/mia Devuelve la entrega del
     * alumno autenticado para una tarea.
     */
    @GetMapping("/{tareaId}/entregas/mia")
    public ResponseEntity<EntregaTareaResponse> getMiEntrega(
            @PathVariable Long comunidadId, @PathVariable Long tareaId) {
        Usuario usuario = getAuthenticatedUser();
        EntregaTareaResponse response = tareaClassroomService.getMiEntrega(tareaId, usuario);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/comunidades/{comunidadId}/tareas/mis-entregas Devuelve todas las entregas del
     * alumno autenticado en la comunidad.
     */
    @GetMapping("/mis-entregas")
    public ResponseEntity<List<EntregaTareaResponse>> getMisEntregas(
            @PathVariable Long comunidadId) {
        Usuario usuario = getAuthenticatedUser();
        return ResponseEntity.ok(
                tareaClassroomService.getMisEntregasEnComunidad(comunidadId, usuario));
    }

    /**
     * PUT /api/v1/comunidades/{comunidadId}/tareas/entregas/{entregaId}/calificar Califica una
     * entrega y sincroniza la nota con Classroom si hay vinculación.
     */
    @PutMapping("/entregas/{entregaId}/calificar")
    public ResponseEntity<EntregaTareaResponse> calificarEntrega(
            @PathVariable Long comunidadId,
            @PathVariable Long entregaId,
            @RequestBody CalificarEntregaRequest req) {
        Usuario usuario = getAuthenticatedUser();
        EntregaTareaResponse response =
                tareaClassroomService.calificarEntrega(entregaId, req, usuario);
        return ResponseEntity.ok(response);
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
