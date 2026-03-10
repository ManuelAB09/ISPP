package es.us.meerkat.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** DTO para crear un nuevo anuncio en una comunidad. */
public record CreateAnuncioRequest(
        @NotBlank(message = "El título es requerido")
                @Size(min = 5, max = 200, message = "El título debe tener entre 5 y 200 caracteres")
                String titulo,
        @NotBlank(message = "El contenido es requerido")
                @Size(
                        min = 10,
                        max = 5000,
                        message = "El contenido debe tener entre 10 y 5000 caracteres")
                String contenido,
        Boolean permitirComentarios) {}
