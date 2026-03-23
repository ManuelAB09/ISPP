package es.us.meerkat.backend.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

import es.us.meerkat.backend.entity.TipoPlan;
import lombok.Builder;
import lombok.Data;

/**
 * DTO que representa la respuesta de una suscripción de usuario.
 *
 * <p>Contiene información sobre el plan activo, fechas de inicio y fin, estado de renovación
 * automática y período de gracia tras cancelación.
 */
@Data
@Builder
public class SubscriptionResponse {

    /** Identificador único de la suscripción. */
    private Long id;

    /** Tipo de plan de la suscripción. */
    private TipoPlan plan;

    /** Periodo de la suscripción (MENSUAL o ANUAL). */
    private String periodo;

    /** Fecha de inicio de la suscripción. */
    @JsonProperty("fechaInicio")
    private LocalDate fechaInicio;

    /** Fecha de finalización de la suscripción. */
    @JsonProperty("fechaFin")
    private LocalDate fechaFin;

    /** Indica si la suscripción está activa. */
    private Boolean activa;

    /** Indica si la suscripción se renueva automáticamente. */
    @JsonProperty("autoRenovar")
    private Boolean autoRenovar;

    /** Indica si está en período de gracia tras cancelación. */
    @JsonProperty("enPeriodoGracia")
    private Boolean enPeriodoGracia;

    /** Nombre de la institución del usuario (si tiene). */
    private String institutionNombre;

    /** Identificador de la institución del usuario (si tiene). */
    private Long institutionId;

    /** Plan corporativo de la institución (si tiene). */
    private String planCorporativo;

    /** Indica si el plan corporativo de la institución está activo. */
    private Boolean planCorporativoActivo;
}
