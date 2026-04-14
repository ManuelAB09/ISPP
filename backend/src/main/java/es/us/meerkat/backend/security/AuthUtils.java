package es.us.meerkat.backend.security;

import org.springframework.stereotype.Component;

import es.us.meerkat.backend.entity.tutors.Tutor;
import es.us.meerkat.backend.entity.users.Usuario;

/**
 * Clase utilitaria para obtener información del usuario autenticado desde el contexto de seguridad.
 * Utiliza patrones de Spring Security para acceder a los datos del Usuario actual.
 */
@Component
public class AuthUtils {

    /**
     * Obtiene el ID del usuario autenticado.
     *
     * @param usuario El Usuario obtenido del contexto de seguridad
     * @return El ID del usuario
     * @throws IllegalArgumentException si el usuario es nulo
     */
    public Long getUserId(Usuario usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException("Usuario no autenticado");
        }
        return usuario.getId();
    }

    /**
     * Obtiene el ID del tutor asociado al usuario autenticado. Un usuario puede tener múltiples
     * tutores, este método retorna el ID del primer tutor.
     *
     * @param usuario El Usuario obtenido del contexto de seguridad
     * @return El ID del tutor asociado
     * @throws IllegalArgumentException si el usuario es nulo o no tiene tutores
     */
    public Long getTutorId(Usuario usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException("Usuario no autenticado");
        }
        if (usuario.getTutor() == null) {
            throw new IllegalArgumentException("Usuario no tiene rol de tutor asignado");
        }
        Tutor tutor = usuario.getTutor();
        return tutor.getId();
    }

    /**
     * Obtiene el Tutor asociado al usuario autenticado.
     *
     * @param usuario El Usuario obtenido del contexto de seguridad
     * @return El Tutor del usuario
     * @throws IllegalArgumentException si el usuario es nulo o no tiene tutores
     */
    public Tutor getTutor(Usuario usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException("Usuario no autenticado");
        }
        if (usuario.getTutor() == null) {
            throw new IllegalArgumentException("Usuario no tiene rol de tutor asignado");
        }
        return usuario.getTutor();
    }

    /**
     * Valida si el usuario autenticado es tutor.
     *
     * @param usuario El Usuario obtenido del contexto de seguridad
     * @return true si el usuario es tutor, false en caso contrario
     */
    public boolean isTutor(Usuario usuario) {
        if (usuario == null) {
            return false;
        }
        return usuario.getEsTutor() && usuario.getTutor() != null;
    }
}
