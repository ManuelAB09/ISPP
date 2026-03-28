package es.us.meerkat.backend.dto.events;

import java.time.LocalDateTime;

import es.us.meerkat.backend.dto.users.UserPublicResponse;
import es.us.meerkat.backend.entity.EstadoAsistencia;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO con los detalles de la asistencia de un usuario a un evento.
 *
 * <p>Contiene información sobre si un usuario ha confirmado o cancelado su asistencia a un evento
 * específico.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceResponse {

    /** Identificador de la asistencia. */
    private Long id;

    /** Usuario que asiste al evento. */
    private UserPublicResponse usuario;

    /** Estado de la asistencia (CONFIRMADA o CANCELADA). */
    private EstadoAsistencia estado;

    /** Fecha de creación de la asistencia. */
    private LocalDateTime createdAt;
}
