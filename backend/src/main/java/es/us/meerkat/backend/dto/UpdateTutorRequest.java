package es.us.meerkat.backend.dto;

import java.math.BigDecimal;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

/** DTO para actualizar un perfil de tutor. */
@Data
@Builder
@Schema(description = "Datos para actualizar un perfil de tutor")
public class UpdateTutorRequest {

    @Schema(
            description = "Biografía del tutor",
            example = "Profesor con 5 años de experiencia en matemáticas")
    @Size(max = 1000, message = "La biografía no puede exceder 1000 caracteres")
    private String biografia;

    @Schema(description = "Tarifa por hora", example = "25.50")
    @DecimalMin(value = "0.01", message = "La tarifa debe ser mayor a 0")
    private BigDecimal tarifaPorHora;

    @Schema(description = "Especialidades del tutor", example = "[\"Matemáticas\", \"Física\"]")
    @NotEmpty(message = "Debe tener al menos una especialidad")
    private List<String> especialidades;

    @Schema(description = "URL de experiencia o certificados")
    private String urlExperiencia;

    @Schema(description = "Teléfono de contacto", example = "+34612345678")
    @Pattern(regexp = "^\\+?[0-9]{10,}$", message = "Teléfono inválido")
    private String telefonoContacto;

    @Schema(description = "Biografia corta para listados")
    @Size(max = 200, message = "La biografia corta no puede exceder 200 caracteres")
    private String bioCorta;
}
