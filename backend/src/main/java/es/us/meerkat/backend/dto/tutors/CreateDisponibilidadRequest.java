package es.us.meerkat.backend.dto.tutors;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request DTO para crear o actualizar disponibilidad de un tutor. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud para crear disponibilidad de tutor")
public class CreateDisponibilidadRequest {

    @NotNull(message = "Es recurrente es requerido")
    @Schema(description = "¿Es una disponibilidad recurrente (cada semana)?", example = "true")
    private Boolean esRecurrente;

    @Schema(
            description = "Día de la semana si es recurrente (MONDAY, TUESDAY, etc)",
            example = "MONDAY")
    private DayOfWeek diaSemana;

    @Schema(description = "Fecha específica si no es recurrente", example = "2026-03-15T10:00:00")
    private LocalDateTime fechaPuntual;

    @NotNull(message = "Hora de inicio es requerida")
    @Schema(description = "Hora de inicio (HH:mm)", example = "10:00")
    private LocalTime horaInicio;

    @NotNull(message = "Hora de fin es requerida")
    @Schema(description = "Hora de fin (HH:mm)", example = "12:00")
    private LocalTime horaFin;

    @NotBlank(message = "Modalidad es requerida")
    @Schema(description = "Modalidad (PRESENCIAL, VIRTUAL, HIBRIDA)", example = "VIRTUAL")
    private String modalidad;

    @Schema(
            description = "Ubicación si es presencial",
            example = "Aula 101, Facultad de Ingeniería")
    private String ubicacionPresencial;
}
