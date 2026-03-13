package es.us.meerkat.backend.exception;

/**
 * Excepción para indicar que el email del usuario no ha sido verificado. Se lanza cuando un usuario
 * intenta iniciar sesión sin haber verificado su email.
 */
public class EmailNotVerifiedException extends RuntimeException {

    public EmailNotVerifiedException(String message) {
        super(message);
    }

    public EmailNotVerifiedException(String message, Throwable cause) {
        super(message, cause);
    }
}
