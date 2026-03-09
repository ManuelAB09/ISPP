package es.us.meerkat.backend.dto;

import java.math.BigDecimal;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/** DTO para respuesta de información de tutor. */
@Data
@Builder
@Schema(description = "Información de perfil de tutor")
public class TutorResponse {

    @Schema(description = "ID del tutor", example = "1")
    private Long id;

    @Schema(description = "Datos del usuario propietario del perfil")
    private UsuarioDto usuario;

    @Schema(description = "Biografía del tutor")
    private String biografia;

    @Schema(description = "Disponibilidad horaria del tutor")
    private String disponibilidad;

    @Schema(description = "Tarifa por hora")
    private BigDecimal tarifaPorHora;

    @Schema(description = "Especialidades del tutor")
    private List<String> especialidades;

    @Schema(description = "Indica si el tutor está verificado")
    private Boolean verificado;

    @Schema(description = "Indica si classroom está conectado")
    private Boolean classroomConectado;

    @Schema(description = "Fecha de creación del perfil")
    private String createdAt;

    @Schema(description = "Ubicación del tutor")
    private UbicacionResponse ubicacion;

    @Data
    @Builder
    public static class UsuarioDto {
        private Long id;
        private String nombre;
        private String foto;
    }
}
