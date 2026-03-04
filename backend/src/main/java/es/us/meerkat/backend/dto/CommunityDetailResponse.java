package es.us.meerkat.backend.dto;

import java.time.LocalDateTime;

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
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String imagenUrl,
        ClassroomInfoResponse classroom) {}
