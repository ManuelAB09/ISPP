package es.us.meerkat.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.us.meerkat.backend.dto.PreferenciasNotificacionResponse;
import es.us.meerkat.backend.dto.UpdatePreferenciasRequest;
import es.us.meerkat.backend.entity.PreferenciasNotificacion;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.PreferenciasNotificacionRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;
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

        if (request.getFrecuenciaMensajeComunidad() != null) {
            prefs.setFrecuenciaMensajeComunidad(request.getFrecuenciaMensajeComunidad());
        }

        if (request.getFrecuenciaMenciones() != null) {
            prefs.setFrecuenciaMenciones(request.getFrecuenciaMenciones());
        }

        if (request.getFrecuenciaInvitaciones() != null) {
            prefs.setFrecuenciaInvitaciones(request.getFrecuenciaInvitaciones());
        }

        if (request.getFrecuenciaAnuncios() != null) {
            prefs.setFrecuenciaAnuncios(request.getFrecuenciaAnuncios());
        }

        if (request.getFrecuenciaSolicitudAcceso() != null) {
            prefs.setFrecuenciaSolicitudAcceso(request.getFrecuenciaSolicitudAcceso());
        }

        if (request.getFrecuenciaCambiosDeEventos() != null) {
            prefs.setFrecuenciaCambiosDeEventos(request.getFrecuenciaCambiosDeEventos());
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
                .frecuenciaMensajeComunidad(prefs.getFrecuenciaMensajeComunidad())
                .frecuenciaMenciones(prefs.getFrecuenciaMenciones())
                .frecuenciaInvitaciones(prefs.getFrecuenciaInvitaciones())
                .frecuenciaAnuncios(prefs.getFrecuenciaAnuncios())
                .frecuenciaSolicitudAcceso(prefs.getFrecuenciaSolicitudAcceso())
                .frecuenciaCambiosDeEventos(prefs.getFrecuenciaCambiosDeEventos())
                .build();
    }
}
