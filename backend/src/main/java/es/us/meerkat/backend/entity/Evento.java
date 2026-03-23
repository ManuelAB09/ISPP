package es.us.meerkat.backend.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import es.us.meerkat.backend.dto.EventDetailResponse;
import es.us.meerkat.backend.dto.EventSummaryResponse;
import es.us.meerkat.backend.dto.UbicacionResponse;
import es.us.meerkat.backend.dto.UserPublicResponse;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad que representa un evento de estudio en la plataforma.
 *
 * <p>Contiene información sobre eventos incluyendo fecha, ubicación, participantes y configuración
 * de visibilidad.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Evento {

    /** Identificador único del evento. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Título del evento. */
    private String titulo;

    /** Descripción detallada del evento. */
    private String descripcion;

    /** Fecha y hora de inicio del evento. */
    private LocalDateTime fechaHora;

    /** Fecha y hora de fin del evento. */
    private LocalDateTime fechaFin;

    /** Fecha y hora de la creación del evento. */
    private LocalDateTime createdAt;

    /** Aforo máximo del evento. */
    private Integer aforo;

    /** Número de participantes confirmados. */
    private Integer asistentesConfirmados;

    /** Cosas o vestimenta que hay que llevar al evento. */
    private String queLlevar;

    /** Enlace para participación virtual en el evento. */
    private String enlaceVirtual;

    /** Indica si el evento está cancelado. */
    private Boolean cancelado;

    /** Indica si el evento es visible en el mapa. */
    private Boolean visibleMapa;

    /** Indica si el evento tiene modalidad virtual. */
    private Boolean esVirtual;

    /** Motivo de cancelación del evento (si aplica). */
    private String motivoCancelacion;

    /** Indica si el evento es privado. */
    private Boolean privado;

    /** ID de la tarea de Google Classroom vinculada (si aplica). */
    private String classroomTaskId;

    /** Título de la tarea de Google Classroom vinculada (si aplica). */
    private String classroomTaskTitle;

    /** URL de la tarea de Google Classroom vinculada (si aplica). */
    private String classroomTaskUrl;

    /**
     * Tipo de evento: REUNION, EXAMEN, CUESTIONARIO, TUTORIA, CLASE u OTRO. Determina el icono y la
     * categoría visual en el panel "Mis Eventos".
     */
    @Enumerated(EnumType.STRING)
    private TipoEvento tipoEvento;

    /** Creador del evento. */
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario creador;

    /** Ubicación del evento (opcional). */
    @ManyToOne
    @JoinColumn(name = "ubicacion_id", nullable = true)
    private Ubicacion ubicacion;

    /** Comunidad a la que pertenece el evento. */
    @ManyToOne
    @JoinColumn(name = "comunidad_id")
    private Comunidad comunidad;

    /** Inicializa valores antes de persistir la entidad. */
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.cancelado == null) {
            this.cancelado = false;
        }
        if (this.privado == null) {
            this.privado = false;
        }
        if (this.asistentesConfirmados == null) {
            this.asistentesConfirmados = 0;
        }
        if (this.tipoEvento == null) {
            this.tipoEvento = TipoEvento.OTRO;
        }
    }

    // ========================
    // MÉTODOS DE GESTIÓN
    // ========================

    /**
     * Crea un nuevo evento con la información proporcionada.
     *
     * @param tituloParam Título del evento.
     * @param descripcionParam Descripción del evento.
     * @param fechaHoraParam Fecha y hora de inicio.
     * @param fechaFinParam Fecha y hora de fin.
     * @param aforoParam Aforo máximo.
     * @param queLlevarParam Qué llevar al evento.
     * @param esVirtualParam Si es evento virtual.
     * @param privadoParam Si es un evento privado.
     * @param tipoEventoParam Tipo de evento (REUNION, EXAMEN, etc.).
     */
    public void crear(
            final String tituloParam,
            final String descripcionParam,
            final LocalDateTime fechaHoraParam,
            final LocalDateTime fechaFinParam,
            final Integer aforoParam,
            final String queLlevarParam,
            final Boolean esVirtualParam,
            final Boolean privadoParam,
            final TipoEvento tipoEventoParam) {
        this.titulo = tituloParam;
        this.descripcion = descripcionParam;
        this.fechaHora = fechaHoraParam;
        this.fechaFin = fechaFinParam;
        this.aforo = aforoParam;
        this.queLlevar = queLlevarParam;
        this.esVirtual = esVirtualParam;
        this.privado = privadoParam;
        this.cancelado = false;
        this.asistentesConfirmados = 0;
        this.tipoEvento = tipoEventoParam != null ? tipoEventoParam : TipoEvento.OTRO;
    }

    /**
     * Sobrecarga de crear() para compatibilidad con código existente que no pasa tipoEvento. Asigna
     * TipoEvento.OTRO por defecto.
     */
    public void crear(
            final String tituloParam,
            final String descripcionParam,
            final LocalDateTime fechaHoraParam,
            final LocalDateTime fechaFinParam,
            final Integer aforoParam,
            final String queLlevarParam,
            final Boolean esVirtualParam,
            final Boolean privadoParam) {
        crear(
                tituloParam,
                descripcionParam,
                fechaHoraParam,
                fechaFinParam,
                aforoParam,
                queLlevarParam,
                esVirtualParam,
                privadoParam,
                TipoEvento.OTRO);
    }

    /**
     * Edita la información del evento existente.
     *
     * @param tituloParam Título del evento.
     * @param descripcionParam Descripción del evento.
     * @param fechaHoraParam Fecha y hora de inicio.
     * @param fechaFinParam Fecha y hora de fin.
     * @param aforoParam Aforo máximo.
     * @param queLlevarParam Qué llevar al evento.
     * @param esVirtualParam Si es evento virtual.
     * @param privadoParam Si es un evento privado.
     */
    public void editar(
            final String tituloParam,
            final String descripcionParam,
            final LocalDateTime fechaHoraParam,
            final LocalDateTime fechaFinParam,
            final Integer aforoParam,
            final String queLlevarParam,
            final Boolean esVirtualParam,
            final Boolean privadoParam) {
        this.titulo = tituloParam;
        this.descripcion = descripcionParam;
        this.fechaHora = fechaHoraParam;
        this.fechaFin = fechaFinParam;
        this.aforo = aforoParam;
        this.queLlevar = queLlevarParam;
        this.esVirtual = esVirtualParam;
        this.privado = privadoParam;
    }

    /**
     * Cancela el evento estableciendo el motivo de la cancelación.
     *
     * @param motivoParam El motivo de la cancelación.
     */
    public void cancelar(final String motivoParam) {
        this.cancelado = true;
        this.motivoCancelacion = motivoParam;
    }

    // ========================
    // MÉTODOS DE VALIDACIÓN
    // ========================

    /**
     * Verifica si el evento ha alcanzado su aforo máximo.
     *
     * @return true si el evento está lleno, false en caso contrario.
     */
    public Boolean verificarAforo() {
        if (this.aforo == null || this.asistentesConfirmados == null) {
            return false;
        }
        return this.asistentesConfirmados >= this.aforo;
    }

    // ========================
    // MÉTODOS DE ASISTENCIA
    // ========================

    /**
     * Cuenta y devuelve el número total de asistentes confirmados.
     *
     * @return Número de asistentes confirmados.
     */
    public Integer contarAsistentes() {
        return this.asistentesConfirmados != null ? this.asistentesConfirmados : 0;
    }

    // ========================
    // MÉTODOS DE ENLACE VIRTUAL
    // ========================

    /**
     * Genera un enlace virtual único para el evento.
     *
     * @return String con el enlace virtual generado.
     */
    public String generarEnlaceVirtual() {
        if (this.enlaceVirtual == null || this.enlaceVirtual.isEmpty()) {
            this.enlaceVirtual = "https://evento.meet/" + UUID.randomUUID().toString();
        }
        return this.enlaceVirtual;
    }

    // ========================
    // CONVERSIÓN A DTOs
    // ========================

    /**
     * Convierte la entidad al DTO de detalle completo.
     *
     * @return EventDetailResponse con toda la información del evento.
     */
    public EventDetailResponse toDTO() {
        final TipoEvento tipo = this.tipoEvento != null ? this.tipoEvento : TipoEvento.OTRO;

        EventDetailResponse.EventDetailResponseBuilder builder =
                EventDetailResponse.builder()
                        .id(this.id)
                        .titulo(this.titulo)
                        .descripcion(this.descripcion)
                        .fechaHora(this.fechaHora)
                        .fechaFin(this.fechaFin)
                        .aforo(this.aforo)
                        .asistentesConfirmados(this.asistentesConfirmados)
                        .queLlevar(this.queLlevar)
                        .visibleMapa(this.visibleMapa)
                        .esVirtual(this.esVirtual)
                        .enlaceVirtual(this.enlaceVirtual)
                        .cancelado(this.cancelado)
                        .motivoCancelacion(this.motivoCancelacion)
                        .privado(this.privado)
                        .classroomTaskId(this.classroomTaskId)
                        .classroomTaskTitle(this.classroomTaskTitle)
                        .classroomTaskUrl(this.classroomTaskUrl)
                        .tipoEvento(tipo)
                        .iconoEvento(tipo.getIcono())
                        .createdAt(this.createdAt)
                        .comunidadId(this.comunidad != null ? this.comunidad.getId() : null)
                        .comunidadNombre(this.comunidad != null ? this.comunidad.getNombre() : null)
                        .creadorId(this.creador != null ? this.creador.getId() : null);

        if (this.creador != null) {
            builder.creador(
                    UserPublicResponse.builder()
                            .id(this.creador.getId())
                            .nombre(this.creador.getNombre())
                            .foto(this.creador.getFoto())
                            .bio(this.creador.getBio())
                            .intereses(this.creador.getIntereses())
                            .esTutor(this.creador.getEsTutor())
                            .tutorId(
                                    this.creador.getTutor() != null
                                            ? this.creador.getTutor().getId()
                                            : null)
                            .build());
        }

        if (this.ubicacion != null) {
            builder.ubicacion(
                    UbicacionResponse.builder()
                            .id(this.ubicacion.getId())
                            .nombre(this.ubicacion.getNombre())
                            .latitud(this.ubicacion.getLatitud())
                            .longitud(this.ubicacion.getLongitud())
                            .build());
        }

        return builder.build();
    }

    /**
     * Convierte la entidad a un DTO resumen para listados.
     *
     * @return EventSummaryResponse con la información básica del evento.
     */
    public EventSummaryResponse toSummaryDTO() {
        final TipoEvento tipo = this.tipoEvento != null ? this.tipoEvento : TipoEvento.OTRO;

        EventSummaryResponse.EventSummaryResponseBuilder builder =
                EventSummaryResponse.builder()
                        .id(this.id)
                        .titulo(this.titulo)
                        .descripcion(this.descripcion)
                        .fechaHora(this.fechaHora)
                        .aforo(this.aforo)
                        .asistentesConfirmados(this.asistentesConfirmados)
                        .esVirtual(this.esVirtual)
                        .cancelado(this.cancelado)
                        .classroomTaskId(this.classroomTaskId)
                        .classroomTaskTitle(this.classroomTaskTitle)
                        .classroomTaskUrl(this.classroomTaskUrl)
                        .tipoEvento(tipo)
                        .iconoEvento(tipo.getIcono())
                        .comunidadId(this.comunidad != null ? this.comunidad.getId() : null)
                        .comunidadNombre(this.comunidad != null ? this.comunidad.getNombre() : null)
                        .creadorId(this.creador != null ? this.creador.getId() : null);

        if (this.ubicacion != null) {
            builder.ubicacion(
                    UbicacionResponse.builder()
                            .id(this.ubicacion.getId())
                            .nombre(this.ubicacion.getNombre())
                            .direccion(this.ubicacion.getDireccion())
                            .latitud(this.ubicacion.getLatitud())
                            .longitud(this.ubicacion.getLongitud())
                            .tipo(this.ubicacion.getTipo())
                            .coste(this.ubicacion.getCoste())
                            .build());
        } else {
            builder.ubicacion(null);
        }
        return builder.build();
    }
}
