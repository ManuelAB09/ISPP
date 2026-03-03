package es.us.meerkat.backend.dto;

import lombok.Data;

@Data
public class EnviarMensajeRequest {

    private Long tutorId;
    private Long userId;
    private String contenido;
}
