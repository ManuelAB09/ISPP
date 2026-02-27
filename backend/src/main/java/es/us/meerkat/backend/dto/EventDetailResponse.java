package es.us.meerkat.backend.dto;

import java.time.LocalDateTime;

import es.us.meerkat.backend.entity.EstadoAsistencia;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO con los detalles completos de un evento.
 *
 * <p>Contiene toda la información de un evento incluyendo comunidad, creador y estado de asistencia
 * del usuario actual.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventDetailResponse {

    /** Identificador del evento. */
    private Long id;

    /** Título del evento. */
    private String titulo;

    /** Descripción del evento. */
    private String descripcion;

    /** Fecha y hora de inicio del evento. */
    private LocalDateTime fechaHora;

    /** Fecha y hora de fin del evento. */
    private LocalDateTime fechaFin;

    /** Aforo máximo del evento. */
    private Integer aforo;

    /** Número de asistentes confirmados. */
    private Integer asistentesConfirmados;

    /** Qué llevar o qué preparar para el evento. */
    private String queLlevar;

    /** Si es visible en el mapa. */
    private Boolean visibleMapa;

    /** Si tiene modalidad virtual. */
    private Boolean esVirtual;

    /** Enlace para participación virtual. */
    private String enlaceVirtual;

    /** Si el evento está cancelado. */
    private Boolean cancelado;

    /** Motivo de la cancelación. */
    private String motivoCancelacion;

    /** Si el evento es privado. */
    private Boolean privado;

    /** Comunidad a la que pertenece el evento. */
    // private CommunitySummaryResponse comunidad;

    /** Usuario creador del evento. */
    private UserPublicResponse creador;

    /** Estado de asistencia del usuario autenticado (null si no está registrado). */
    private EstadoAsistencia miAsistencia;

    /** Fecha de creación del evento. */
    private LocalDateTime createdAt;

    /** Ubicación del evento. */
    private UbicacionResponse ubicacion;
}
