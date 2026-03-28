package es.us.meerkat.backend.dto.events;

import java.time.LocalDateTime;

import es.us.meerkat.backend.dto.maps.UbicacionResponse;
import es.us.meerkat.backend.dto.users.UserPublicResponse;
import es.us.meerkat.backend.entity.EstadoAsistencia;
import es.us.meerkat.backend.entity.TipoEvento;
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
    /** Tipo de evento (REUNION, EXAMEN, CUESTIONARIO, TUTORIA, CLASE, OTRO). */
    private TipoEvento tipoEvento;

    //
    // /** Icono emoji del tipo de evento (ej: "📝", "👥"). */
    private String iconoEvento;

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

    /** ID de la tarea de Google Classroom vinculada. */
    private String classroomTaskId;

    /** Título de la tarea de Google Classroom vinculada. */
    private String classroomTaskTitle;

    /** URL de la tarea de Google Classroom vinculada. */
    private String classroomTaskUrl;

    /** ID de la comunidad a la que pertenece el evento. */
    private Long comunidadId;

    /** Nombre de la comunidad a la que pertenece el evento. */
    private String comunidadNombre;

    /** Usuario creador del evento. */
    private UserPublicResponse creador;

    /** Identificador del creador del evento. */
    private Long creadorId;

    /** Rol del creador en la comunidad del evento (ADMIN, PROFESOR, ALUMNO). */
    private String creadorRolComunidad;

    /** Estado de asistencia del usuario autenticado (null si no está registrado). */
    private EstadoAsistencia miAsistencia;

    /** Fecha de creación del evento. */
    private LocalDateTime createdAt;

    /** Ubicación del evento. */
    private UbicacionResponse ubicacion;
}
