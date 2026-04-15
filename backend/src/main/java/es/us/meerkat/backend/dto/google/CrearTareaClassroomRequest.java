package es.us.meerkat.backend.dto.google;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request DTO para crear una tarea en el aula. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud para crear una tarea")
public class CrearTareaClassroomRequest {

    @NotBlank(message = "Título requerido")
    @Schema(description = "Título de la tarea", example = "Ensayo sobre sostenibilidad")
    private String titulo;

    @Schema(description = "Descripción detallada", example = "Escribe un ensayo...")
    private String descripcion;

    @NotNull(message = "Fecha de vencimiento requerida")
    @Schema(description = "Fecha de vencimiento", example = "2026-03-20T23:59:00")
    private LocalDateTime fechaVencimiento;

    @Schema(description = "Puntuación máxima", example = "100")
    private Integer puntosMaximos;

    @NotNull(message = "¿Publicar en Classroom?")
    @Schema(description = "Publicar en Google Classroom automáticamente", example = "true")
    private Boolean publicarEnClassroom;

    @NotNull(message = "Acepta términos")
    @Schema(description = "Acepta términos y condiciones", example = "true")
    private Boolean aceptarTerminos;
}
