package es.us.meerkat.backend.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;
import lombok.Data;

/**
 * DTO que representa la información completa de un tutor, incluyendo datos del usuario asociado.
 */
@Data
@Builder
public class TutorProfileResponse {

    /** Identificador único del tutor. */
    private Long id;

    /** Id del usuario asociado al tutor. */
    private Long userId;

    /** Información del usuario. */
    private UsuarioDto usuario;

    /** Lista de especialidades del tutor. */
    private List<String> especialidades;

    /** Tarifa por hora del tutor. */
    private BigDecimal tarifaHora;

    /** Disponibilidad del tutor en formato texto. */
    private String disponibilidad;

    /** Breve biografía del tutor. */
    private String bio;

    /** Estado de verificación del tutor. */
    private Boolean verificado;

    /** Indica si está conectado a Classroom. */
    private Boolean classroomConectado;

    /** Fecha de creación del tutor. */
    private String createdAt;

    /** DTO interno para representar al usuario. */
    @Data
    @Builder
    public static class UsuarioDto {
        private Long id;
        private String nombre;
        private String foto;
        private String bio;
        private List<String> intereses;
        private Boolean esTutor;
    }
}
