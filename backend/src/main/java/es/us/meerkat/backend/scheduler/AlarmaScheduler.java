package es.us.meerkat.backend.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import es.us.meerkat.backend.service.AlarmaPersonalizadaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduler que dispara las alarmas personalizadas de los usuarios.
 *
 * <p>Se ejecuta cada minuto y comprueba si hay alarmas cuya {@code fechaDisparo} ya ha llegado. A
 * diferencia del {@link RecordatorioEmailScheduler} (que trabaja con ventanas de tiempo), este
 * scheduler es exacto: cada alarma tiene una {@code fechaDisparo} calculada al crearla, y se
 * dispara en cuanto llega ese momento.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AlarmaScheduler {

    private final AlarmaPersonalizadaService alarmaPersonalizadaService;

    /** Comprueba y dispara alarmas pendientes. Se ejecuta cada minuto. */
    @Scheduled(fixedDelay = 60_000)
    public void dispararAlarmas() {
        log.info("AlarmaScheduler: comprobando alarmas pendientes...");
        try {
            alarmaPersonalizadaService.dispararAlarmasPendientes();
        } catch (Exception e) {
            log.error("Error en AlarmaScheduler: {}", e.getMessage(), e);
        }
    }

    /** Limpieza diaria de alarmas disparadas con más de 30 días. A las 4:30 AM. */
    @Scheduled(cron = "0 30 4 * * *")
    public void limpiezaDiaria() {
        log.info("AlarmaScheduler: limpiando alarmas antiguas...");
        try {
            alarmaPersonalizadaService.limpiarAlarmasAntiguas();
        } catch (Exception e) {
            log.error("Error en limpieza de alarmas: {}", e.getMessage(), e);
        }
    }
}
