package es.us.meerkat.backend.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.us.meerkat.backend.dto.AlertaEventoResponse;
import es.us.meerkat.backend.dto.MisEventosItemResponse;
import es.us.meerkat.backend.entity.AlertaEvento;
import es.us.meerkat.backend.entity.EstadoAsistencia;
import es.us.meerkat.backend.entity.Evento;
import es.us.meerkat.backend.entity.TipoAlerta;
import es.us.meerkat.backend.entity.TipoEvento;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.AlertaEventoRepository;
import es.us.meerkat.backend.repository.AsistenciaEventoRepository;
import es.us.meerkat.backend.repository.EventoRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servicio para gestionar el listado unificado "Mis Eventos" y las alertas automáticas.
 *
 * <p>Proporciona:
 *
 * <ul>
 *   <li>Listado de todos los eventos próximos del usuario con información visual de proximidad.
 *   <li>Gestión de alertas: consulta, marcado como leída y conteo de no leídas.
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MisEventosService {

    /** Minutos antes del evento para generar alerta inminente. */
    private static final long MINUTOS_ALERTA_INMINENTE = 15L;

    /** Horas antes del evento para considerar "próxima en 24h". */
    private static final long HORAS_ALERTA_24H = 24L;

    private final EventoRepository eventoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AlertaEventoRepository alertaEventoRepository;
    private final AsistenciaEventoRepository asistenciaEventoRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // ===============================
    // MIS EVENTOS
    // ===============================

    /**
     * Obtiene el listado unificado de próximos eventos del usuario.
     *
     * <p>Incluye todos los eventos de las comunidades del usuario que aún no han comenzado,
     * enriquecidos con información de proximidad para su destacado visual.
     *
     * @param usuarioId Identificador del usuario autenticado.
     * @return Lista de eventos próximos ordenados por fecha.
     */
    public List<MisEventosItemResponse> obtenerMisEventos(final Long usuarioId) {
        validarUsuario(usuarioId);

        final LocalDateTime ahora = LocalDateTime.now();
        final List<Evento> eventos =
                eventoRepository.findProximosEventosByUsuarioId(
                        usuarioId,
                        ahora,
                        ahora.minusHours(
                                2)); // Incluir eventos que comenzaron hace hasta 2 horas para
        // mostrar recientes
        return eventos.stream()
                .map(e -> mapToMisEventosItem(e, usuarioId, ahora))
                .collect(Collectors.toList());
    }

    /**
     * Obtiene el historial completo de eventos del usuario (pasados y futuros).
     *
     * @param usuarioId Identificador del usuario.
     * @param incluirCancelados Si se incluyen eventos cancelados.
     * @return Lista de todos los eventos del usuario.
     */
    public List<MisEventosItemResponse> obtenerHistorialEventos(
            final Long usuarioId, final boolean incluirCancelados) {
        validarUsuario(usuarioId);

        final LocalDateTime ahora = LocalDateTime.now();
        List<Evento> eventos = eventoRepository.findAllEventosByUsuarioId(usuarioId);

        if (!incluirCancelados) {
            eventos =
                    eventos.stream()
                            .filter(e -> !Boolean.TRUE.equals(e.getCancelado()))
                            .collect(Collectors.toList());
        }

        return eventos.stream()
                .map(e -> mapToMisEventosItem(e, usuarioId, ahora))
                .collect(Collectors.toList());
    }

    // ===============================
    // ALERTAS
    // ===============================

    /**
     * Obtiene las alertas no leídas del usuario.
     *
     * @param usuarioId Identificador del usuario.
     * @return Lista de alertas no leídas ordenadas por fecha desc.
     */
    public List<AlertaEventoResponse> obtenerAlertasNoLeidas(final Long usuarioId) {
        validarUsuario(usuarioId);
        return alertaEventoRepository.findUnreadByUsuarioId(usuarioId).stream()
                .map(this::mapToAlertaResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene todas las alertas del usuario (leídas y no leídas).
     *
     * @param usuarioId Identificador del usuario.
     * @return Lista de todas las alertas del usuario.
     */
    public List<AlertaEventoResponse> obtenerTodasLasAlertas(final Long usuarioId) {
        validarUsuario(usuarioId);
        return alertaEventoRepository.findAllByUsuarioId(usuarioId).stream()
                .map(this::mapToAlertaResponse)
                .collect(Collectors.toList());
    }

    /**
     * Cuenta las alertas no leídas del usuario.
     *
     * @param usuarioId Identificador del usuario.
     * @return Número de alertas no leídas.
     */
    public Long contarAlertasNoLeidas(final Long usuarioId) {
        validarUsuario(usuarioId);
        return alertaEventoRepository.countUnreadByUsuarioId(usuarioId);
    }

    /**
     * Marca una alerta específica como leída.
     *
     * @param alertaId Identificador de la alerta.
     * @param usuarioId Identificador del usuario (para verificar propiedad).
     * @return La alerta actualizada.
     */
    @Transactional
    public AlertaEventoResponse marcarAlertaComoLeida(final Long alertaId, final Long usuarioId) {
        final AlertaEvento alerta =
                alertaEventoRepository
                        .findById(alertaId)
                        .orElseThrow(() -> new RuntimeException("Alerta no encontrada"));

        if (!alerta.getUsuario().getId().equals(usuarioId)) {
            throw new RuntimeException("No tienes permiso para modificar esta alerta");
        }

        alerta.marcarComoLeida();
        AlertaEventoResponse response = mapToAlertaResponse(alertaEventoRepository.save(alerta));
        broadcastAlertCount(usuarioId, alerta.getUsuario().getEmail());
        return response;
    }

    /**
     * Marca todas las alertas no leídas del usuario como leídas.
     *
     * @param usuarioId Identificador del usuario.
     */
    @Transactional
    public void marcarTodasComoLeidas(final Long usuarioId) {
        Usuario usuario =
                usuarioRepository
                        .findById(usuarioId)
                        .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        alertaEventoRepository.markAllAsReadByUsuarioId(usuarioId, LocalDateTime.now());
        broadcastAlertCount(usuarioId, usuario.getEmail());
    }

    // ===============================
    // GENERACIÓN DE ALERTAS (llamado desde AlertaScheduler)
    // ===============================

    /**
     * Genera alertas para todos los usuarios cuyos eventos comiencen en el rango especificado. Este
     * método es invocado por {@link AlertaScheduler}.
     *
     * @param desde Inicio del rango de tiempo.
     * @param hasta Fin del rango de tiempo.
     * @param tipoAlerta Tipo de alerta a generar.
     */
    @Transactional
    public void generarAlertasParaRango(
            final LocalDateTime desde, final LocalDateTime hasta, final TipoAlerta tipoAlerta) {

        final List<Evento> eventosEnRango = eventoRepository.findEventosInRange(desde, hasta);

        // Obtener IDs de eventos que ya tienen alerta generada de este tipo en este
        // rango
        final List<Long> eventosConAlertaExistente =
                alertaEventoRepository.findEventoIdsWithAlertaInRange(desde, hasta, tipoAlerta);

        eventosEnRango.stream()
                .filter(e -> !eventosConAlertaExistente.contains(e.getId()))
                .forEach(evento -> generarAlertasParaEvento(evento, tipoAlerta));
    }

    /**
     * Genera alertas individuales para todos los miembros de la comunidad del evento.
     *
     * @param evento Evento para el que se generan alertas.
     * @param tipoAlerta Tipo de alerta a generar.
     */
    private void generarAlertasParaEvento(final Evento evento, final TipoAlerta tipoAlerta) {
        if (evento.getComunidad() == null) {
            return;
        }

        // Obtener todos los miembros de la comunidad del evento
        final List<Usuario> miembros =
                usuarioRepository.findMiembrosByComunidadId(evento.getComunidad().getId());

        final String mensaje = buildMensajeAlerta(evento, tipoAlerta);

        miembros.forEach(
                usuario -> {
                    // Verificar que no existe ya la alerta para este usuario y evento
                    boolean yaExiste =
                            alertaEventoRepository
                                    .findByEventoIdAndUsuarioIdAndTipo(
                                            evento.getId(), usuario.getId(), tipoAlerta)
                                    .isPresent();

                    if (!yaExiste) {
                        final AlertaEvento alerta = new AlertaEvento();
                        alerta.setTipo(tipoAlerta);
                        alerta.setMensaje(mensaje);
                        alerta.setEvento(evento);
                        alerta.setUsuario(usuario);
                        alertaEventoRepository.save(alerta);
                        broadcastAlertCount(usuario.getId(), usuario.getEmail());
                        log.info(
                                "Alerta {} generada para usuario {} en evento {}",
                                tipoAlerta,
                                usuario.getId(),
                                evento.getId());
                    }
                });
    }

    // ===============================
    // BROADCAST WEBSOCKET
    // ===============================

    private void broadcastAlertCount(final Long usuarioId, final String email) {
        try {
            Long count = alertaEventoRepository.countUnreadByUsuarioId(usuarioId);
            messagingTemplate.convertAndSendToUser(email, "/queue/alerts_count", count);
        } catch (Exception e) {
            log.warn(
                    "No se pudo enviar alert count por WS al usuario {}: {}",
                    usuarioId,
                    e.getMessage());
        }
    }

    // ===============================
    // HELPERS DE MAPEO
    // ===============================

    /** Convierte una entidad Evento a MisEventosItemResponse con información de proximidad. */
    private MisEventosItemResponse mapToMisEventosItem(
            final Evento evento, final Long usuarioId, final LocalDateTime ahora) {

        final TipoEvento tipo =
                evento.getTipoEvento() != null ? evento.getTipoEvento() : TipoEvento.OTRO;

        final long minutosHastaEvento = ChronoUnit.MINUTES.between(ahora, evento.getFechaHora());
        final boolean proximaEn24H =
                minutosHastaEvento >= 0 && minutosHastaEvento <= (HORAS_ALERTA_24H * 60);
        final boolean inminenteEn15Min =
                minutosHastaEvento >= 0 && minutosHastaEvento <= MINUTOS_ALERTA_INMINENTE;

        // Verificar si el usuario confirmó asistencia
        boolean asistenciaConfirmada =
                asistenciaEventoRepository
                        .findByEventoIdAndUsuarioId(evento.getId(), usuarioId)
                        .map(ae -> EstadoAsistencia.CONFIRMADA.equals(ae.getEstado()))
                        .orElse(false);

        return MisEventosItemResponse.builder()
                .id(evento.getId())
                .titulo(evento.getTitulo())
                .descripcion(evento.getDescripcion())
                .tipoEvento(tipo)
                .tipoEventoNombre(tipo.getNombre())
                .icono(tipo.getIcono())
                .fechaHora(evento.getFechaHora())
                .fechaFin(evento.getFechaFin())
                .proximaEn24H(proximaEn24H)
                .inminenteEn15Min(inminenteEn15Min)
                .asistenciaConfirmada(asistenciaConfirmada)
                .esVirtual(evento.getEsVirtual())
                .cancelado(evento.getCancelado())
                .comunidadNombre(
                        evento.getComunidad() != null ? evento.getComunidad().getNombre() : null)
                .comunidadId(evento.getComunidad() != null ? evento.getComunidad().getId() : null)
                .ubicacionNombre(
                        evento.getUbicacion() != null ? evento.getUbicacion().getNombre() : null)
                .asistentesConfirmados(evento.getAsistentesConfirmados())
                .aforo(evento.getAforo())
                .build();
    }

    /** Convierte una entidad AlertaEvento a AlertaEventoResponse. */
    private AlertaEventoResponse mapToAlertaResponse(final AlertaEvento alerta) {
        final Evento evento = alerta.getEvento();
        final TipoEvento tipoEvento =
                evento.getTipoEvento() != null ? evento.getTipoEvento() : TipoEvento.OTRO;

        return AlertaEventoResponse.builder()
                .id(alerta.getId())
                .tipo(alerta.getTipo())
                .mensaje(alerta.getMensaje())
                .leida(alerta.getLeida())
                .createdAt(alerta.getCreatedAt())
                .eventoId(evento.getId())
                .eventoTitulo(evento.getTitulo())
                .tipoEvento(tipoEvento)
                .icono(tipoEvento.getIcono())
                .eventoFechaHora(evento.getFechaHora())
                .comunidadNombre(
                        evento.getComunidad() != null ? evento.getComunidad().getNombre() : null)
                .build();
    }

    /** Construye el mensaje de una alerta según el tipo. */
    private String buildMensajeAlerta(final Evento evento, final TipoAlerta tipoAlerta) {
        return switch (tipoAlerta) {
            case INMINENTE_15MIN ->
                    String.format("⏰ El evento \"%s\" comienza en 15 minutos.", evento.getTitulo());
            case PROXIMA_24H ->
                    String.format("📅 El evento \"%s\" comienza mañana.", evento.getTitulo());
        };
    }

    // ===============================
    // LIMPIEZA (llamado desde AlertaScheduler)
    // ===============================

    /**
     * Elimina alertas leídas con más de 7 días de antigüedad. Llamado por el scheduler diario de
     * limpieza.
     */
    @Transactional
    public void limpiarAlertasAntiguas() {
        final LocalDateTime fechaLimite = LocalDateTime.now().minusDays(7);
        alertaEventoRepository.deleteOldReadAlertas(fechaLimite);
        log.info("Limpieza de alertas antiguas completada. Límite: {}", fechaLimite);
    }

    /** Valida que el usuario existe. */
    private void validarUsuario(final Long usuarioId) {
        if (!usuarioRepository.existsById(usuarioId)) {
            throw new RuntimeException("Usuario no encontrado");
        }
    }
}
