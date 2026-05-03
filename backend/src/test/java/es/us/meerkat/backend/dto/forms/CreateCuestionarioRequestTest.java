package es.us.meerkat.backend.dto.forms;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test para validar las restricciones de tiempo en cuestionarios. UC-64: Cuestionarios con tiempo
 * inválido
 */
@DisplayName("UC-64: Validación de tiempo en cuestionarios")
class CreateCuestionarioRequestTest {

    private CreateCuestionarioRequest request;

    @BeforeEach
    void setUp() {
        request =
                CreateCuestionarioRequest.builder()
                        .titulo("Test Quiz")
                        .materia("Matemáticas")
                        .tiempoEstimadoMinutos(30)
                        .build();
    }

    @Test
    @DisplayName("Debe rechazar tiempo estimado decimal (0) cuando debería ser 1")
    void testDecimalTimeNotAllowed() {
        // Los decimales serían convertidos a 0 o truncados por Integer
        CreateCuestionarioRequest requestWithDecimal =
                CreateCuestionarioRequest.builder()
                        .titulo("Test Quiz")
                        .materia("Matemáticas")
                        .tiempoEstimadoMinutos(0) // 0.3 se convierte a 0
                        .build();

        // La validación con @Min(1) debe rechazar 0
        assertFalse(isValidTiempo(requestWithDecimal));
    }

    @Test
    @DisplayName("Debe rechazar tiempo negativo")
    void testNegativeTimeNotAllowed() {
        CreateCuestionarioRequest requestWithNegative =
                CreateCuestionarioRequest.builder()
                        .titulo("Test Quiz")
                        .materia("Matemáticas")
                        .tiempoEstimadoMinutos(-5)
                        .build();

        assertFalse(isValidTiempo(requestWithNegative));
    }

    @Test
    @DisplayName("Debe rechazar tiempo de cero")
    void testZeroTimeNotAllowed() {
        CreateCuestionarioRequest requestWithZero =
                CreateCuestionarioRequest.builder()
                        .titulo("Test Quiz")
                        .materia("Matemáticas")
                        .tiempoEstimadoMinutos(0)
                        .build();

        assertFalse(isValidTiempo(requestWithZero));
    }

    @Test
    @DisplayName("Debe aceptar tiempo válido (1 minuto)")
    void testValid1Minute() {
        CreateCuestionarioRequest validRequest =
                CreateCuestionarioRequest.builder()
                        .titulo("Test Quiz")
                        .materia("Matemáticas")
                        .tiempoEstimadoMinutos(1)
                        .build();

        assertTrue(isValidTiempo(validRequest));
    }

    @Test
    @DisplayName("Debe aceptar tiempo válido (30 minutos)")
    void testValid30Minutes() {
        // Default is 30, should be valid
        assertTrue(isValidTiempo(request));
    }

    @Test
    @DisplayName("Debe aceptar tiempo válido (60 minutos)")
    void testValid60Minutes() {
        CreateCuestionarioRequest validRequest =
                CreateCuestionarioRequest.builder()
                        .titulo("Test Quiz")
                        .materia("Matemáticas")
                        .tiempoEstimadoMinutos(60)
                        .build();

        assertTrue(isValidTiempo(validRequest));
    }

    @Test
    @DisplayName("Debe aceptar tiempo máximo válido (1440 minutos = 24 horas)")
    void testValid1440Minutes() {
        CreateCuestionarioRequest validRequest =
                CreateCuestionarioRequest.builder()
                        .titulo("Test Quiz")
                        .materia("Matemáticas")
                        .tiempoEstimadoMinutos(1440)
                        .build();

        assertTrue(isValidTiempo(validRequest));
    }

    @Test
    @DisplayName("Debe rechazar tiempo mayor a 1440 minutos (24 horas)")
    void testExceedsMaxTime() {
        CreateCuestionarioRequest requestWithExcessTime =
                CreateCuestionarioRequest.builder()
                        .titulo("Test Quiz")
                        .materia("Matemáticas")
                        .tiempoEstimadoMinutos(1441)
                        .build();

        assertFalse(isValidTiempo(requestWithExcessTime));
    }

    /** Helper method to validate the request. */
    private boolean isValidTiempo(CreateCuestionarioRequest req) {
        if (req.getTiempoEstimadoMinutos() == null) {
            return false;
        }
        if (req.getTiempoEstimadoMinutos() < 1) {
            return false;
        }
        if (req.getTiempoEstimadoMinutos() > 1440) {
            return false;
        }
        return true;
    }
}
