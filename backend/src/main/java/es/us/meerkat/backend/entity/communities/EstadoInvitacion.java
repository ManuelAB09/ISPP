package es.us.meerkat.backend.entity.communities;

/** Enumeración que define los estados posibles de una invitación a una comunidad. */
public enum EstadoInvitacion {
    /** Invitación pendiente de aceptación. */
    PENDIENTE,
    /** Invitación aceptada por el usuario. */
    ACEPTADA,
    /** Invitación rechazada por el usuario. */
    RECHAZADA,
    /** Invitación expirada (no fue aceptada a tiempo). */
    EXPIRADA
}
