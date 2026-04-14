package es.us.meerkat.backend.dto.tutors;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response DTO para una disponibilidad de tutor. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Disponibilidad horaria de un tutor")
public class DisponibilidadTutorResponse {

    @Schema(description = "ID de la disponibilidad", example = "101")
    private Long id;

    @Schema(description = "¿Es una disponibilidad recurrente?", example = "true")
    private Boolean esRecurrente;

    @Schema(description = "Día de la semana si es recurrente", example = "MONDAY")
    private DayOfWeek diaSemana;

    @Schema(description = "Fecha específica si no es recurrente")
    private LocalDateTime fechaPuntual;

    @Schema(description = "Hora de inicio", example = "10:00")
    private LocalTime horaInicio;

    @Schema(description = "Hora de fin", example = "12:00")
    private LocalTime horaFin;

    @Schema(description = "Modalidad (PRESENCIAL, VIRTUAL, HIBRIDA)")
    private String modalidad;

    @Schema(description = "Ubicación si es presencial")
    private String ubicacionPresencial;

    @Schema(description = "¿Está activa?", example = "true")
    private Boolean activa;

    @Schema(description = "Fecha de creación")
    private LocalDateTime createdAt;
}
