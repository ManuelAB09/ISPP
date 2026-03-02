package es.us.meerkat.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** DTO de entrada para solicitar la previsualización de una URL. */
@Data
public class LinkPreviewRequest {

    /** URL a analizar para extraer metadatos. */
    @NotBlank(message = "La URL es obligatoria")
    private String url;
}
