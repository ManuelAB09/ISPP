package es.us.meerkat.backend.entity.suscriptions;

/** Enum que representa los posibles estados de una transacción. */
public enum EstadoTransaccion {

    /** La transacción está pendiente y aún no se ha completado el pago. */
    PENDIENTE,

    /** La transacción se completó correctamente y el pago fue confirmado. */
    COMPLETADA,

    /** La transacción falló o el pago fue rechazado. */
    FALLIDA,

    /** La transacción fue reembolsada. */
    REEMBOLSADA;
}
