package es.us.meerkat.backend.dto.communities;

import java.time.LocalDateTime;

import es.us.meerkat.backend.dto.users.UserSimpleResponse;

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
