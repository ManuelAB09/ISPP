package es.us.meerkat.backend.dto.communities;

import java.time.LocalDateTime;

import es.us.meerkat.backend.dto.users.UserSimpleResponse;

public record MemberResponse(
        Long id,
        UserSimpleResponse usuario,
        String rol,
        String rolDocente,
        LocalDateTime fechaIngreso) {}
