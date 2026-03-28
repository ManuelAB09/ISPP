package es.us.meerkat.backend.dto.communities;

import java.time.LocalDateTime;

import es.us.meerkat.backend.dto.users.UserSimpleResponse;
import es.us.meerkat.backend.entity.EstadoInvitacion;
import es.us.meerkat.backend.entity.RolComunidad;

/** DTO de respuesta para invitaciones a comunidades. */
public record InvitacionResponse(
        Long id,
        String email,
        String codigo,
        RolComunidad rol,
        EstadoInvitacion estado,
        LocalDateTime createdAt,
        LocalDateTime fechaExpiracion,
        LocalDateTime fechaAceptacion,
        UserSimpleResponse usuarioInvitador,
        UserSimpleResponse usuarioAceptador) {}
