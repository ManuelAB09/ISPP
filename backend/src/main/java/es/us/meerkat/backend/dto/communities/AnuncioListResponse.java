package es.us.meerkat.backend.dto.communities;

import java.util.List;

/** DTO para listar anuncios de una comunidad. */
public record AnuncioListResponse(
        List<AnuncioResponse> anuncios, Integer total, Integer pagina, Integer tamaño) {}
