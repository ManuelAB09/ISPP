package es.us.meerkat.backend.dto.communities;

import java.time.LocalDateTime;

import es.us.meerkat.backend.dto.google.ClassroomInfoResponse;
import es.us.meerkat.backend.dto.users.UserSimpleResponse;

public record CommunityDetailResponse(
        Long id,
        String nombre,
        String descripcion,
        String tipoGrupo,
        String tipoPlan,
        Integer maxMiembros,
        Long miembrosActuales,
        UserSimpleResponse creador,
        String estado,
        Boolean esMiembro,
        String miRol,
        String miRolDocente,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String imagenUrl,
        ClassroomInfoResponse classroom) {}
