package es.us.meerkat.backend.service.notifications;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.us.meerkat.backend.dto.events.UpdatePreferenciasRequest;
import es.us.meerkat.backend.dto.notifications.PreferenciasNotificacionResponse;
import es.us.meerkat.backend.entity.notifications.PreferenciasNotificacion;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.notifications.PreferenciasNotificacionRepository;
import es.us.meerkat.backend.repository.users.UsuarioRepository;
import lombok.RequiredArgsConstructor;

/**
 * Servicio para gestionar las preferencias de notificación del usuario. Versión actualizada:
 * incluye canalAlarmasPorDefecto para alarmas personalizadas.
 */
@Service
@RequiredArgsConstructor
public class PreferenciasNotificacionService {

    private final PreferenciasNotificacionRepository preferenciasRepository;
    private final UsuarioRepository usuarioRepository;

    // ===============================
    // OBTENER
    // ===============================

    @Transactional
    public PreferenciasNotificacionResponse obtenerPreferencias(final Long usuarioId) {
        return toResponse(getOrCreate(usuarioId));
    }

    // ===============================
    // ACTUALIZAR
    // ===============================

    @Transactional
    public PreferenciasNotificacionResponse actualizarPreferencias(
            final Long usuarioId, final UpdatePreferenciasRequest request) {

        final PreferenciasNotificacion prefs = getOrCreate(usuarioId);

        if (request.getEmailsActivados() != null) {
            prefs.setEmailsActivados(request.getEmailsActivados());
        }

        if (request.getRecordatorio24h() != null) {
            prefs.setRecordatorio24h(request.getRecordatorio24h());
        }

        if (request.getRecordatorio1h() != null) {
            prefs.setRecordatorio1h(request.getRecordatorio1h());
        }

        if (request.getRecordatorio30min() != null) {
            prefs.setRecordatorio30min(request.getRecordatorio30min());
        }

        if (request.getCanalAlarmasPorDefecto() != null) {
            prefs.setCanalAlarmasPorDefecto(request.getCanalAlarmasPorDefecto());
        }

        if (request.getNotificarMensajeComunidad() != null) {
            prefs.setNotificarMensajeComunidad(request.getNotificarMensajeComunidad());
        }

        if (request.getNotificarMenciones() != null) {
            prefs.setNotificarMenciones(request.getNotificarMenciones());
        }

        if (request.getNotificarInvitaciones() != null) {
            prefs.setNotificarInvitaciones(request.getNotificarInvitaciones());
        }

        if (request.getNotificarAnuncios() != null) {
            prefs.setNotificarAnuncios(request.getNotificarAnuncios());
        }

        if (request.getNotificarSolicitudAcceso() != null) {
            prefs.setNotificarSolicitudAcceso(request.getNotificarSolicitudAcceso());
        }

        if (request.getNotificarCambiosDeEventos() != null) {
            prefs.setNotificarCambiosDeEventos(request.getNotificarCambiosDeEventos());
        }

        return toResponse(preferenciasRepository.save(prefs));
    }

    // ===============================
    // USO INTERNO
    // ===============================

    /**
     * Devuelve la entidad de preferencias, creándola con valores por defecto si no existe. Público
     * para ser usado por AlarmaPersonalizadaService y RecordatorioEmailService.
     */
    @Transactional
    public PreferenciasNotificacion getOrCreate(final Long usuarioId) {
        return preferenciasRepository
                .findByUsuarioId(usuarioId)
                .orElseGet(
                        () -> {
                            final Usuario usuario =
                                    usuarioRepository
                                            .findById(usuarioId)
                                            .orElseThrow(
                                                    () ->
                                                            new RuntimeException(
                                                                    "Usuario no encontrado"));
                            final PreferenciasNotificacion nuevas = new PreferenciasNotificacion();
                            nuevas.setUsuario(usuario);
                            return preferenciasRepository.save(nuevas);
                        });
    }

    // ===============================
    // HELPER
    // ===============================

    private PreferenciasNotificacionResponse toResponse(final PreferenciasNotificacion prefs) {
        return PreferenciasNotificacionResponse.builder()
                .emailsActivados(prefs.getEmailsActivados())
                .recordatorio24h(prefs.getRecordatorio24h())
                .recordatorio1h(prefs.getRecordatorio1h())
                .recordatorio30min(prefs.getRecordatorio30min())
                .canalAlarmasPorDefecto(prefs.getCanalAlarmasPorDefecto())
                .notificarMensajeComunidad(prefs.getNotificarMensajeComunidad())
                .notificarMenciones(prefs.getNotificarMenciones())
                .notificarInvitaciones(prefs.getNotificarInvitaciones())
                .notificarAnuncios(prefs.getNotificarAnuncios())
                .notificarSolicitudAcceso(prefs.getNotificarSolicitudAcceso())
                .notificarCambiosDeEventos(prefs.getNotificarCambiosDeEventos())
                .build();
    }
}
