package es.us.meerkat.backend.dto.events;

import es.us.meerkat.backend.entity.notifications.TipoCanal;
import lombok.Data;

/**
 * DTO de entrada para actualizar las preferencias de notificación del usuario.
 *
 * <p>Todos los campos son opcionales; solo se actualizan los que vienen informados.
 */
@Data
public class UpdatePreferenciasRequest {

    /** Activar/desactivar todos los emails de recordatorio. */
    private Boolean emailsActivados;

    /** Activar/desactivar recordatorio 24h antes. */
    private Boolean recordatorio24h;

    /** Activar/desactivar recordatorio 1h antes. */
    private Boolean recordatorio1h;

    /** Activar/desactivar recordatorio 30min antes. */
    private Boolean recordatorio30min;

    private TipoCanal canalAlarmasPorDefecto;

    private Boolean notificarMensajeComunidad;

    private Boolean notificarMenciones;

    private Boolean notificarInvitaciones;

    private Boolean notificarAnuncios;

    private Boolean notificarSolicitudAcceso;

    private Boolean notificarCambiosDeEventos;
}
