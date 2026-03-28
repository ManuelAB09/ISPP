package es.us.meerkat.backend.dto.events;

import es.us.meerkat.backend.entity.events.TipoEvento;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** DTO para actualizar el tipo de un evento existente. */
@Data
public class UpdateEventTypeRequest {

    /** Nuevo tipo del evento. */
    @NotNull(message = "El tipo de evento es obligatorio")
    private TipoEvento tipoEvento;
}
