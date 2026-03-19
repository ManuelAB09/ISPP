package es.us.meerkat.backend.dto;

import es.us.meerkat.backend.entity.TipoEvento;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** DTO para actualizar el tipo de un evento existente. */
@Data
public class UpdateEventTypeRequest {

    /** Nuevo tipo del evento. */
    @NotNull(message = "El tipo de evento es obligatorio")
    private TipoEvento tipoEvento;
}
