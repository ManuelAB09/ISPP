package es.us.meerkat.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.us.meerkat.backend.dto.EnviarMensajeRequest;
import es.us.meerkat.backend.dto.MensajeResponse;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.service.MensajeService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/mensajes")
@RequiredArgsConstructor
public class MensajeController {

    private final MensajeService mensajeService;

    @PostMapping
    public ResponseEntity<MensajeResponse> enviarMensaje(
            @AuthenticationPrincipal Usuario usuario, @RequestBody EnviarMensajeRequest request) {

        return ResponseEntity.ok(mensajeService.enviarMensaje(usuario.getId(), request));
    }

    @GetMapping("/tutor/{tutorId}")
    public ResponseEntity<List<MensajeResponse>> obtenerConversacion(
            @AuthenticationPrincipal Usuario usuario, @PathVariable Long tutorId) {

        return ResponseEntity.ok(mensajeService.obtenerConversacion(usuario.getId(), tutorId));
    }
}
