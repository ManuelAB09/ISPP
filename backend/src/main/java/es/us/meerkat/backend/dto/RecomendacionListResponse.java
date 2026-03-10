package es.us.meerkat.backend.dto;

import java.util.List;

/** DTO para listar recomendaciones de comunidades. */
public record RecomendacionListResponse(
        List<RecomendacionResponse> recomendaciones,
        Integer total,
        Integer pagina,
        Integer tamaño) {}
