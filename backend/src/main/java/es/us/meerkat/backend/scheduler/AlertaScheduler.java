package es.us.meerkat.backend.scheduler;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import es.us.meerkat.backend.entity.notifications.TipoAlerta;
import es.us.meerkat.backend.service.events.MisEventosService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduler que genera alertas automáticas para eventos próximos.
 *
 * <p>Se ejecuta periódicamente para detectar:
 *
 * <ul>
 *   <li>Eventos que comienzan en menos de 24 horas → alerta PROXIMA_24H.
 *   <li>Eventos que comienzan en 15 minutos o menos → alerta INMINENTE_15MIN.
 * </ul>
 *
 * <p>Las alertas se generan una sola vez por evento y usuario (sin duplicados).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AlertaScheduler {

    /**
     * Ventana de tiempo para alerta de 15 minutos: busca eventos entre ahora y ahora+15min. Se
     * añaden 2 minutos de margen para no perder eventos en el borde de la ventana.
     */
    private static final long MINUTOS_VENTANA_INMINENTE = 17L;

    /**
     * Ventana de tiempo para alerta de 24h: busca eventos entre ahora+23h y ahora+24h. Así detecta
     * eventos que entran en la ventana de 24h sin duplicar con la de 15min.
     */
    private static final long HORAS_VENTANA_24H_INICIO = 23L;

    private static final long HORAS_VENTANA_24H_FIN = 24L;

    private final MisEventosService misEventosService;

    /**
     * Genera alertas inminentes cada 5 minutos. Detecta eventos que comienzan en los próximos 15
     * minutos.
     */
    @Scheduled(fixedDelay = 300_000) // 5 minutos en ms
    public void generarAlertasInminentes() {
        log.info("Scheduler: verificando eventos inminentes (15 min)...");
        try {
            final LocalDateTime ahora = LocalDateTime.now();
            final LocalDateTime hasta = ahora.plusMinutes(MINUTOS_VENTANA_INMINENTE);
            misEventosService.generarAlertasParaRango(ahora, hasta, TipoAlerta.INMINENTE_15MIN);
        } catch (Exception e) {
            log.error("Error generando alertas inminentes: {}", e.getMessage(), e);
        }
    }

    /**
     * Genera alertas de 24 horas cada 30 minutos. Detecta eventos que comienzan entre 23h y 24h a
     * partir de ahora.
     */
    @Scheduled(fixedDelay = 1_800_000) // 30 minutos en ms
    public void generarAlertas24H() {
        log.info("Scheduler: verificando eventos próximas 24h...");
        try {
            final LocalDateTime ahora = LocalDateTime.now();
            final LocalDateTime desde = ahora.plusHours(HORAS_VENTANA_24H_INICIO);
            final LocalDateTime hasta = ahora.plusHours(HORAS_VENTANA_24H_FIN);
            misEventosService.generarAlertasParaRango(desde, hasta, TipoAlerta.PROXIMA_24H);
        } catch (Exception e) {
            log.error("Error generando alertas 24h: {}", e.getMessage(), e);
        }
    }

    /** Limpia alertas leídas con más de 7 días de antigüedad. Se ejecuta una vez al día. */
    @Scheduled(cron = "0 0 3 * * *") // 3:00 AM cada día
    public void limpiarAlertasAntiguas() {
        log.info("Scheduler: limpiando alertas antiguas leídas...");
        try {
            // La lógica de limpieza se delega al repositorio desde el servicio
            misEventosService.limpiarAlertasAntiguas();
        } catch (Exception e) {
            log.error("Error limpiando alertas antiguas: {}", e.getMessage(), e);
        }
    }
}
