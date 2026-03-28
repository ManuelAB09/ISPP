package es.us.meerkat.backend.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import es.us.meerkat.backend.entity.TipoRecordatorio;
import es.us.meerkat.backend.service.emails.RecordatorioEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduler que dispara el envío de recordatorios por email en los momentos adecuados.
 *
 * <p>Ejecuciones:
 *
 * <ul>
 *   <li>Cada 10 min → comprueba recordatorios de 24h antes.
 *   <li>Cada 5 min → comprueba recordatorios de 1h antes.
 *   <li>Cada 5 min → comprueba recordatorios de 30min antes.
 *   <li>Cada día a las 4AM → limpia registros de más de 30 días.
 * </ul>
 *
 * <p>Los envíos son idempotentes: si el scheduler se ejecuta dos veces en la misma ventana, el
 * anti-duplicado en {@link RecordatorioEmailService} evita enviar el mismo email dos veces.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RecordatorioEmailScheduler {

    private final RecordatorioEmailService recordatorioEmailService;

    /** Comprueba y envía recordatorios de 24 horas antes. Cada 10 minutos. */
    @Scheduled(fixedDelay = 600_000)
    public void recordatorios24h() {
        log.info("Scheduler: procesando recordatorios 24h...");
        try {
            recordatorioEmailService.procesarRecordatorios(TipoRecordatorio.HORAS_24);
        } catch (Exception e) {
            log.error("Error en scheduler recordatorios 24h: {}", e.getMessage(), e);
        }
    }

    /** Comprueba y envía recordatorios de 1 hora antes. Cada 5 minutos. */
    @Scheduled(fixedDelay = 300_000)
    public void recordatorios1h() {
        log.info("Scheduler: procesando recordatorios 1h...");
        try {
            recordatorioEmailService.procesarRecordatorios(TipoRecordatorio.HORA_1);
        } catch (Exception e) {
            log.error("Error en scheduler recordatorios 1h: {}", e.getMessage(), e);
        }
    }

    /** Comprueba y envía recordatorios de 30 minutos antes. Cada 5 minutos. */
    @Scheduled(fixedDelay = 300_000)
    public void recordatorios30min() {
        log.info("Scheduler: procesando recordatorios 30min...");
        try {
            recordatorioEmailService.procesarRecordatorios(TipoRecordatorio.MINUTOS_30);
        } catch (Exception e) {
            log.error("Error en scheduler recordatorios 30min: {}", e.getMessage(), e);
        }
    }

    /** Limpieza diaria de registros de envío antiguos. A las 4:00 AM. */
    @Scheduled(cron = "0 0 4 * * *")
    public void limpiezaDiaria() {
        log.info("Scheduler: limpiando registros de recordatorios antiguos...");
        try {
            recordatorioEmailService.limpiarRecordatoriosAntiguos();
        } catch (Exception e) {
            log.error("Error en limpieza de recordatorios: {}", e.getMessage(), e);
        }
    }
}
