package es.us.meerkat.backend.dto.chats;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MensajeResponse {

    private Long id;
    private String contenido;
    private Boolean editado;
    private LocalDateTime createdAt;
    private Long emisorId;
    private Long receptorId;
    private String archivoUrl;
    private String archivoNombre;
    private String archivoMimeType;
    private Long archivoTamano;
}
