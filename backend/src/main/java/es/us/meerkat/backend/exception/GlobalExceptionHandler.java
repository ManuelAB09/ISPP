package es.us.meerkat.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Manejador global de excepciones para toda la aplicación.
 *
 * <p>Centraliza el manejo de excepciones y proporciona respuestas HTTP coherentes según el tipo de
 * error. Excluye rutas de Swagger para evitar interferencias con la documentación automática.
 */
@RestControllerAdvice()
@Hidden
public class GlobalExceptionHandler {

    /**
     * Maneja excepciones de validación (400 Bad Request).
     *
     * @param ex Excepción de validación.
     * @param request Solicitud HTTP que causó el error.
     * @return ResponseEntity con estado 400 y detalles del error.
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            final ValidationException ex, final HttpServletRequest request) {
        final ErrorResponse errorResponse =
                new ErrorResponse(
                        HttpStatus.BAD_REQUEST.value(), ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Maneja excepciones de conflicto (409 Conflict).
     *
     * @param ex Excepción de conflicto.
     * @param request Solicitud HTTP que causó el error.
     * @return ResponseEntity con estado 409 y detalles del error.
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflictException(
            final ConflictException ex, final HttpServletRequest request) {
        final ErrorResponse errorResponse =
                new ErrorResponse(
                        HttpStatus.CONFLICT.value(), ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Maneja excepciones genéricas no capturadas (500 Internal Server Error).
     *
     * <p>NOTA: Aunque esté aquí, no afectará a las rutas de Swagger porque el @RestControllerAdvice
     * está limitado a basePackages = "es.us.meerkat.backend.controller"
     *
     * @param ex Excepción genérica.
     * @param request Solicitud HTTP que causó el error.
     * @return ResponseEntity con estado 500 y detalles del error.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            final Exception ex, final HttpServletRequest request) {
        final ErrorResponse errorResponse =
                new ErrorResponse(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Error interno del servidor",
                        request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
