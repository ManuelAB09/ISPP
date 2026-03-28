package es.us.meerkat.backend.entity.communities;

/**
 * Enumeración que define los roles que puede tener un usuario dentro de una comunidad.
 *
 * <p>Roles:
 *
 * <ul>
 *   <li><b>ADMIN</b>: Administrador de la comunidad con permisos totales
 *   <li><b>PROFESOR</b>: Profesor/mentor con permisos moderados
 *   <li><b>ALUMNO</b>: Estudiante miembro regular con permisos limitados
 * </ul>
 */
public enum RolComunidad {
    /** Administrador de comunidad - permisos totales */
    ADMIN,
    /** Profesor/mentor - permisos moderados */
    PROFESOR,
    /** Estudiante - permisos limitados */
    ALUMNO
}
