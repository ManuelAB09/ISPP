package es.us.meerkat.backend.dto.communities;

import java.time.LocalDateTime;

import es.us.meerkat.backend.dto.users.UserSimpleResponse;

public record ComentarioAnuncioResponse(
        Long id, String texto, UserSimpleResponse usuario, LocalDateTime createdAt) {}
