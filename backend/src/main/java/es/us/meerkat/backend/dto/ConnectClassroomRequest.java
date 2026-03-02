package es.us.meerkat.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

/** DTO para conectar Google Classroom. */
@Data
@Builder
@Schema(description = "Datos para conectar Google Classroom")
public class ConnectClassroomRequest {

    @Schema(
            description = "Token de autenticación de Google",
            example =
                    "eyJhbGciOiJSUzI1NiIsImtpZCI6IjE2ZTYzODY5YWU4YzQ1YzdhODk4Mzg2ZWQwNDZlNTQyYmJl...")
    @NotBlank(message = "El token de Google es requerido")
    private String googleToken;

    @Schema(description = "Email de la cuenta de Google")
    @Email(message = "El email debe ser válido")
    private String googleEmail;
}
