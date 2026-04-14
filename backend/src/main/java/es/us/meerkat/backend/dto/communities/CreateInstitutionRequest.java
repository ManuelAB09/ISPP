package es.us.meerkat.backend.dto.communities;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO para registrar una institución/academia. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos para registrar una institución")
public class CreateInstitutionRequest {

    @Schema(description = "Nombre de la institución", example = "Universidad de Sevilla")
    @NotBlank(message = "El nombre es requerido")
    @Size(min = 3, max = 200, message = "El nombre debe tener entre 3 y 200 caracteres")
    private String nombre;

    @Schema(
            description = "Descripción de la institución",
            example = "Universidad pública de educación superior")
    @Size(max = 1000, message = "La descripción no puede exceder 1000 caracteres")
    private String descripcion;

    @Schema(description = "Email de contacto", example = "contacto@universidad.es")
    @NotBlank(message = "El email es requerido")
    @Email(message = "El email es inválido")
    private String emailContacto;

    @Schema(description = "Teléfono de contacto", example = "+34912345678")
    @Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
    private String telefonoContacto;

    @Schema(
            description = "Dominio de email de la institución para validación",
            example = "universidad.es")
    @NotBlank(message = "El dominio de email es requerido")
    private String dominioEmail;

    @Schema(description = "Ubicación/ciudad", example = "Sevilla")
    private String ubicacion;

    @Schema(description = "URL del sitio web", example = "https://www.universidad.es")
    private String sitioweb;

    @Schema(
            description = "Logo de la institución (URL)",
            example = "https://www.universidad.es/logo.png")
    private String logoUrl;
}
