package es.us.meerkat.backend.dto;

import java.time.LocalDateTime;

public record UserSimpleResponse(
    Long id,
    String nombre,
    String email,
    String avatarUrl
) {}
