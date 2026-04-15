package es.us.meerkat.backend.dto.communities;

import java.util.List;

/** DTO para listar invitaciones de una comunidad. */
public record InvitacionListResponse(
        List<InvitacionResponse> invitaciones, Integer total, Integer pagina, Integer tamaño) {}
