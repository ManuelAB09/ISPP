package es.us.meerkat.backend.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Manejador global de excepciones para toda la aplicación.
 *
 * <p>Centraliza el manejo de excepciones y proporciona respuestas HTTP coherentes según el tipo de
 * error. Excluye rutas de Swagger para evitar interferencias con la documentación automática.
 */
@Slf4j
@RestControllerAdvice()
@Hidden
public class GlobalExceptionHandler {

    /**
     * Maneja errores de validación de DTOs anotados con @Valid (400 Bad Request).
     *
     * @param ex Excepción con los errores de validación por campo.
     * @param request Solicitud HTTP que causó el error.
     * @return ResponseEntity con estado 400 y detalle de campos inválidos.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            final MethodArgumentNotValidException ex, final HttpServletRequest request) {
        final Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult()
                .getFieldErrors()
                .forEach(
                        error ->
                                fieldErrors.putIfAbsent(
                                        error.getField(), error.getDefaultMessage()));

        final String validationDetails =
                fieldErrors.values().stream().collect(Collectors.joining("; "));

        final String message =
                validationDetails.isBlank()
                        ? "Datos de entrada inválidos"
                        : "Datos de entrada inválidos: " + validationDetails;

        final ErrorResponse errorResponse =
                new ErrorResponse(HttpStatus.BAD_REQUEST.value(), message, request.getRequestURI());
        errorResponse.setErrors(fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

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
     * Maneja excepciones de argumento inválido (400 Bad Request).
     *
     * @param ex Excepción de argumento inválido.
     * @param request Solicitud HTTP que causó el error.
     * @return ResponseEntity con estado 400 y detalles del error.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            final IllegalArgumentException ex, final HttpServletRequest request) {
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
     * Maneja excepciones de email no verificado (403 Forbidden).
     *
     * @param ex Excepción de email no verificado.
     * @param request Solicitud HTTP que causó el error.
     * @return ResponseEntity con estado 403 y detalles del error.
     */
    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<ErrorResponse> handleEmailNotVerifiedException(
            final EmailNotVerifiedException ex, final HttpServletRequest request) {
        final ErrorResponse errorResponse =
                new ErrorResponse(
                        HttpStatus.FORBIDDEN.value(), ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * Maneja conflictos de integridad en base de datos (409 Conflict).
     *
     * @param ex Excepción de integridad de datos.
     * @param request Solicitud HTTP que causó el error.
     * @return ResponseEntity con estado 409 y mensaje legible.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
            final DataIntegrityViolationException ex, final HttpServletRequest request) {
        log.warn(
                "Conflicto de integridad en {} {}: {}",
                request.getMethod(),
                request.getRequestURI(),
                ex.getMostSpecificCause().getMessage());
        final String message =
                request.getRequestURI().contains("/disponibilidades")
                        ? "Conflicto de datos al guardar. Revisa que la franja no se solape con"
                                + " otra existente."
                        : "Conflicto de datos: existen relaciones asociadas que impiden completar"
                                + " la operacion.";
        final ErrorResponse errorResponse =
                new ErrorResponse(HttpStatus.CONFLICT.value(), message, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Maneja ResponseStatusException reenviando el código HTTP y el mensaje indicados (e.g. 403,
     * 404, 409).
     *
     * @param ex Excepción con el estado HTTP y el motivo.
     * @param request Solicitud HTTP que causó el error.
     * @return ResponseEntity con el estado y el mensaje de la excepción.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(
            final ResponseStatusException ex, final HttpServletRequest request) {
        final ErrorResponse errorResponse =
                new ErrorResponse(
                        ex.getStatusCode().value(), ex.getReason(), request.getRequestURI());
        return ResponseEntity.status(ex.getStatusCode()).body(errorResponse);
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
        log.error(
                "Error no controlado en {} {}: {}",
                request.getMethod(),
                request.getRequestURI(),
                ex.getMessage(),
                ex);
        final ErrorResponse errorResponse =
                new ErrorResponse(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Error interno del servidor",
                        request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
