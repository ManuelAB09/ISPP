package es.us.meerkat.backend.dto.events;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import es.us.meerkat.backend.entity.TipoEvento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa un evento en el listado unificado "Mis Eventos" del usuario.
 *
 * <p>Incluye información visual para el frontend: tipo con icono, indicadores de proximidad y
 * estado de la alerta.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MisEventosItemResponse {

    /** Identificador del evento. */
    private Long id;

    /** Título del evento. */
    private String titulo;

    /** Descripción breve del evento. */
    private String descripcion;

    /** Tipo de evento (REUNION, EXAMEN, CUESTIONARIO, TUTORIA, CLASE, OTRO). */
    private TipoEvento tipoEvento;

    /** Nombre legible del tipo de evento. */
    private String tipoEventoNombre;

    /** Icono emoji asociado al tipo de evento. */
    private String icono;

    /** Fecha y hora de inicio del evento. */
    private LocalDateTime fechaHora;

    /** Fecha y hora de fin del evento. */
    private LocalDateTime fechaFin;

    /** Si el evento está en las próximas 24 horas (destacar visualmente). */
    private Boolean proximaEn24H;

    /** Si el evento comienza en 15 minutos o menos (alerta inminente). */
    private Boolean inminenteEn15Min;

    /** Si el usuario tiene el evento en su agenda (asistencia confirmada). */
    private Boolean asistenciaConfirmada;

    /** Si el evento es virtual. */
    private Boolean esVirtual;

    /** Si el evento ha sido cancelado. */
    private Boolean cancelado;

    /** Nombre de la comunidad a la que pertenece el evento. */
    private String comunidadNombre;

    /** ID de la comunidad. */
    private Long comunidadId;

    /** Nombre de la ubicación (si aplica). */
    private String ubicacionNombre;

    /** Número de asistentes confirmados. */
    private Integer asistentesConfirmados;

    /** Aforo máximo. */
    private Integer aforo;
}
