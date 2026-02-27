package es.us.meerkat.backend.entity;

import java.time.LocalDateTime;

import es.us.meerkat.backend.dto.AttendanceResponse;
import es.us.meerkat.backend.dto.UserPublicResponse;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad que representa la asistencia a un evento de estudio en la plataforma.
 *
 * <p>Contiene información sobre el estado de la asistencia y la fecha de esta.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsistenciaEvento {

    /** Identificador único de la asistencia a un evento. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Estado en el que se encuentra la asistencia al evento. */
    private EstadoAsistencia estado;

    /** Fecha y hora de la asistencia al evento. */
    private LocalDateTime createdAt;

    /** Evento al que se asiste. */
    @ManyToOne
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    /** Usuario que asiste a un evento. */
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // ========================
    // MÉTODOS DE ASISTENCIA
    // ========================

    /** Confirma la asistencia a un evento. */
    public void confirmarAsistencia() {
        this.estado = EstadoAsistencia.CONFIRMADA;
    }

    /** Cancela la asistencia a un evento. */
    public void cancelarAsistencia() {
        this.estado = EstadoAsistencia.CANCELADA;
    }

    public AttendanceResponse toDTO() {
        UserPublicResponse userDto = null;
        if (this.usuario != null) {
            userDto = es.us.meerkat.backend.dto.UserPublicResponse.builder()
                    .id(this.usuario.getId())
                    .nombre(this.usuario.getNombre())
                    .foto(this.usuario.getFoto())
                    .bio(this.usuario.getBio())
                    .intereses(this.usuario.getIntereses())
                    .esTutor(this.usuario.getEsTutor())
                    .build();
        }
        return AttendanceResponse.builder()
                .id(this.id)
                .usuario(userDto)
                .estado(this.estado)
                .createdAt(this.createdAt)
                .build();
    }
}
