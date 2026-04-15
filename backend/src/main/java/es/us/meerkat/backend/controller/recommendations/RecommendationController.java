package es.us.meerkat.backend.controller.recommendations;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import es.us.meerkat.backend.dto.recommendations.FeedbackRecomendacionRequest;
import es.us.meerkat.backend.dto.recommendations.RecomendacionResponse;
import es.us.meerkat.backend.dto.recommendations.RecomendacionesPageResponse;
import es.us.meerkat.backend.dto.recommendations.RegistrarActividadRequest;
import es.us.meerkat.backend.dto.recommendations.ValoracionTutorRequest;
import es.us.meerkat.backend.service.recommendations.RecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService service;

    // -----------------------------------------------------------------------
    // CONSULTA
    // -----------------------------------------------------------------------

    /** GET /api/recommendations/page — todas las secciones de golpe */
    @GetMapping("/page")
    public ResponseEntity<RecomendacionesPageResponse> page(
            @AuthenticationPrincipal UserDetails u) {
        return ResponseEntity.ok(service.getRecomendacionesPage(uid(u)));
    }

    /** GET /api/recommendations/profesores?page=0&size=6 */
    @GetMapping("/profesores")
    public ResponseEntity<Page<RecomendacionResponse>> profesores(
            @AuthenticationPrincipal UserDetails u,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size) {
        return ResponseEntity.ok(
                service.getRecomendacionesProfesores(uid(u), PageRequest.of(page, size)));
    }

    /** GET /api/recommendations/contenido?page=0&size=8 */
    @GetMapping("/contenido")
    public ResponseEntity<Page<RecomendacionResponse>> contenido(
            @AuthenticationPrincipal UserDetails u,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size) {
        return ResponseEntity.ok(
                service.getRecomendacionesContenido(uid(u), PageRequest.of(page, size)));
    }

    /** GET /api/recommendations/cuestionarios?page=0&size=6 */
    @GetMapping("/cuestionarios")
    public ResponseEntity<Page<RecomendacionResponse>> cuestionarios(
            @AuthenticationPrincipal UserDetails u,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size) {
        return ResponseEntity.ok(
                service.getRecomendacionesCuestionarios(uid(u), PageRequest.of(page, size)));
    }

    /** GET /api/recommendations/comunidades?page=0&size=4 */
    @GetMapping("/comunidades")
    public ResponseEntity<Page<RecomendacionResponse>> comunidades(
            @AuthenticationPrincipal UserDetails u,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "4") int size) {
        return ResponseEntity.ok(
                service.getRecomendacionesComunidades(uid(u), PageRequest.of(page, size)));
    }

    /** GET /api/recommendations?page=0&size=20 — todas las activas */
    @GetMapping
    public ResponseEntity<Page<RecomendacionResponse>> activas(
            @AuthenticationPrincipal UserDetails u,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                service.getRecomendacionesActivas(uid(u), PageRequest.of(page, size)));
    }

    /**
     * GET /api/recommendations/no-vistas?page=0&size=10 Útil para mostrar badge "X recomendaciones
     * nuevas" en el frontend.
     */
    @GetMapping("/no-vistas")
    public ResponseEntity<Page<RecomendacionResponse>> noVistas(
            @AuthenticationPrincipal UserDetails u,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                service.getRecomendacionesNoVistas(uid(u), PageRequest.of(page, size)));
    }

    // -----------------------------------------------------------------------
    // ACCIONES
    // -----------------------------------------------------------------------

    /**
     * POST /api/recommendations/tutores/{tutorId}/valorar Body: { "puntuacion": 5, "comentario":
     * "Muy buen profesor" } Valoracion 1-5 estrellas que alimenta el algoritmo de recomendaciones.
     */
    @PostMapping("/tutores/{tutorId}/valorar")
    public ResponseEntity<Void> valorarTutor(
            @PathVariable Long tutorId,
            @RequestBody @Valid ValoracionTutorRequest request,
            @AuthenticationPrincipal UserDetails u) {
        service.valorarTutor(tutorId, uid(u), request);
        return ResponseEntity.ok().build();
    }

    /**
     * POST /api/recommendations/{id}/feedback Body: { "esUtil": true, "comentario": "...",
     * "satisfaccion": 4 }
     */
    @PostMapping("/{id}/feedback")
    public ResponseEntity<Void> feedback(
            @PathVariable Long id,
            @RequestBody @Valid FeedbackRecomendacionRequest request,
            @AuthenticationPrincipal UserDetails u) {
        service.darFeedbackRecomendacion(id, request, uid(u));
        return ResponseEntity.ok().build();
    }

    /**
     * POST /api/recommendations/{id}/vista Llamar cuando el frontend muestra la tarjeta al usuario.
     */
    @PostMapping("/{id}/vista")
    public ResponseEntity<Void> marcarVista(
            @PathVariable Long id, @AuthenticationPrincipal UserDetails u) {
        service.marcarComoVista(id, uid(u));
        return ResponseEntity.ok().build();
    }

    /** DELETE /api/recommendations/{id} El usuario descarta manualmente una recomendación. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id, @AuthenticationPrincipal UserDetails u) {
        service.eliminarRecomendacion(id, uid(u));
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/recommendations/actividad Registrar búsqueda, clic, visualización… para alimentar
     * el motor. Body: { "tipoActividad": "BUSQUEDA", "categoriaObjeto": "Tutor",
     * "terminosBusqueda": "física" }
     */
    @PostMapping("/actividad")
    public ResponseEntity<Void> actividad(
            @RequestBody @Valid RegistrarActividadRequest request,
            @AuthenticationPrincipal UserDetails u) {
        service.registrarActividad(uid(u), request);
        return ResponseEntity.accepted().build();
    }

    /**
     * POST /api/recommendations/refresh Forzar regeneración — llamar tras actualizar perfil o
     * intereses.
     */
    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(@AuthenticationPrincipal UserDetails u) {
        service.generarRecomendacionesUsuario(uid(u));
        return ResponseEntity.accepted().build();
    }

    // -----------------------------------------------------------------------
    // Helper — getUsername() devuelve el ID numérico como String
    // -----------------------------------------------------------------------
    private Long uid(UserDetails u) {
        return Long.parseLong(u.getUsername());
    }
}
