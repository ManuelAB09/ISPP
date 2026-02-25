package es.us.meerkat.backend.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

/**
 * DTO para la creación o edición del perfil de un tutor.
 *
 * <p>Contiene la información básica que un tutor puede enviar para actualizar su perfil:
 * especialidades, tarifa, disponibilidad y bio.
 */
@Data
public class TutorProfileRequest {

    /** Lista de especialidades del tutor. */
    private List<String> especialidades;

    /** Tarifa por hora del tutor. */
    private BigDecimal tarifaHora;

    /** Disponibilidad del tutor en formato texto. */
    private String disponibilidad;

    /** Breve biografía del tutor. */
    private String bio;
}
