package es.us.meerkat.backend.dto;

import es.us.meerkat.backend.entity.TipoCanal;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** DTO para crear una alarma personalizada en un evento concreto. */
@Data
public class CrearAlarmaRequest {

    /**
     * Minutos de antelación antes del inicio del evento. Valores sugeridos: 2880 (2 días), 1440 (1
     * día), 120 (2h), 60 (1h), 30 (30min). El frontend puede enviar cualquier valor positivo.
     */
    @NotNull(message = "minutosAntes es obligatorio")
    @Min(value = 1, message = "minutosAntes debe ser mayor que 0")
    private Integer minutosAntes;

    /**
     * Canal de notificación: PLATAFORMA, EMAIL o AMBOS. Si no se envía, se usa el canal guardado en
     * las preferencias del usuario.
     */
    private TipoCanal canal;
}
