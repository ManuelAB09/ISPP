package es.us.meerkat.backend.dto.communities;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO para actualizar datos de una institución. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos para actualizar una institución")
public class UpdateInstitutionRequest {

    @Schema(description = "Nombre de la institución")
    @Size(min = 3, max = 200, message = "El nombre debe tener entre 3 y 200 caracteres")
    private String nombre;

    @Schema(description = "Descripción de la institución")
    @Size(max = 1000, message = "La descripción no puede exceder 1000 caracteres")
    private String descripcion;

    @Schema(description = "Email de contacto")
    @Email(message = "El email es inválido")
    private String emailContacto;

    @Schema(description = "Teléfono de contacto")
    @Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
    private String telefonoContacto;

    @Schema(description = "Ubicación/ciudad")
    private String ubicacion;

    @Schema(description = "URL del sitio web")
    private String sitioweb;

    @Schema(description = "Logo de la institución (URL)")
    private String logoUrl;
}
