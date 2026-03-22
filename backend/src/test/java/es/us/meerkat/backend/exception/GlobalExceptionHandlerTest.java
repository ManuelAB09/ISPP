package es.us.meerkat.backend.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpServletRequest;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private HttpServletRequest mockRequest(String uri) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(uri);
        return request;
    }

    @Test
    void handleValidationExceptionShouldReturn400() {
        HttpServletRequest request = mockRequest("/api/v1/test");
        ValidationException ex = new ValidationException("Campo inválido");

        ResponseEntity<ErrorResponse> response = handler.handleValidationException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).isEqualTo("Campo inválido");
        assertThat(response.getBody().getPath()).isEqualTo("/api/v1/test");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void handleConflictExceptionShouldReturn409() {
        HttpServletRequest request = mockRequest("/api/v1/test");
        ConflictException ex = new ConflictException("Recurso ya existe");

        ResponseEntity<ErrorResponse> response = handler.handleConflictException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getMessage()).isEqualTo("Recurso ya existe");
    }

    @Test
    void handleEmailNotVerifiedExceptionShouldReturn403() {
        HttpServletRequest request = mockRequest("/api/v1/auth/login");
        EmailNotVerifiedException ex = new EmailNotVerifiedException("Debes verificar tu email");

        ResponseEntity<ErrorResponse> response =
                handler.handleEmailNotVerifiedException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(403);
        assertThat(response.getBody().getMessage()).isEqualTo("Debes verificar tu email");
    }

    @Test
    void handleGenericExceptionShouldReturn500() {
        HttpServletRequest request = mockRequest("/api/v1/test");
        Exception ex = new RuntimeException("Algo salió mal");

        ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getMessage()).isEqualTo("Error interno del servidor");
    }

    @Test
    void errorResponseShouldHaveTimestampSet() {
        HttpServletRequest request = mockRequest("/api/v1/test");
        ValidationException ex = new ValidationException("test");

        ResponseEntity<ErrorResponse> response = handler.handleValidationException(ex, request);

        assertThat(response.getBody().getTimestamp()).isNotNull();
    }
}
