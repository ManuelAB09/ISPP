package es.us.meerkat.backend.exception;

/**
 * Excepción para errores de validación de datos. Se lanza cuando hay un problema con los datos
 * proporcionados (validación fallida).
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
