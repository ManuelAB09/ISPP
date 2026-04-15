package es.us.meerkat.backend.dto.communities;

import es.us.meerkat.backend.dto.users.UserSimpleResponse;

public record CommunityRankingEntryResponse(
        UserSimpleResponse usuario,
        long mensajes,
        long eventosCreados,
        long asistentesEventos,
        long puntos) {}
