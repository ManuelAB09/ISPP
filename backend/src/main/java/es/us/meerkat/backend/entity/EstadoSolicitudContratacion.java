package es.us.meerkat.backend.entity;

/** Enum para estados de una solicitud de contratación directa de tutor. */
public enum EstadoSolicitudContratacion {
    PENDIENTE,
    ACEPTADA,
    RECHAZADA,
    PAGADA,
    CANCELADA_ALUMNO,
    CANCELADA_TUTOR,
    COMPLETADA,
    NO_ASISTIDA,
    REPROGRAMACION_PENDIENTE
}
