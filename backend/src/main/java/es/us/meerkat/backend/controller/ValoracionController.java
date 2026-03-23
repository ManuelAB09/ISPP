package es.us.meerkat.backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import es.us.meerkat.backend.entity.Valoracion;
import es.us.meerkat.backend.service.ValoracionService;

@RestController
@RequestMapping("/api/valoraciones")
public class ValoracionController {
    @Autowired private ValoracionService valoracionService;

    @PostMapping
    public ResponseEntity<?> crearValoracion(@RequestBody Valoracion valoracion) {
        try {
            Valoracion guardada = valoracionService.guardarValoracion(valoracion);
            return ResponseEntity.ok(guardada);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/check")
    public ResponseEntity<Map<String, Boolean>> checkAlreadyRated(
            @RequestParam Long alumnoId, @RequestParam Long eventoId) {
        boolean rated = valoracionService.isAlreadyRated(alumnoId, eventoId);
        return ResponseEntity.ok(Map.of("rated", rated));
    }

    @GetMapping("/profesor/{profesorId}")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticas(@PathVariable Long profesorId) {
        Double media = valoracionService.obtenerMediaPorProfesor(profesorId);
        Long total = valoracionService.contarValoracionesPorProfesor(profesorId);
        String nivel = valoracionService.calcularNivel(profesorId);
        return ResponseEntity.ok(
                Map.of(
                        "media", media == null ? 0 : media,
                        "total", total == null ? 0 : total,
                        "nivel", nivel));
    }

    @GetMapping("/profesor/{profesorId}/todas")
    public ResponseEntity<List<Valoracion>> obtenerValoraciones(@PathVariable Long profesorId) {
        return ResponseEntity.ok(valoracionService.obtenerValoracionesPorProfesor(profesorId));
    }
}
