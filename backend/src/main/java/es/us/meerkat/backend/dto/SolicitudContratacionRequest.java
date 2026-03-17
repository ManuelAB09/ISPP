package es.us.meerkat.backend.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO para crear una solicitud de contratación directa a un tutor. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudContratacionRequest {

    @NotNull(message = "El día es obligatorio")
    @Future(message = "El día debe ser futuro")
    private LocalDate dia;

    @NotNull(message = "La hora de inicio es obligatoria")
    private LocalTime horaInicio;

    @NotNull(message = "La hora de fin es obligatoria")
    private LocalTime horaFin;

    @NotNull(message = "La modalidad es obligatoria")
    private String modalidad;

    @Size(max = 500, message = "El mensaje no puede superar los 500 caracteres")
    private String mensaje;
}
