package es.us.meerkat.backend.dto.chats;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

/** DTO para responder con información de un mensaje de comunidad. */
@Data
@Builder
public class MensajeComunidadResponse {

    private Long id;

    private String contenido;

    private Boolean editado;

    private LocalDateTime createdAt;

    private LocalDateTime editedAt;

    private Long usuarioId;

    private String usuarioNombre;

    private String usuarioFoto;

    private Long comunidadId;

    private String comunidadNombre;

    private String comunidadImagenUrl;

    private String archivoUrl;

    private String archivoNombre;

    private String archivoMimeType;

    private Long archivoTamano;
}
