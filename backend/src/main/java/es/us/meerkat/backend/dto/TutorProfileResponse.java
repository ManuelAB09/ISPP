package es.us.meerkat.backend.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;
import lombok.Data;

/**
 * DTO que representa la información pública o completa de un tutor.
 *
 * Contiene datos como identificador, nombre, especialidades, tarifa,
 * disponibilidad, biografía y estado de verificación.
 */
@Data
@Builder
public class TutorProfileResponse {

    /** Identificador único del tutor. */
    private Long id;

    /** Nombre completo del tutor. */
    private String nombre;

    /** Lista de especialidades del tutor. */
    private List<String> especialidades;

    /** Tarifa por hora del tutor. */
    private BigDecimal tarifaHora;

    /** Disponibilidad del tutor en formato texto. */
    private String disponibilidad;

    /** Breve biografía del tutor. */
    private String bio;

    /** Estado de verificación del tutor (true si verificado). */
    private Boolean verificado;

}
