package es.us.meerkat.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO de respuesta con las preferencias de notificación del usuario. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PreferenciasNotificacionResponse {

    /** Si los emails de recordatorio están activados en general. */
    private Boolean emailsActivados;

    /** Si recibe recordatorio 24h antes. */
    private Boolean recordatorio24h;

    /** Si recibe recordatorio 1h antes. */
    private Boolean recordatorio1h;

    /** Si recibe recordatorio 30min antes. */
    private Boolean recordatorio30min;
}
