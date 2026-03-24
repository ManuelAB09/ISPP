package es.us.meerkat.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import es.us.meerkat.backend.entity.TipoCanal;
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

    /** Canal por defecto para alarmas personalizadas. PLATAFORMA, EMAIL o AMBOS. */
    private TipoCanal canalAlarmasPorDefecto;

    /** Si desea recibir notificaciones para mensajes de comunidad. */
    private Boolean notificarMensajeComunidad;

    /** Frecuencia de notificaciones para menciones. SIEMPRE, RESUMEN_DIARIO, NUNCA */
    private Boolean notificarMenciones;

    /** Frecuencia de notificaciones para invitaciones. SIEMPRE, RESUMEN_DIARIO, NUNCA */
    private Boolean notificarInvitaciones;

    /** Frecuencia de notificaciones para anuncios. SIEMPRE, RESUMEN_DIARIO, NUNCA */
    private Boolean notificarAnuncios;

    /** Frecuencia de notificaciones para solicitudes de acceso. SIEMPRE, RESUMEN_DIARIO, NUNCA */
    private Boolean notificarSolicitudAcceso;

    /** Frecuencia de notificaciones para cambios de eventos. SIEMPRE, RESUMEN_DIARIO, NUNCA */
    private Boolean notificarCambiosDeEventos;
}
