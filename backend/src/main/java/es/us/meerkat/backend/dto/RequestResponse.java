package es.us.meerkat.backend.dto;

import java.time.LocalDateTime;

public record RequestResponse(
        Long id,
        UserSimpleResponse solicitante,
        String estado,
        String mensaje,
        String rolDeseado,
        LocalDateTime fechaSolicitud,
        UserSimpleResponse respondidaPor,
        LocalDateTime fechaRespuesta) {}
