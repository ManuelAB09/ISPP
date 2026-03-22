package es.us.meerkat.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response DTO para una reserva de clase con todos sus detalles. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Detalles de una reserva de clase")
public class ReservaClaseDetailResponse {

    @Schema(description = "ID de la reserva", example = "123")
    private Long id;

    @Schema(description = "ID del alumno")
    private Long alumnoId;

    @Schema(description = "Nombre del alumno")
    private String alumnoNombre;

    @Schema(description = "ID del tutor")
    private Long tutorId;

    @Schema(description = "Nombre del tutor")
    private String tutorNombre;

    @Schema(description = "Fecha y hora de la clase", example = "2026-03-15T10:00:00")
    private LocalDateTime fechaHora;

    @Schema(description = "Duración en minutos", example = "60")
    private Integer duracionMinutos;

    @Schema(description = "Modalidad (PRESENCIAL, VIRTUAL, HIBRIDA)")
    private String modalidad;

    @Schema(description = "Ubicación si es presencial")
    private String ubicacion;

    @Schema(description = "URL de la sala virtual si es virtual")
    private String enlaceVirtual;

    @Schema(description = "Tema o asignatura")
    private String tema;

    @Schema(description = "Descripción adicional")
    private String descripcion;

    @Schema(description = "Tarifa en EUR/USD/etc", example = "25.50")
    private BigDecimal tarifa;

    @Schema(description = "Moneda (EUR, USD, etc)")
    private String moneda;

    @Schema(
            description =
                    "Estado actual (PENDIENTE, CONFIRMADA, COMPLETADA, CANCELADA_ALUMNO,"
                            + " CANCELADA_TUTOR, NO_ASISTIDA)")
    private String estado;

    @Schema(description = "Calificación del alumno (1-5)")
    private Integer calificacion;

    @Schema(description = "Comentario del alumno después de la clase")
    private String comentarioAlumno;

    @Schema(description = "Calificación del tutor al alumno (1-5)")
    private Integer calificacionTutor;

    @Schema(description = "Comentario del tutor")
    private String comentarioTutor;

    @Schema(description = "URL de grabación si es virtual")
    private String urlGrabacion;

    @Schema(description = "Notas de la tutela")
    private String notasTutela;

    @Schema(description = "Fecha de creación", example = "2026-02-15T09:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Fecha de confirmación por el tutor")
    private LocalDateTime fechaConfirmacion;

    @Schema(description = "¿Puede ser cancelada con las políticas actuales?")
    private Boolean puedeSerCancelada;

    @Schema(description = "Motivo de cancelación si aplica")
    private String motivoCancelacion;
}
