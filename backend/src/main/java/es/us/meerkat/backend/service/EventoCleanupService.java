package es.us.meerkat.backend.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.us.meerkat.backend.entity.Evento;
import es.us.meerkat.backend.repository.AlarmaPersonalizadaRepository;
import es.us.meerkat.backend.repository.AlertaEventoRepository;
import es.us.meerkat.backend.repository.AsistenciaEventoRepository;
import es.us.meerkat.backend.repository.EventoRepository;
import es.us.meerkat.backend.repository.GoogleCalendarEventoRepository;
import es.us.meerkat.backend.repository.RecordatorioEmailRepository;
import es.us.meerkat.backend.repository.ZoomMeetingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servicio para la limpieza automática de eventos pasados. Elimina eventos cuya fecha de fin (o
 * fecha de inicio si no tienen fin) sea anterior a 24 horas atrás.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventoCleanupService {

    private final EventoRepository eventoRepository;
    private final AsistenciaEventoRepository asistenciaEventoRepository;
    private final AlertaEventoRepository alertaEventoRepository;
    private final AlarmaPersonalizadaRepository alarmaPersonalizadaRepository;
    private final GoogleCalendarEventoRepository googleCalendarEventoRepository;
    private final RecordatorioEmailRepository recordatorioEmailRepository;
    private final ZoomMeetingRepository zoomMeetingRepository;

    /**
     * Elimina todos los eventos cuya fecha de fin (o inicio) sea anterior a 24h atrás, junto con
     * todas sus entidades relacionadas.
     *
     * @return Número de eventos eliminados.
     */
    @Transactional
    public int eliminarEventosPasados() {
        final LocalDateTime limite = LocalDateTime.now().minusHours(24);
        final List<Evento> eventos = eventoRepository.findEventosPasadosParaEliminar(limite);

        if (eventos.isEmpty()) {
            return 0;
        }

        for (Evento evento : eventos) {
            Long eventoId = evento.getId();
            asistenciaEventoRepository.deleteByEventoId(eventoId);
            alertaEventoRepository.deleteByEventoId(eventoId);
            alarmaPersonalizadaRepository.deleteByEventoId(eventoId);
            googleCalendarEventoRepository.deleteByEventoId(eventoId);
            recordatorioEmailRepository.deleteByEventoId(eventoId);
            zoomMeetingRepository.deleteByEventoId(eventoId);
            eventoRepository.delete(evento);
        }

        log.info("Limpieza automática: {} eventos pasados eliminados", eventos.size());
        return eventos.size();
    }
}
