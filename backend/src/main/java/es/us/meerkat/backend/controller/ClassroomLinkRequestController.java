package es.us.meerkat.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import es.us.meerkat.backend.dto.ClassroomLinkRequestResponse;
import es.us.meerkat.backend.dto.CompleteClassroomLinkRequest;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.service.google.ClassroomLinkRequestService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/classroom-link-requests")
@RequiredArgsConstructor
public class ClassroomLinkRequestController {
    private final ClassroomLinkRequestService service;

    @GetMapping("/me")
    public ResponseEntity<List<ClassroomLinkRequestResponse>> listarSolicitudesPendientes(
            @AuthenticationPrincipal Usuario tutor) {
        if (tutor == null) {
            return ResponseEntity.status(401).build();
        }
        List<ClassroomLinkRequestResponse> solicitudes =
                service.listarSolicitudesPendientes(tutor.getId());
        return ResponseEntity.ok(solicitudes);
    }

    @PostMapping("/{solicitudId}/link")
    public ResponseEntity<?> completarSolicitud(
            @PathVariable Long solicitudId,
            @AuthenticationPrincipal Usuario tutor,
            @RequestBody CompleteClassroomLinkRequest request) {
        if (tutor == null) {
            return ResponseEntity.status(401).body("Usuario no autenticado.");
        }
        service.completarSolicitud(
                solicitudId, tutor.getId(), request.cursoId(), request.nombreCurso());
        return ResponseEntity.ok().build();
    }
}
