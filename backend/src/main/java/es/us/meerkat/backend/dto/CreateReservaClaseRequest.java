package es.us.meerkat.backend.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request DTO para crear una reserva de clase. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud para crear una reserva de clase")
public class CreateReservaClaseRequest {

    @NotNull(message = "Fecha y hora son requeridas")
    @Schema(description = "Fecha y hora de la clase", example = "2026-03-15T10:00:00")
    private LocalDateTime fechaHora;

    @NotNull(message = "Duración es requerida")
    @Min(value = 15, message = "La duración mínima es 15 minutos")
    @Schema(description = "Duración en minutos", example = "60")
    private Integer duracionMinutos;

    @NotBlank(message = "Modalidad es requerida")
    @Schema(description = "Modalidad (PRESENCIAL, VIRTUAL, HIBRIDA)", example = "VIRTUAL")
    private String modalidad;

    @Schema(description = "Ubicación si es presencial", example = "Starbucks Centro")
    private String ubicacion;

    @NotBlank(message = "Tema es requerido")
    @Schema(description = "Tema o asignatura de la clase", example = "Cálculo Diferencial")
    private String tema;

    @Schema(description = "Descripción adicional", example = "Necesito ayuda con integrales")
    private String descripcion;

    @NotNull(message = "Se requiere aceptar los términos")
    @Schema(description = "Aceptación de términos y condiciones", example = "true")
    private Boolean aceptarTerminos;
}
