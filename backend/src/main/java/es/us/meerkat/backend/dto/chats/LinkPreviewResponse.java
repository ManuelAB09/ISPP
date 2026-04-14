package es.us.meerkat.backend.dto.chats;

import lombok.Builder;
import lombok.Data;

/** DTO de salida con los metadatos de una URL. */
@Data
@Builder
public class LinkPreviewResponse {

    /** URL final resuelta tras redirecciones. */
    private String url;

    /** Dominio principal de la URL. */
    private String domain;

    /** Título de la página. */
    private String title;

    /** Descripción corta de la página. */
    private String description;

    /** Nombre del sitio (si existe en metadatos). */
    private String siteName;

    /** Imagen representativa de la página. */
    private String image;
}
