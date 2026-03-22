package es.us.meerkat.backend.entity;

/** Estados posibles de una reserva de clase/servicio. */
public enum EstadoReserva {
    /** Reserva recién creada, pendiente de confirmación */
    PENDIENTE,
    /** Reserva confirmada por el tutor */
    CONFIRMADA,
    /** Reserva completada (ya pasó la clase) */
    COMPLETADA,
    /** Reserva cancelada por el alumno */
    CANCELADA_ALUMNO,
    /** Reserva cancelada por el tutor */
    CANCELADA_TUTOR,
    /** Reserva no asistida */
    NO_ASISTIDA
}
