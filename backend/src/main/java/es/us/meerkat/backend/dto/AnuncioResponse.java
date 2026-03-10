package es.us.meerkat.backend.dto;

import java.time.LocalDateTime;

/** DTO de respuesta para anuncios de comunidades. */
public record AnuncioResponse(
        Long id,
        String titulo,
        String contenido,
        UserSimpleResponse usuario,
        Boolean permitirComentarios,
        Boolean editado,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
