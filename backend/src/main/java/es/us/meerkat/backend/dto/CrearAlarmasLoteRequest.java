package es.us.meerkat.backend.dto;

import java.util.List;

import es.us.meerkat.backend.entity.TipoCanal;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO para crear múltiples alarmas para un evento en una sola llamada.
 *
 * <p>Se usa principalmente al confirmar asistencia, cuando el usuario marca varios checkboxes de
 * antelación a la vez.
 */
@Data
public class CrearAlarmasLoteRequest {

    /**
     * Lista de minutos de antelación para los que crear alarma. Ejemplo: [2880, 1440, 30] → alarmas
     * 2 días, 1 día y 30 min antes.
     */
    @NotNull(message = "minutosAntesList es obligatorio")
    private List<@Min(1) Integer> minutosAntesList;

    /**
     * Canal de notificación para todas las alarmas del lote. Si no se envía, se usa el canal
     * guardado en las preferencias del usuario.
     */
    private TipoCanal canal;
}
