package es.us.meerkat.backend.dto.google;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import es.us.meerkat.backend.entity.events.TipoEvento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO con el estado actual de la conexión de Google Calendar del usuario. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GoogleCalendarStatusResponse {

    /** Si el usuario tiene Google Calendar conectado. */
    private Boolean conectado;

    /** Si la sincronización automática está activa. */
    private Boolean sincronizacionActiva;

    /** Tipos de evento que se sincronizan. Null o lista vacía = todos los tipos. */
    private List<TipoEvento> tiposSincronizados;

    /** Fecha de la última sincronización realizada. */
    private LocalDateTime ultimaSincronizacion;
}
