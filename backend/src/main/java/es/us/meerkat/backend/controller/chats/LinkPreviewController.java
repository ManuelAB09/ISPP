package es.us.meerkat.backend.controller.chats;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.us.meerkat.backend.dto.chats.LinkPreviewRequest;
import es.us.meerkat.backend.dto.chats.LinkPreviewResponse;
import es.us.meerkat.backend.service.chats.LinkPreviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Controlador para generar vistas previas de enlaces compartidos en el chat. */
@RestController
@RequestMapping("/api/v1/link-preview")
@RequiredArgsConstructor
public final class LinkPreviewController {

    /** Servicio encargado de resolver y parsear los metadatos de una URL. */
    private final LinkPreviewService linkPreviewService;

    /**
     * Devuelve metadatos básicos de una URL para renderizar una tarjeta de preview.
     *
     * @param request DTO con la URL a previsualizar.
     * @return Respuesta con título, descripción, imagen y dominio.
     */
    @PostMapping
    public ResponseEntity<LinkPreviewResponse> preview(
            @Valid @RequestBody final LinkPreviewRequest request) {
        return ResponseEntity.ok(linkPreviewService.getPreview(request.getUrl()));
    }
}
