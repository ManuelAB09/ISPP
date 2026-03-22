package es.us.meerkat.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad que representa a un tutor dentro de la plataforma.
 *
 * <p>Contiene información de perfil, estado de verificación, conexión a classroom, historial de
 * transacciones y especialidades.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tutor {

    /** Identificador único del tutor. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nivel de desempeño del tutor. */
    @Column(name = "nivel_desempeno", nullable = false)
    private String nivelDesempeno = "PRINCIPIANTE";

    /** Puntuación media del tutor. */
    @Column(name = "puntuacion_media", nullable = false)
    private Double puntuacionMedia = 0.0;

    /** Total de valoraciones del tutor. */
    @Column(name = "total_valoraciones", nullable = false)
    private Integer totalValoraciones = 0;

    /** Usuario asociado al tutor. */
    @OneToOne
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    /** Lista de especialidades del tutor. */
    @ElementCollection
    @CollectionTable(name = "esp", joinColumns = @JoinColumn(name = "t_id"))
    @Column(name = "especialidad")
    private List<String> especialidades = new ArrayList<>();

    /** Tarifa por hora del tutor. */
    private BigDecimal tarifaHora;

    /** Disponibilidad del tutor en formato texto. */
    private String disponibilidad;

    /** Breve biografía del tutor. */
    private String bio;

    /** Ubicación del tutor (opcional). */
    @ManyToOne
    @JoinColumn(name = "ubicacion_id", nullable = true)
    private Ubicacion ubicacion;

    /** Estado de verificación del tutor. */
    private Boolean verificado = false;

    /** Indica si el tutor está conectado a Classroom. */
    private Boolean classroomConectado = false;

    /** Email de Google Classroom del tutor. */
    private String emailClassroom;

    /** ID de la cuenta conectada de Stripe (Express) para recibir pagos. */
    private String stripeAccountId;

    /** Fecha de creación del registro. */
    private LocalDateTime createdAt;

    /** Transacciones asociadas al tutor. */
    @OneToMany(mappedBy = "tutor")
    private List<TransaccionPago> transacciones = new ArrayList<>();

    /** Inicializa valores antes de persistir la entidad. */
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.verificado == null) {
            this.verificado = false;
        }
        if (this.classroomConectado == null) {
            this.classroomConectado = false;
        }
        if (this.nivelDesempeno == null) {
            this.nivelDesempeno = "PRINCIPIANTE";
        }
        if (this.puntuacionMedia == null) {
            this.puntuacionMedia = 0.0;
        }
        if (this.totalValoraciones == null) {
            this.totalValoraciones = 0;
        }
    }

    // ========================
    // MÉTODOS DE PERFIL
    // ========================

    /**
     * Crea o establece el perfil del tutor.
     *
     * @param tarifaHoraParam Tarifa por hora.
     * @param disponibilidadParam Disponibilidad en texto.
     * @param bioParam Biografía del tutor.
     * @param especialidadesParam Lista de especialidades.
     */
    public void crearPerfil(
            final BigDecimal tarifaHoraParam,
            final String disponibilidadParam,
            final String bioParam,
            final List<String> especialidadesParam) {
        this.tarifaHora = tarifaHoraParam;
        this.disponibilidad = disponibilidadParam;
        this.bio = bioParam;
        this.especialidades = especialidadesParam;
    }

    /**
     * Edita el perfil existente del tutor.
     *
     * @param tarifaHoraParam Tarifa por hora.
     * @param disponibilidadParam Disponibilidad en texto.
     * @param bioParam Biografía del tutor.
     * @param especialidadesParam Lista de especialidades.
     */
    public void editarPerfil(
            final BigDecimal tarifaHoraParam,
            final String disponibilidadParam,
            final String bioParam,
            final List<String> especialidadesParam) {
        this.tarifaHora = tarifaHoraParam;
        this.disponibilidad = disponibilidadParam;
        this.bio = bioParam;
        this.especialidades = especialidadesParam;
    }

    // ========================
    // MÉTODOS DE VERIFICACIÓN
    // ========================

    /** Inicia el proceso de verificación del tutor. */
    public void solicitarVerificacion() {
        this.verificado = false;
    }

    // ========================
    // MÉTODOS DE CLASSROOM
    // ========================

    /** Marca al tutor como conectado a Classroom. */
    public void conectarClassroom() {
        this.classroomConectado = true;
    }

    /** Marca al tutor como desconectado de Classroom. */
    public void desconectarClassroom() {
        this.classroomConectado = false;
    }

    // ========================
    // MÉTODOS DE LISTADO
    // ========================

    /**
     * Filtra y devuelve solo los tutores verificados de una lista dada.
     *
     * @param todosParam Lista completa de tutores.
     * @return Lista de tutores verificados.
     */
    public static List<Tutor> mostrarEnListado(final List<Tutor> todosParam) {
        final List<Tutor> verificados = new ArrayList<>();
        for (final Tutor t : todosParam) {
            if (Boolean.TRUE.equals(t.getVerificado())) {
                verificados.add(t);
            }
        }
        return verificados;
    }
}
