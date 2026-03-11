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
 * Servicio para gestionar las preferencias de notificación por email del usuario.
 *
 * <p>Si el usuario no tiene preferencias guardadas aún, se crean con los valores por defecto (todo
 * activado) en el momento de la primera consulta.
 */
@Service
@RequiredArgsConstructor
public class PreferenciasNotificacionService {

    private final PreferenciasNotificacionRepository preferenciasRepository;
    private final UsuarioRepository usuarioRepository;

    // ===============================
    // OBTENER PREFERENCIAS
    // ===============================

    /**
     * Obtiene las preferencias de notificación del usuario. Si no existen, las crea con valores por
     * defecto.
     *
     * @param usuarioId ID del usuario.
     * @return DTO con las preferencias actuales.
     */
    @Transactional
    public PreferenciasNotificacionResponse obtenerPreferencias(final Long usuarioId) {
        final PreferenciasNotificacion prefs = getOrCreate(usuarioId);
        return toResponse(prefs);
    }

    // ===============================
    // ACTUALIZAR PREFERENCIAS
    // ===============================

    /**
     * Actualiza las preferencias de notificación del usuario. Solo actualiza los campos que vienen
     * informados en el request (patch parcial).
     *
     * @param usuarioId ID del usuario.
     * @param request Campos a actualizar.
     * @return DTO con las preferencias actualizadas.
     */
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

        return toResponse(preferenciasRepository.save(prefs));
    }

    // ===============================
    // USO INTERNO (desde RecordatorioService)
    // ===============================

    /**
     * Devuelve la entidad de preferencias del usuario. Crea una con valores por defecto si no
     * existe. Método público para ser usado por {@link RecordatorioEmailService}.
     *
     * @param usuarioId ID del usuario.
     * @return Entidad PreferenciasNotificacion.
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
    // HELPERS
    // ===============================

    private PreferenciasNotificacionResponse toResponse(final PreferenciasNotificacion prefs) {
        return PreferenciasNotificacionResponse.builder()
                .emailsActivados(prefs.getEmailsActivados())
                .recordatorio24h(prefs.getRecordatorio24h())
                .recordatorio1h(prefs.getRecordatorio1h())
                .recordatorio30min(prefs.getRecordatorio30min())
                .build();
    }
}
