package es.us.meerkat.backend.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.entity.Valoracion;
import es.us.meerkat.backend.repository.UsuarioRepository;
import es.us.meerkat.backend.service.ValoracionService;

@RestController
@RequestMapping("/api/valoraciones")
public class ValoracionController {
    @Autowired private ValoracionService valoracionService;

    @Autowired private UsuarioRepository usuarioRepository;

    @PostMapping
    public ResponseEntity<?> valorarProfesor(
            @RequestBody Valoracion valoracion, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body("No autenticado");
        }
        Usuario alumno = usuarioRepository.findByUsername(userDetails.getUsername()).orElse(null);
        if (alumno == null) {
            return ResponseEntity.status(401).body("Usuario no encontrado");
        }
        valoracion.setAlumno(alumno);
        Valoracion guardada = valoracionService.guardarValoracion(valoracion);
        return ResponseEntity.ok(guardada);
    }

    @GetMapping("/profesor/{profesorId}/stats")
    public ResponseEntity<Map<String, Object>> getStats(@PathVariable Long profesorId) {
        Double media = valoracionService.obtenerMediaPorProfesor(profesorId);
        Long total = valoracionService.contarPorProfesor(profesorId);
        String nivel = "principiante";
        if (total >= 10 && total <= 50 && media != null && media >= 3) {
            nivel = "avanzado";
        } else if (total > 50 && media != null && media >= 4.5) {
            nivel = "experto";
        } else if (total < 10 || (media != null && media < 3)) {
            nivel = "principiante";
        }
        Map<String, Object> result = new HashMap<>();
        result.put("media", media != null ? media : 0);
        result.put("total", total);
        result.put("nivel", nivel);
        return ResponseEntity.ok(result);
    }
}
