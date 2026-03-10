package es.us.meerkat.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MisContratacionesResponse {

    private Long id;
    private String estado;
    private String modalidad;
    private String duracion;
    private BigDecimal tarifaAcordada;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String motivoCancelacion;
    private String paymentUrl;
    private String stripeSessionId;

    private TutorDto tutor;
    private ComunidadDto comunidad;

    @Data
    @Builder
    public static class TutorDto {
        private Long id;
        private UsuarioDto usuario;
    }

    @Data
    @Builder
    public static class UsuarioDto {
        private Long id;
        private String nombre;
        private String foto;
    }

    @Data
    @Builder
    public static class ComunidadDto {
        private Long id;
        private String nombre;
        private String descripcion;
        private String imagenUrl;
    }
}
