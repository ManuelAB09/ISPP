package es.us.meerkat.backend.exception;

/**
 * Excepción para errores de conflicto de recursos. Se lanza cuando hay un conflicto en los datos
 * (ej: email ya registrado).
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
