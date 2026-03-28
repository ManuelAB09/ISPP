package es.us.meerkat.backend.service.emails;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.us.meerkat.backend.entity.Evento;
import es.us.meerkat.backend.entity.PreferenciasNotificacion;
import es.us.meerkat.backend.entity.RecordatorioEmail;
import es.us.meerkat.backend.entity.TipoRecordatorio;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.EventoRepository;
import es.us.meerkat.backend.repository.RecordatorioEmailRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;
import es.us.meerkat.backend.service.notifications.PreferenciasNotificacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servicio que orquesta el envío de recordatorios por email.
 *
 * <p>Lógica principal:
 *
 * <ol>
 *   <li>El scheduler llama a {@link #procesarRecordatorios(TipoRecordatorio)} periódicamente.
 *   <li>Se buscan los eventos que empiezan en la ventana de tiempo del tipo de recordatorio.
 *   <li>Por cada evento se obtienen los miembros de su comunidad.
 *   <li>Por cada miembro se comprueba: ¿ya se envió? ¿tiene ese recordatorio activado?
 *   <li>Si todo OK, se envía el email y se registra en {@link RecordatorioEmail}.
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecordatorioEmailService {

    /**
     * Margen en minutos que se añade a la ventana de búsqueda para no perder eventos en el borde
     * cuando el scheduler se ejecuta con ligero retraso.
     */
    private static final long MARGEN_MINUTOS = 5L;

    private final EventoRepository eventoRepository;
    private final UsuarioRepository usuarioRepository;
    private final RecordatorioEmailRepository recordatorioRepository;
    private final PreferenciasNotificacionService preferenciasService;
    private final EmailService emailService;

    // ===============================
    // PROCESADO DE RECORDATORIOS
    // ===============================

    /**
     * Punto de entrada llamado por el scheduler para un tipo de recordatorio concreto.
     *
     * <p>Busca eventos que comiencen en la ventana de tiempo correspondiente y dispara el envío
     * para cada miembro elegible.
     *
     * @param tipo Tipo de recordatorio a procesar.
     */
    @Transactional
    public void procesarRecordatorios(final TipoRecordatorio tipo) {
        final LocalDateTime ahora = LocalDateTime.now();
        final LocalDateTime desde = ahora.plusMinutes(tipo.getMinutosAntes() - MARGEN_MINUTOS);
        final LocalDateTime hasta = ahora.plusMinutes(tipo.getMinutosAntes() + MARGEN_MINUTOS);

        final List<Evento> eventos = eventoRepository.findEventosInRange(desde, hasta);
        log.info(
                "RecordatorioScheduler [{}]: {} eventos en ventana {}-{}",
                tipo,
                eventos.size(),
                desde,
                hasta);

        eventos.forEach(evento -> procesarEvento(evento, tipo));
    }

    /**
     * Limpia registros de recordatorios con más de 30 días. Llamado por el scheduler de limpieza.
     */
    @Transactional
    public void limpiarRecordatoriosAntiguos() {
        final LocalDateTime fechaLimite = LocalDateTime.now().minusDays(30);
        recordatorioRepository.deleteOldRecordatorios(fechaLimite);
        log.info("Limpieza de recordatorios antiguos completada. Límite: {}", fechaLimite);
    }

    // ===============================
    // PROCESO POR EVENTO
    // ===============================

    private void procesarEvento(final Evento evento, final TipoRecordatorio tipo) {
        if (evento.getComunidad() == null) {
            return;
        }

        final List<Usuario> miembros =
                usuarioRepository.findMiembrosByComunidadId(evento.getComunidad().getId());

        miembros.forEach(usuario -> procesarUsuario(usuario, evento, tipo));
    }

    private void procesarUsuario(
            final Usuario usuario, final Evento evento, final TipoRecordatorio tipo) {

        // 1. Comprobar si ya se envió este recordatorio (anti-duplicado)
        if (recordatorioRepository
                .findByEventoIdAndUsuarioIdAndTipo(evento.getId(), usuario.getId(), tipo)
                .isPresent()) {
            return;
        }

        // 2. Comprobar preferencias del usuario
        final PreferenciasNotificacion prefs = preferenciasService.getOrCreate(usuario.getId());
        if (!prefs.quiereRecordatorio(tipo)) {
            log.debug("Usuario {} no quiere recordatorio {} → omitido", usuario.getId(), tipo);
            return;
        }

        // 3. Intentar envío y registrar resultado
        final RecordatorioEmail registro = new RecordatorioEmail();
        registro.setTipo(tipo);
        registro.setEvento(evento);
        registro.setUsuario(usuario);

        try {
            emailService.enviarRecordatorio(usuario, evento, tipo);
            registro.setExitoso(true);
        } catch (Exception e) {
            log.error(
                    "Error enviando recordatorio [{}] a {} para evento {}: {}",
                    tipo,
                    usuario.getEmail(),
                    evento.getId(),
                    e.getMessage());
            registro.setExitoso(false);
            registro.setErrorMensaje(e.getMessage());
        }

        recordatorioRepository.save(registro);
    }
}
