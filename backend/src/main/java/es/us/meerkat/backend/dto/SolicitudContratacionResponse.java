package es.us.meerkat.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO de respuesta para una solicitud de contratación directa. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudContratacionResponse {

    private Long id;
    private Long alumnoId;
    private String alumnoNombre;
    private String alumnoFoto;
    private Long tutorId;
    private String tutorNombre;
    private String tutorFoto;
    private LocalDate dia;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private BigDecimal tarifaHora;
    private BigDecimal importeTotal;
    private String modalidad;
    private String mensaje;
    private String estado;
    private String motivoRechazo;
    private Boolean tutorStripeConfigured;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
