package es.us.meerkat.backend.dto;

import java.time.LocalDateTime;

public record MemberResponse(
        Long id,
        UserSimpleResponse usuario,
        String rol,
        String rolDocente,
        LocalDateTime fechaIngreso) {}
