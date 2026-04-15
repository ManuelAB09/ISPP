package es.us.meerkat.backend.dto.communities;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para respuestas de apuntes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApunteResponse {

    private Long id;
    private String titulo;
    private String descripcion;
    private String nombreArchivo;
    private String tipoMime;
    private Long tamanioArchivo;
    private Integer descargas;
    private Double valoracionMedia;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Long usuarioId;
    private String usuarioNombre;
    private String usuarioFoto;

    private Long comunidadId;
}
