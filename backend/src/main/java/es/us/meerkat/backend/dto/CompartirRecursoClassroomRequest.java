package es.us.meerkat.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request DTO para compartir un recurso (documento, presentación, etc). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud para compartir un recurso educativo")
public class CompartirRecursoClassroomRequest {

    @NotBlank(message = "Tipo de recurso requerido")
    @Schema(
            description = "Tipo: DOCUMENTO, PRESENTACION, VIDEO, ENLACE, ARCHIVO",
            example = "PRESENTACION")
    private String tipo;

    @NotBlank(message = "Título requerido")
    @Schema(
            description = "Título del recurso",
            example = "Presentación: Introducción a la Biología")
    private String titulo;

    @Schema(description = "Descripción del recurso")
    private String descripcion;

    @NotBlank(message = "URL requerida")
    @Schema(
            description = "URL del recurso (Drive link, YouTube, etc)",
            example = "https://drive.google.com/file/d/...")
    private String url;

    @Schema(description = "ID de Google Drive si aplica", example = "abc123def456")
    private String googleDriveFileId;

    @Schema(description = "Publicar en Classroom automáticamente", example = "true")
    private Boolean publicarEnClassroom;

    @NotBlank(message = "Acepta términos")
    @Schema(description = "Acepta términos y condiciones", example = "true")
    private Boolean aceptarTerminos;
}
