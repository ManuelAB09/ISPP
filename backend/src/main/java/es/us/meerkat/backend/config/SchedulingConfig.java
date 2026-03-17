package es.us.meerkat.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuración para habilitar la ejecución de tareas programadas con {@code @Scheduled}.
 *
 * <p>Activa el scheduler de alertas automáticas ({@link
 * es.us.meerkat.backend.scheduler.AlertaScheduler}). Asegúrate de que esta clase esté en el paquete
 * base o en un subpaquete escaneado por Spring.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
    // No requiere configuración adicional.
    // Spring Boot usará el thread pool por defecto para las tareas @Scheduled.
    // Para producción con alto volumen, configura un TaskScheduler personalizado:
    //
    // @Bean
    // public TaskScheduler taskScheduler() {
    // ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    // scheduler.setPoolSize(3);
    // scheduler.setThreadNamePrefix("alerta-scheduler-");
    // return scheduler;
    // }
}
