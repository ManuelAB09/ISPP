package es.us.meerkat.backend.dto.google;

import java.util.List;

import es.us.meerkat.backend.entity.TipoEvento;
import lombok.Data;

/** DTO para actualizar las preferencias de sincronización con Google Calendar. */
@Data
public class UpdateCalendarPreferenciasRequest {

    /** Activar o desactivar la sincronización automática. */
    private Boolean sincronizacionActiva;

    /**
     * Lista de tipos de evento a sincronizar. Si se envía lista vacía o null → sincroniza todos los
     * tipos.
     */
    private List<TipoEvento> tiposSincronizados;
}
