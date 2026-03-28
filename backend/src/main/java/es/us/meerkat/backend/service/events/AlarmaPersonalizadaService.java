package es.us.meerkat.backend.service.events;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.us.meerkat.backend.dto.AlarmaPersonalizadaResponse;
import es.us.meerkat.backend.dto.CrearAlarmaRequest;
import es.us.meerkat.backend.dto.CrearAlarmasLoteRequest;
import es.us.meerkat.backend.entity.AlarmaPersonalizada;
import es.us.meerkat.backend.entity.AlertaEvento;
import es.us.meerkat.backend.entity.Evento;
import es.us.meerkat.backend.entity.PreferenciasNotificacion;
import es.us.meerkat.backend.entity.TipoAlerta;
import es.us.meerkat.backend.entity.TipoCanal;
import es.us.meerkat.backend.entity.TipoEvento;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.AlarmaPersonalizadaRepository;
import es.us.meerkat.backend.repository.AlertaEventoRepository;
import es.us.meerkat.backend.repository.EventoRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;
import es.us.meerkat.backend.service.emails.EmailService;
import es.us.meerkat.backend.service.notifications.PreferenciasNotificacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servicio para gestionar las alarmas personalizadas por evento.
 *
 * <p>Responsabilidades:
 *
 * <ul>
 *   <li>Crear alarmas individuales o en lote para un evento.
 *   <li>Listar y eliminar alarmas del usuario.
 *   <li>Disparar alarmas pendientes (llamado desde el scheduler).
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlarmaPersonalizadaService {

    private final AlarmaPersonalizadaRepository alarmaRepository;
    private final EventoRepository eventoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AlertaEventoRepository alertaEventoRepository;
    private final PreferenciasNotificacionService preferenciasService;
    private final EmailService emailService;
    private final SimpMessagingTemplate messagingTemplate;

    // ===============================
    // CREAR ALARMAS
    // ===============================

    /**
     * Crea una alarma personalizada para un evento concreto. Si ya existe una alarma con los mismos
     * minutos para ese usuario y evento, la devuelve sin crear duplicado.
     *
     * @param usuarioId ID del usuario.
     * @param eventoId ID del evento.
     * @param request Datos de la alarma (minutosAntes, canal opcional).
     * @return La alarma creada o existente.
     */
    @Transactional
    public AlarmaPersonalizadaResponse crearAlarma(
            final Long usuarioId, final Long eventoId, final CrearAlarmaRequest request) {

        final Usuario usuario = getUsuario(usuarioId);
        final Evento evento = getEvento(eventoId);

        validarEventoNoFinalizado(evento);

        // Comprobar duplicado
        final var existente =
                alarmaRepository.findByEventoIdAndUsuarioIdAndMinutosAntes(
                        eventoId, usuarioId, request.getMinutosAntes());
        if (existente.isPresent()) {
            return toResponse(existente.get());
        }

        final TipoCanal canal = resolverCanal(request.getCanal(), usuarioId);
        final AlarmaPersonalizada alarma =
                buildAlarma(usuario, evento, request.getMinutosAntes(), canal);
        return toResponse(alarmaRepository.save(alarma));
    }

    /**
     * Crea múltiples alarmas en un solo request. Ignora silenciosamente duplicados (mismos minutos
     * ya configurados). Útil al confirmar asistencia con varios checkboxes seleccionados.
     *
     * @param usuarioId ID del usuario.
     * @param eventoId ID del evento.
     * @param request Lista de minutos y canal opcional.
     * @return Lista de alarmas creadas (no incluye las que ya existían).
     */
    @Transactional
    public List<AlarmaPersonalizadaResponse> crearAlarmasLote(
            final Long usuarioId, final Long eventoId, final CrearAlarmasLoteRequest request) {

        final Usuario usuario = getUsuario(usuarioId);
        final Evento evento = getEvento(eventoId);

        validarEventoNoFinalizado(evento);

        final TipoCanal canal = resolverCanal(request.getCanal(), usuarioId);

        return request.getMinutosAntesList().stream()
                .filter(
                        minutos ->
                                alarmaRepository
                                        .findByEventoIdAndUsuarioIdAndMinutosAntes(
                                                eventoId, usuarioId, minutos)
                                        .isEmpty())
                .map(
                        minutos -> {
                            final AlarmaPersonalizada alarma =
                                    buildAlarma(usuario, evento, minutos, canal);
                            return toResponse(alarmaRepository.save(alarma));
                        })
                .collect(Collectors.toList());
    }

    // ===============================
    // CONSULTAR ALARMAS
    // ===============================

    /** Lista todas las alarmas pendientes del usuario, ordenadas por fecha de disparo. */
    public List<AlarmaPersonalizadaResponse> listarAlarmasPendientes(final Long usuarioId) {
        return alarmaRepository.findPendientesByUsuarioId(usuarioId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** Lista todas las alarmas del usuario para un evento concreto. */
    public List<AlarmaPersonalizadaResponse> listarAlarmasDeEvento(
            final Long usuarioId, final Long eventoId) {
        return alarmaRepository.findByEventoIdAndUsuarioId(eventoId, usuarioId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ===============================
    // ELIMINAR ALARMAS
    // ===============================

    /**
     * Elimina una alarma concreta del usuario. Verifica que la alarma pertenezca al usuario antes
     * de eliminar.
     */
    @Transactional
    public void eliminarAlarma(final Long alarmaId, final Long usuarioId) {
        final AlarmaPersonalizada alarma =
                alarmaRepository
                        .findById(alarmaId)
                        .orElseThrow(() -> new RuntimeException("Alarma no encontrada"));

        if (!alarma.getUsuario().getId().equals(usuarioId)) {
            throw new RuntimeException("No tienes permiso para eliminar esta alarma");
        }

        alarmaRepository.delete(alarma);
    }

    /**
     * Elimina todas las alarmas del usuario para un evento. Se llama cuando el usuario cancela su
     * asistencia al evento.
     */
    @Transactional
    public void eliminarAlarmasDeEvento(final Long usuarioId, final Long eventoId) {
        alarmaRepository.deleteByEventoIdAndUsuarioId(eventoId, usuarioId);
    }

    // ===============================
    // DISPARO DE ALARMAS (scheduler)
    // ===============================

    /**
     * Dispara todas las alarmas cuya fechaDisparo ya ha llegado. Llamado cada minuto por {@link
     * AlarmaScheduler}. Por cada alarma: envía notificación en plataforma y/o email según canal.
     */
    @Transactional
    public void dispararAlarmasPendientes() {
        final LocalDateTime ahora = LocalDateTime.now();
        final List<AlarmaPersonalizada> pendientes =
                alarmaRepository.findAlarmasPendientesADisparar(ahora);

        log.info("AlarmaScheduler: {} alarmas pendientes a disparar", pendientes.size());

        pendientes.forEach(
                alarma -> {
                    try {
                        dispararAlarma(alarma);
                        alarma.marcarDisparada();
                        alarmaRepository.save(alarma);
                    } catch (Exception e) {
                        log.error("Error disparando alarma {}: {}", alarma.getId(), e.getMessage());
                    }
                });
    }

    /** Limpia alarmas disparadas con más de 30 días. Llamado por el scheduler diario. */
    @Transactional
    public void limpiarAlarmasAntiguas() {
        alarmaRepository.deleteOldDisparadas(LocalDateTime.now().minusDays(30));
    }

    // ===============================
    // DISPARO INTERNO
    // ===============================

    private void dispararAlarma(final AlarmaPersonalizada alarma) throws Exception {
        final Evento evento = alarma.getEvento();
        final Usuario usuario = alarma.getUsuario();
        final TipoCanal canal = alarma.getCanal();

        log.info(
                "Disparando alarma [{}] canal={} para usuario={} evento={}",
                alarma.getEtiqueta(),
                canal,
                usuario.getId(),
                evento.getId());

        // Notificación en plataforma
        if (canal == TipoCanal.PLATAFORMA || canal == TipoCanal.AMBOS) {
            final AlertaEvento alerta = new AlertaEvento();
            alerta.setTipo(TipoAlerta.INMINENTE_15MIN); // reutilizamos el tipo más cercano
            alerta.setMensaje(buildMensajeAlarma(alarma));
            alerta.setEvento(evento);
            alerta.setUsuario(usuario);
            alertaEventoRepository.save(alerta);

            final long unreadCount = alertaEventoRepository.countUnreadByUsuarioId(usuario.getId());
            messagingTemplate.convertAndSendToUser(
                    usuario.getId().toString(), "/queue/alerts_count", unreadCount);

            messagingTemplate.convertAndSendToUser(
                    usuario.getId().toString(),
                    "/queue/notificaciones",
                    Map.of(
                            "tipo",
                            "EVENT_ALERT",
                            "mensaje",
                            alerta.getMensaje(),
                            "eventoId",
                            evento.getId(),
                            "eventoTitulo",
                            evento.getTitulo(),
                            "eventoFechaHora",
                            evento.getFechaHora() != null ? evento.getFechaHora().toString() : null,
                            "comunidadNombre",
                            evento.getComunidad() != null
                                    ? evento.getComunidad().getNombre()
                                    : null,
                            "icono",
                            evento.getTipoEvento() != null
                                    ? evento.getTipoEvento().getIcono()
                                    : TipoEvento.OTRO.getIcono()));
        }

        // Email
        if (canal == TipoCanal.EMAIL || canal == TipoCanal.AMBOS) {
            emailService.enviarRecordatorioAlarma(usuario, evento, alarma.getEtiqueta());
        }
    }

    private String buildMensajeAlarma(final AlarmaPersonalizada alarma) {
        final TipoEvento tipo =
                alarma.getEvento().getTipoEvento() != null
                        ? alarma.getEvento().getTipoEvento()
                        : TipoEvento.OTRO;
        return String.format(
                "%s \"%s\" comienza %s.",
                tipo.getIcono(), alarma.getEvento().getTitulo(), alarma.getEtiqueta());
    }

    // ===============================
    // HELPERS
    // ===============================

    private AlarmaPersonalizada buildAlarma(
            final Usuario usuario,
            final Evento evento,
            final Integer minutosAntes,
            final TipoCanal canal) {

        final AlarmaPersonalizada alarma = new AlarmaPersonalizada();
        alarma.setUsuario(usuario);
        alarma.setEvento(evento);
        alarma.setMinutosAntes(minutosAntes);
        alarma.setCanal(canal);
        // fechaDisparo se calcula en @PrePersist
        return alarma;
    }

    /** Si el request no especifica canal, usa el canal por defecto de las preferencias. */
    private TipoCanal resolverCanal(final TipoCanal canalSolicitado, final Long usuarioId) {
        if (canalSolicitado != null) {
            return canalSolicitado;
        }

        final PreferenciasNotificacion prefs = preferenciasService.getOrCreate(usuarioId);
        return prefs.getCanalAlarmasPorDefecto() != null
                ? prefs.getCanalAlarmasPorDefecto()
                : TipoCanal.AMBOS;
    }

    private void validarEventoNoFinalizado(final Evento evento) {
        if (Boolean.TRUE.equals(evento.getCancelado())) {
            throw new RuntimeException("No se pueden crear alarmas para un evento cancelado");
        }
        if (evento.getFechaHora() != null && evento.getFechaHora().isBefore(LocalDateTime.now())) {
            throw new RuntimeException(
                    "No se pueden crear alarmas para un evento que ya ha comenzado");
        }
    }

    private AlarmaPersonalizadaResponse toResponse(final AlarmaPersonalizada alarma) {
        return AlarmaPersonalizadaResponse.builder()
                .id(alarma.getId())
                .minutosAntes(alarma.getMinutosAntes())
                .etiqueta(alarma.getEtiqueta())
                .canal(alarma.getCanal())
                .fechaDisparo(alarma.getFechaDisparo())
                .disparada(alarma.getDisparada())
                .eventoId(alarma.getEvento().getId())
                .eventoTitulo(alarma.getEvento().getTitulo())
                .build();
    }

    private Usuario getUsuario(final Long usuarioId) {
        return usuarioRepository
                .findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    private Evento getEvento(final Long eventoId) {
        return eventoRepository
                .findById(eventoId)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado"));
    }
}
