package es.us.meerkat.backend.entity;

/**
 * Enumeración que define los tipos de planes disponibles para una comunidad.
 *
 * <p>Tipos:
 *
 * <ul>
 *   <li><b>FREE</b>: Plan gratuito con límite de miembros
 *   <li><b>PREMIUM</b>: Plan premium con mayor límite de miembros
 *   <li><b>UNLIMITED</b>: Plan ilimitado para comunidades institucionales
 * </ul>
 */
public enum TipoPlanComunidad {
    /** Plan gratuito con límites estándar. */
    FREE,
    /** Plan premium con límites ampliados. */
    PREMIUM,
    /** Plan ilimitado para instituciones educativas. */
    UNLIMITED
}
