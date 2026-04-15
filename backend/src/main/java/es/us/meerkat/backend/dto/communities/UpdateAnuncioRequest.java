package es.us.meerkat.backend.dto.communities;

import jakarta.validation.constraints.Size;

/** DTO para actualizar un anuncio en una comunidad. */
public record UpdateAnuncioRequest(
        @Size(min = 5, max = 200, message = "El título debe tener entre 5 y 200 caracteres")
                String titulo,
        @Size(min = 10, max = 5000, message = "El contenido debe tener entre 10 y 5000 caracteres")
                String contenido,
        Boolean permitirComentarios) {}
