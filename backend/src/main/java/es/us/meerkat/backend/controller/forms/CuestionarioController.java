package es.us.meerkat.backend.controller.forms;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import es.us.meerkat.backend.dto.forms.CreateCuestionarioRequest;
import es.us.meerkat.backend.dto.forms.SubmitAttemptRequest;
import es.us.meerkat.backend.entity.forms.Cuestionario;
import es.us.meerkat.backend.entity.forms.CuestionarioIntento;
import es.us.meerkat.backend.entity.forms.Opcion;
import es.us.meerkat.backend.entity.forms.Pregunta;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.service.forms.CuestionarioService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/cuestionarios")
@Tag(name = "Cuestionarios", description = "Gestión de cuestionarios y preguntas personalizadas")
@RequiredArgsConstructor
public class CuestionarioController {

    private final CuestionarioService cuestionarioService;

    private static Map<String, Object> toResponse(Cuestionario c) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", c.getId());
        response.put("titulo", c.getTitulo());
        response.put("descripcion", c.getDescripcion());
        response.put("imagenUrl", c.getImagenUrl());
        response.put("materia", c.getMateria());
        response.put("dificultad", c.getDificultad() != null ? c.getDificultad().name() : null);
        response.put("nivelEducativo", c.getNivelEducativo());
        response.put("numPreguntas", c.getNumPreguntas());
        response.put("tiempoEstimadoMinutos", c.getTiempoEstimadoMinutos());
        response.put("activo", c.getActivo());
        response.put("publicado", c.getPublicado());
        response.put("createdAt", c.getCreatedAt());
        response.put(
                "comunidadesIds", c.getComunidades().stream().map(com -> com.getId()).toList());
        return response;
    }

    private static Map<String, Object> toPreguntaResolver(Pregunta p) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", p.getId());
        item.put("enunciado", p.getEnunciado());
        item.put("tipo", p.getTipo() != null ? p.getTipo().name() : null);
        item.put(
                "opciones",
                p.getOpciones().stream()
                        .sorted(
                                java.util.Comparator.comparing(
                                        Opcion::getOrden,
                                        java.util.Comparator.nullsLast(
                                                java.util.Comparator.naturalOrder())))
                        .map(
                                o -> {
                                    Map<String, Object> op = new LinkedHashMap<>();
                                    op.put("id", o.getId());
                                    op.put("texto", o.getTexto());
                                    op.put("orden", o.getOrden());
                                    return op;
                                })
                        .toList());
        return item;
    }

    private static Map<String, Object> toIntentoResumen(CuestionarioIntento intento) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", intento.getId());
        item.put("puntuacion", intento.getPuntuacion());
        item.put("createdAt", intento.getCreatedAt());
        return item;
    }

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

        Cuestionario created = cuestionarioService.createFromDto(request, usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    /** Lista cuestionarios creados por el usuario autenticado. */
    @GetMapping("/mine")
    public ResponseEntity<?> listMine(@AuthenticationPrincipal Usuario usuario) {
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<Map<String, Object>> items =
                cuestionarioService.findByCreadorId(usuario.getId()).stream()
                        .map(CuestionarioController::toResponse)
                        .toList();
        return ResponseEntity.ok(items);
    }

    /** Lista cuestionarios asociados a una comunidad concreta. */
    @GetMapping("/community/{communityId}")
    public ResponseEntity<List<Map<String, Object>>> listByCommunity(
            @PathVariable Long communityId, @AuthenticationPrincipal Usuario usuario) {
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<Map<String, Object>> items =
                cuestionarioService.findByComunidadId(communityId).stream()
                        .map(CuestionarioController::toResponse)
                        .toList();
        return ResponseEntity.ok(items);
    }

    /** Lista cuestionarios públicos creados por un usuario específico (para visitar su perfil). */
    @GetMapping("/user/{userId}/public")
    public ResponseEntity<List<Map<String, Object>>> listPublicByUserId(@PathVariable Long userId) {
        List<Map<String, Object>> items =
                cuestionarioService.findByCreadorId(userId).stream()
                        .filter(c -> Boolean.TRUE.equals(c.getPublicado()))
                        .map(CuestionarioController::toResponse)
                        .toList();
        return ResponseEntity.ok(items);
    }

    /** Obtiene un cuestionario por id. */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        return cuestionarioService
                .findById(id)
                .map(CuestionarioController::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Vista previa para iniciar el cuestionario, con intentos previos del usuario. */
    @GetMapping("/{id}/preview")
    public ResponseEntity<?> getPreview(
            @PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return cuestionarioService
                .findById(id)
                .map(
                        cuestionario -> {
                            Map<String, Object> body = toResponse(cuestionario);
                            List<Map<String, Object>> intentos =
                                    cuestionarioService
                                            .findAttemptsByUsuarioAndCuestionario(
                                                    usuario.getId(), id)
                                            .stream()
                                            .map(CuestionarioController::toIntentoResumen)
                                            .toList();

                            body.put("intentosPrevios", intentos);
                            body.put(
                                    "puedeResolver", Boolean.TRUE.equals(cuestionario.getActivo()));
                            return ResponseEntity.ok(body);
                        })
                .orElse(ResponseEntity.notFound().build());
    }

    /** Obtiene el cuestionario para resolverlo (sin revelar respuestas correctas). */
    @GetMapping("/{id}/resolver")
    public ResponseEntity<?> getResolver(
            @PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return cuestionarioService
                .findById(id)
                .map(
                        cuestionario -> {
                            Map<String, Object> body = toResponse(cuestionario);
                            body.put(
                                    "preguntas",
                                    cuestionario.getPreguntas().stream()
                                            .map(CuestionarioController::toPreguntaResolver)
                                            .toList());
                            return ResponseEntity.ok(body);
                        })
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
            CuestionarioService.AttemptSubmissionResult resultado =
                    cuestionarioService.submitAttempt(id, request, usuario);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("attemptId", resultado.intento().getId());
            body.put("cuestionarioId", id);
            body.put("puntuacion", resultado.intento().getPuntuacion());
            body.put("createdAt", resultado.intento().getCreatedAt());
            body.put("totalPreguntas", resultado.totalPreguntas());
            body.put("preguntasCorrectas", resultado.preguntasCorrectas());
            body.put(
                    "preguntasIncorrectas",
                    Math.max(0, resultado.totalPreguntas() - resultado.preguntasCorrectas()));
            body.put(
                    "preguntas",
                    resultado.preguntas().stream()
                            .map(
                                    p -> {
                                        Map<String, Object> item = new LinkedHashMap<>();
                                        item.put("preguntaId", p.preguntaId());
                                        item.put("enunciado", p.enunciado());
                                        item.put("tipo", p.tipo() != null ? p.tipo().name() : null);
                                        item.put("acertada", p.acertada());
                                        item.put("respuestaUsuario", p.respuestaUsuario());
                                        item.put("respuestaCorrecta", p.respuestaCorrecta());
                                        return item;
                                    })
                            .toList());

            return ResponseEntity.ok(body);
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
            return ResponseEntity.ok(toResponse(updated));
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
            return ResponseEntity.ok(toResponse(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
