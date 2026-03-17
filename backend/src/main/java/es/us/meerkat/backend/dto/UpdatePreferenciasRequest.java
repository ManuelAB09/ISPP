package es.us.meerkat.backend.dto;

import es.us.meerkat.backend.entity.FrecuenciaNotificacion;
import es.us.meerkat.backend.entity.TipoCanal;
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

    /** Canal por defecto para las alarmas personalizadas. PLATAFORMA, EMAIL o AMBOS. */
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
