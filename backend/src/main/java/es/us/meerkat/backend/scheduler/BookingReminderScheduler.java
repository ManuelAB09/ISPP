package es.us.meerkat.backend.scheduler;

import java.time.LocalDate;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import es.us.meerkat.backend.entity.EstadoSolicitudContratacion;
import es.us.meerkat.backend.entity.SolicitudContratacionDirecta;
import es.us.meerkat.backend.repository.tutors.SolicitudContratacionDirectaRepository;
import es.us.meerkat.backend.service.emails.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Tarea programada que envía recordatorios por email a alumnos y tutores 24 horas antes de sus
 * clases reservadas, y expira solicitudes aceptadas cuya fecha ya ha pasado.
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

    /**
     * Expira solicitudes aceptadas (pendientes de pago) cuya fecha de clase ya ha pasado. Se
     * ejecuta todos los días a las 01:00.
     */
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void expirarSolicitudesVencidas() {
        LocalDate ayer = LocalDate.now().minusDays(1);

        // Expirar solicitudes ACEPTADAS (pendientes de pago) vencidas
        List<SolicitudContratacionDirecta> aceptadasVencidas =
                solicitudRepository.findExpiredAcceptedBookings(ayer);

        // Expirar solicitudes PENDIENTES (sin respuesta del tutor) vencidas
        List<SolicitudContratacionDirecta> pendientesVencidas =
                solicitudRepository.findExpiredPendingBookings(ayer);

        int total = aceptadasVencidas.size() + pendientesVencidas.size();
        if (total == 0) {
            return;
        }

        log.info(
                "Expirando {} solicitudes vencidas ({} aceptadas, {} pendientes)",
                total,
                aceptadasVencidas.size(),
                pendientesVencidas.size());

        for (SolicitudContratacionDirecta solicitud : aceptadasVencidas) {
            solicitud.setEstado(EstadoSolicitudContratacion.CANCELADA_TUTOR);
            solicitud.setMotivoRechazo(
                    "Expirada automáticamente: la fecha de la clase pasó sin realizarse el pago.");
            solicitudRepository.save(solicitud);

            try {
                emailService.sendBookingExpiredEmail(
                        solicitud.getAlumno().getEmail(),
                        solicitud.getAlumno().getNombre(),
                        solicitud.getTutor().getUsuario().getNombre(),
                        solicitud.getDia(),
                        solicitud.getHoraInicio(),
                        solicitud.getHoraFin());
            } catch (Exception e) {
                log.warn(
                        "No se pudo enviar email de expiración para solicitud {}: {}",
                        solicitud.getId(),
                        e.getMessage());
            }
        }

        for (SolicitudContratacionDirecta solicitud : pendientesVencidas) {
            solicitud.setEstado(EstadoSolicitudContratacion.CANCELADA_TUTOR);
            solicitud.setMotivoRechazo(
                    "Expirada automáticamente: la fecha de la clase pasó sin respuesta.");
            solicitudRepository.save(solicitud);
        }
    }
}
