package es.us.meerkat.backend.entity;

/**
 * Enum que representa los tipos de transacciones posibles en la plataforma.
 */
public enum TipoTransaccion {

    /** Pago realizado por un tutor para solicitar verificación de su perfil. */
    PAGO_VERIFICACION,

    /** Pago a un tutor por los servicios prestados a los usuarios. */
    PAGO_TUTOR,

    /** Pago de suscripción premium por parte de un usuario. */
    SUSCRIPCION,

    /** Comisiones aplicadas sobre pagos realizados en la plataforma. */
    COMISION;

}
