package es.us.meerkat.backend.dto;

import lombok.Data;

@Data
public class EnviarMensajeRequest {

    private Long tutorId;
    private String contenido;
}
