package es.us.meerkat.backend.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO con el resumen de un evento.
 *
 * <p>Contiene la información básica de un evento para mostrar en listados.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventSummaryResponse {

    /** Identificador del evento. */
    private Long id;

    /** Título del evento. */
    private String titulo;

    /** Descripción del evento. */
    private String descripcion;

    /** Fecha y hora de inicio del evento. */
    private LocalDateTime fechaHora;

    /** Ubicación del evento. */
    private UbicacionResponse ubicacion;

    /** Aforo máximo del evento. */
    private Integer aforo;

    /** Número de asistentes confirmados. */
    private Integer asistentesConfirmados;

    /** Si el evento tiene modalidad virtual. */
    private Boolean esVirtual;

    /** Si el evento está cancelado. */
    private Boolean cancelado;

    /** Identificador de la comunidad a la que pertenece. */
    private Long comunidadId;

    /** Nombre de la comunidad a la que pertenece. */
    private String comunidadNombre;
}
