package es.us.meerkat.backend.dto;

import lombok.Data;

/** DTO para enviar un mensaje en el chat de una comunidad. */
@Data
public class EnviarMensajeComunidadRequest {

    /** ID de la comunidad donde se envía el mensaje. */
    private Long comunidadId;

    /** Contenido del mensaje. */
    private String contenido;
}
