package es.us.meerkat.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import es.us.meerkat.backend.entity.FrecuenciaNotificacion;
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

    /** Frecuencia de notificaciones para mensajes de comunidad. SIEMPRE, RESUMEN_DIARIO, NUNCA */
    private FrecuenciaNotificacion frecuenciaMensajeComunidad;

    /** Frecuencia de notificaciones para menciones. SIEMPRE, RESUMEN_DIARIO, NUNCA */
    private FrecuenciaNotificacion frecuenciaMenciones;

    /** Frecuencia de notificaciones para invitaciones. SIEMPRE, RESUMEN_DIARIO, NUNCA */
    private FrecuenciaNotificacion frecuenciaInvitaciones;

    /** Frecuencia de notificaciones para anuncios. SIEMPRE, RESUMEN_DIARIO, NUNCA */
    private FrecuenciaNotificacion frecuenciaAnuncios;

    /** Frecuencia de notificaciones para solicitudes de acceso. SIEMPRE, RESUMEN_DIARIO, NUNCA */
    private FrecuenciaNotificacion frecuenciaSolicitudAcceso;

    /** Frecuencia de notificaciones para cambios de eventos. SIEMPRE, RESUMEN_DIARIO, NUNCA */
    private FrecuenciaNotificacion frecuenciaCambiosDeEventos;
}
