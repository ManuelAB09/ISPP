package es.us.meerkat.backend.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import es.us.meerkat.backend.dto.EventDetailResponse;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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

    /** Indica si se debe enviar notificación en caso de cancelación. */
    private String motivoCancelacion;

    /** Indica si el evento es privado. */
    private Boolean privado;

    /** Ubicación asociada al evento (opcional). */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "ubicacion_id", nullable = true)
    private Ubicacion ubicacion;

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
    }

    // ========================
    // MÉTODOS DE GESTIÓN
    // ========================

    /**
     * Crea un nuevo evento con la información proporcionada.
     *
     * @param tituloParam Título del evento.
     * @param descripcionParam Descripción del evento.
     * @param fechaInicioParam Fecha y hora de inicio.
     * @param fechaFinParam Fecha y hora de fin.
     * @param aforoParam Aforo máximo.
     * @param queLlevarParam Qué llevar al evento.
     * @param esVirtualParam Si es evento virtual.
     * @param privadoParam Si es un evento privado.
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
    }

    /**
     * Edita la información del evento existente.
     *
     * @param tituloParam Título del evento.
     * @param descripcionParam Descripción del evento.
     * @param fechaInicioParam Fecha y hora de inicio.
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
    public String generarEnlaceVirtual() { // TODO: Cambiar cuando se tenga una url definitiva
        if (this.enlaceVirtual == null || this.enlaceVirtual.isEmpty()) {
            this.enlaceVirtual = "https://evento.meet/" + UUID.randomUUID().toString();
        }
        return this.enlaceVirtual;
    }

    public EventDetailResponse toDTO() {
        return EventDetailResponse.builder()
                .id(this.id)
                .titulo(this.titulo)
                .descripcion(this.descripcion)
                .fechaHora(this.fechaHora)
                .fechaFin(this.fechaFin)
                .aforo(this.aforo)
                .asistentesConfirmados(this.asistentesConfirmados)
                .queLlevar(this.queLlevar)
                .visibleEnMapa(this.visibleMapa)
                .esVirtual(this.esVirtual)
                .enlaceVirtual(this.enlaceVirtual)
                .cancelado(this.cancelado)
                .motivoCancelacion(this.motivoCancelacion)
                // .comunidad(this.comunidad)
                // .creador(this.creador)
                // .miAsistencia()
                .createdAt(this.createdAt)
                .build();
    }
}
