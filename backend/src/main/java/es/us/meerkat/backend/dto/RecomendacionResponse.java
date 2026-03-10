package es.us.meerkat.backend.dto;

import es.us.meerkat.backend.entity.FactorRecomendacion;

/** DTO de respuesta para recomendaciones de comunidades. */
public record RecomendacionResponse(
        Long id,
        Long communityId,
        String nombre,
        String descripcion,
        String imagenUrl,
        FactorRecomendacion factor,
        Double relevancia,
        String motivo) {}
