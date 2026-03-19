package es.us.meerkat.backend.dto;

public record CommunityRankingEntryResponse(
        UserSimpleResponse usuario,
        long mensajes,
        long eventosCreados,
        long asistentesEventos,
        long puntos) {}
