package es.us.meerkat.backend.scheduler;

import java.time.LocalDate;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import es.us.meerkat.backend.entity.SolicitudContratacionDirecta;
import es.us.meerkat.backend.repository.SolicitudContratacionDirectaRepository;
import es.us.meerkat.backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Tarea programada que envía recordatorios por email a alumnos y tutores 24 horas antes de sus
 * clases reservadas.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingReminderScheduler {

    private final SolicitudContratacionDirectaRepository solicitudRepository;
    private final EmailService emailService;

    /** Se ejecuta todos los días a las 10:00. */
    @Scheduled(cron = "0 0 10 * * *")
    public void enviarRecordatorios() {
        LocalDate manana = LocalDate.now().plusDays(1);
        List<SolicitudContratacionDirecta> reservas =
                solicitudRepository.findBookingsForDate(manana);

        log.info("Enviando recordatorios para {} reservas del {}", reservas.size(), manana);

        for (SolicitudContratacionDirecta reserva : reservas) {
            try {
                String modalidad =
                        reserva.getModalidad() != null ? reserva.getModalidad() : "ONLINE";
                String tutorNombre = reserva.getTutor().getUsuario().getNombre();

                // Recordatorio al alumno
                emailService.sendBookingReminderEmail(
                        reserva.getAlumno().getEmail(),
                        reserva.getAlumno().getNombre(),
                        tutorNombre,
                        reserva.getDia(),
                        reserva.getHoraInicio(),
                        reserva.getHoraFin(),
                        modalidad);

                // Recordatorio al tutor
                emailService.sendBookingReminderEmail(
                        reserva.getTutor().getUsuario().getEmail(),
                        tutorNombre,
                        reserva.getAlumno().getNombre(),
                        reserva.getDia(),
                        reserva.getHoraInicio(),
                        reserva.getHoraFin(),
                        modalidad);

            } catch (Exception e) {
                log.error(
                        "Error enviando recordatorio para reserva {}: {}",
                        reserva.getId(),
                        e.getMessage());
            }
        }
    }
}
