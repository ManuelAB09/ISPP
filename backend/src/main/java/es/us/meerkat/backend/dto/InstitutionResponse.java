package es.us.meerkat.backend.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/** DTO para respuesta de información de institución. */
@Data
@Builder
@Schema(description = "Información de una institución")
public class InstitutionResponse {

    @Schema(description = "ID de la institución", example = "1")
    private Long id;

    @Schema(description = "Nombre de la institución")
    private String nombre;

    @Schema(description = "Descripción de la institución")
    private String descripcion;

    @Schema(description = "Email de contacto")
    private String emailContacto;

    @Schema(description = "Teléfono de contacto")
    private String telefonoContacto;

    @Schema(description = "Dominio de email para validación")
    private String dominioEmail;

    @Schema(description = "Ubicación/ciudad")
    private String ubicacion;

    @Schema(description = "URL del sitio web")
    private String sitioweb;

    @Schema(description = "Logo de la institución")
    private String logoUrl;

    @Schema(description = "Indica si la institución está verificada")
    private Boolean verificada;

    @Schema(description = "Plan corporativo contratado")
    private String planCorporativo;

    @Schema(description = "Indica si el plan corporativo está activo")
    private Boolean planActivo;

    @Schema(description = "Número de usuarios de la institución")
    private Integer totalUsuarios;

    @Schema(description = "Número de comunidades creadas")
    private Integer totalComunidades;

    @Schema(description = "Fecha de creación")
    private LocalDateTime createdAt;

    @Schema(description = "Fecha de última actualización")
    private LocalDateTime updatedAt;
}
