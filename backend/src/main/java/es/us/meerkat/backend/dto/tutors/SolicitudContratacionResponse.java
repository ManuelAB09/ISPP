package es.us.meerkat.backend.dto.tutors;

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
    private LocalDate diaOriginal;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private BigDecimal tarifaHora;
    private BigDecimal importeTotal;
    private String modalidad;
    private String mensaje;
    private String estado;
    private String motivoRechazo;
    private Boolean tutorStripeConfigured;
    private Integer calificacion;
    private String comentarioAlumno;
    private Boolean puedeSerCanceladaPorAlumno;
    private String reprogramacionDia;
    private String reprogramacionHoraInicio;
    private String reprogramacionHoraFin;
    private String estadoAnterior;
    private String ubicacionClase;
    private String zoomJoinUrl;
    private String zoomStartUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
