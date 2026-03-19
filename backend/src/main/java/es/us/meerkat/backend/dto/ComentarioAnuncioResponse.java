package es.us.meerkat.backend.dto;

import java.time.LocalDateTime;

public record ComentarioAnuncioResponse(
        Long id, String texto, UserSimpleResponse usuario, LocalDateTime createdAt) {}
