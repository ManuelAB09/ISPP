package es.us.meerkat.backend.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    @Value("${spring.mail.from:meerkattersauth@gmail.com}")
    private String from;

    @Value("${app.name:Meerkat}")
    private String appName;

    private final JavaMailSender mailSender;

    public void sendPasswordResetEmail(
            final String to, final String userName, final String temporaryPassword)
            throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        String subject = appName + " - Recuperación de contraseña";
        String htmlContent = buildPasswordResetHtmlEmail(userName, temporaryPassword);

        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        try {
            mailSender.send(message);
            log.info("Email de recuperación de contraseña enviado a: {}", to);
        } catch (final Exception e) {
            log.error("Error al enviar email de recuperación a {}: {}", to, e.getMessage());
            throw e;
        }
    }

    private String buildPasswordResetHtmlEmail(
            final String userName, final String temporaryPassword) {
        return "<html>"
                + "<head>"
                + "<style>"
                + "body { font-family: Arial, sans-serif; color: #333; }"
                + ".container { max-width: 600px; margin: 0 auto; padding: 20px; }"
                + ".header { background-color: #2E75B6; color: white; padding: 20px; "
                + "text-align: center; border-radius: 5px 5px 0 0; }"
                + ".content { background-color: #f9f9f9; padding: 20px; border: 1px solid #ddd; }"
                + ".footer { background-color: #f0f0f0; padding: 15px; text-align: "
                + "center; font-size: 12px; border-radius: 0 0 5px 5px; }"
                + ".password-box { background-color: #e8f4f8; "
                + "padding: 15px; border-left: 4px solid #2E75B6;"
                + " margin: 20px 0; font-family: monospace; font-size: 14px; }"
                + ".warning { background-color: #fff3cd; padding: 10px; border-left:"
                + " 4px solid #ffc107; margin: 15px 0; }"
                + "</style>"
                + "</head>"
                + "<body>"
                + "<div class='container'>"
                + "<div class='header'>"
                + "<h1>"
                + appName
                + " - Recuperación de Contraseña</h1>"
                + "</div>"
                + "<div class='content'>"
                + "<p>Hola <strong>"
                + userName
                + "</strong>,</p>"
                + "<p>Hemos recibido una solicitud para recuperar tu contraseña."
                + "Si fuiste tú, utiliza la contraseña temporal a continuación:</p>"
                + "<div class='password-box'>"
                + "<strong>Contraseña temporal:</strong><br>"
                + temporaryPassword
                + "</div>"
                + "<div class='warning'>"
                + "<strong>⚠️ Importante:</strong>"
                + "<ul>"
                + "<li>Esta contraseña expirará en 24 horas.</li>"
                + "<li>No compartas esta contraseña con nadie.</li>"
                + "<li>Cambia tu contraseña lo antes posible después de acceder.</li>"
                + "</ul>"
                + "</div>"
                + "<p>Si no solicitaste esto, ignora el email. Tu cuenta permanece segura.</p>"
                + "</div>"
                + "<div class='footer'>"
                + "<p>&copy; "
                + appName
                + "</p>"
                + "</div>"
                + "</div>"
                + "</body>"
                + "</html>";
    }

    public void sendSimpleEmail(final String to, final String subject, final String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);

        try {
            mailSender.send(message);
            log.info("Email enviado a: {}", to);
        } catch (final Exception e) {
            log.error("Error al enviar email a {}: {}", to, e.getMessage());
            throw new RuntimeException("No se pudo enviar el email", e);
        }
    }

    /**
     * Envía un email de verificación de cuenta.
     *
     * @param to Email del destinatario
     * @param userName Nombre del usuario
     * @param verificationToken Token de verificación
     * @param verificationUrl URL base para la verificación
     */
    public void sendVerificationEmail(
            final String to,
            final String userName,
            final String verificationToken,
            final String verificationUrl)
            throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        String subject = appName + " - Verifica tu cuenta";
        String htmlContent =
                buildVerificationHtmlEmail(userName, verificationToken, verificationUrl);

        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        try {
            mailSender.send(message);
            log.info("Email de verificación enviado a: {}", to);
        } catch (final Exception e) {
            log.error("Error al enviar email de verificación a {}: {}", to, e.getMessage());
            throw e;
        }
    }

    private String buildVerificationHtmlEmail(
            final String userName, final String verificationToken, final String verificationUrl) {
        String fullVerificationUrl = verificationUrl + "?token=" + verificationToken;

        return "<html><head><style>body { font-family: Arial, sans-serif; color: #333; }.container"
                + " { max-width: 600px; margin: 0 auto; padding: 20px; }.header {"
                + " background-color: #2D3250; color: white; padding: 20px; text-align: center;"
                + " border-radius: 5px 5px 0 0; }.content { background-color: #f9f9f9; padding:"
                + " 20px; border: 1px solid #ddd; }.footer { background-color: #f0f0f0; padding:"
                + " 15px; text-align: center; font-size: 12px; border-radius: 0 0 5px 5px;"
                + " }.button { display: inline-block; background-color: #2D3250; color: white;"
                + " padding: 15px 30px; text-decoration: none; border-radius: 5px; font-weight:"
                + " bold; margin: 20px 0; }.button:hover { background-color: #1e2340; }.info {"
                + " background-color: #e8f4f8; padding: 15px; border-left: 4px solid #2D3250;"
                + " margin: 20px 0; }</style></head><body><div class='container'><div"
                + " class='header'><h1>¡Bienvenido a "
                + appName
                + "!</h1>"
                + "</div>"
                + "<div class='content'>"
                + "<p>Hola <strong>"
                + userName
                + "</strong>,</p>"
                + "<p>Gracias por registrarte en "
                + appName
                + ". Para completar tu registro y activar tu cuenta, por favor verifica tu"
                + " dirección de correo electrónico haciendo clic en el siguiente botón:</p><div"
                + " style='text-align: center;'><a href='"
                + fullVerificationUrl
                + "' class='button' style='color: white;'>Verificar mi cuenta</a></div><div"
                + " class='info'><strong>ℹ️ Información importante:</strong><ul><li>Este enlace"
                + " expirará en 24 horas.</li><li>Si no solicitaste esta cuenta, puedes ignorar"
                + " este email.</li></ul></div><p>Si el botón no funciona, copia y pega el"
                + " siguiente enlace en tu navegador:</p><p style='word-break: break-all;"
                + " font-size: 12px; color: #666;'>"
                + fullVerificationUrl
                + "</p>"
                + "</div>"
                + "<div class='footer'>"
                + "<p>&copy; "
                + appName
                + " - Universidad de Sevilla</p>"
                + "</div>"
                + "</div>"
                + "</body>"
                + "</html>";
    }

    /**
     * Envía un email de invitación a plan institucional.
     *
     * @param to Email del destinatario
     * @param institutionName Nombre de la institución
     * @param planName Nombre del plan corporativo
     * @param isNewAccount true si es una cuenta recién creada
     * @param verificationToken Token de verificación (solo para cuentas nuevas)
     * @param frontendUrl URL base del frontend
     */
    public void sendInstitutionInviteEmail(
            final String to,
            final String institutionName,
            final String planName,
            final boolean isNewAccount,
            final String verificationToken,
            final String frontendUrl)
            throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        String subject = appName + " - Plan institucional asignado";
        String htmlContent =
                buildInstitutionInviteHtmlEmail(
                        institutionName, planName, isNewAccount, verificationToken, frontendUrl);

        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        try {
            mailSender.send(message);
            log.info("Email de invitación institucional enviado a: {}", to);
        } catch (final Exception e) {
            log.error(
                    "Error al enviar email de invitación institucional a {}: {}",
                    to,
                    e.getMessage());
            throw e;
        }
    }

    private String buildInstitutionInviteHtmlEmail(
            final String institutionName,
            final String planName,
            final boolean isNewAccount,
            final String verificationToken,
            final String frontendUrl) {

        String actionSection;
        if (isNewAccount) {
            String verifyUrl = frontendUrl + "/verify-email?token=" + verificationToken;
            actionSection =
                    "<p>Se ha creado una cuenta en "
                            + appName
                            + " para ti. Para activarla, verifica tu email:</p>"
                            + "<div style='text-align: center;'>"
                            + "<a href='"
                            + verifyUrl
                            + "' style='display: inline-block; background-color: #2D3250;"
                            + " color: white; padding: 15px 30px; text-decoration: none;"
                            + " border-radius: 5px; font-weight: bold; margin: 20px 0;'>"
                            + "Verificar mi cuenta</a></div>"
                            + "<p style='word-break: break-all; font-size: 12px; color: #666;'>"
                            + verifyUrl
                            + "</p>";
        } else {
            String planesUrl = frontendUrl + "/planes";
            actionSection =
                    "<p>Tu cuenta ya está activa. Puedes ver los detalles de tu plan aquí:</p>"
                            + "<div style='text-align: center;'>"
                            + "<a href='"
                            + planesUrl
                            + "' style='display: inline-block; background-color: #2D3250;"
                            + " color: white; padding: 15px 30px; text-decoration: none;"
                            + " border-radius: 5px; font-weight: bold; margin: 20px 0;'>"
                            + "Ver mi plan</a></div>";
        }

        return "<html><head><style>"
                + "body { font-family: Arial, sans-serif; color: #333; }"
                + ".container { max-width: 600px; margin: 0 auto; padding: 20px; }"
                + ".header { background-color: #2D3250; color: white; padding: 20px;"
                + " text-align: center; border-radius: 5px 5px 0 0; }"
                + ".content { background-color: #f9f9f9; padding: 20px;"
                + " border: 1px solid #ddd; }"
                + ".footer { background-color: #f0f0f0; padding: 15px;"
                + " text-align: center; font-size: 12px; border-radius: 0 0 5px 5px; }"
                + ".plan-box { background-color: #e8f4f8; padding: 15px;"
                + " border-left: 4px solid #2D3250; margin: 20px 0; }"
                + "</style></head><body>"
                + "<div class='container'>"
                + "<div class='header'>"
                + "<h1>Plan Institucional Asignado</h1>"
                + "</div>"
                + "<div class='content'>"
                + "<p>¡Hola!</p>"
                + "<p>La institución <strong>"
                + institutionName
                + "</strong> te ha incluido en su plan institucional de "
                + appName
                + ".</p>"
                + "<div class='plan-box'>"
                + "<strong>Plan asignado:</strong> "
                + planName
                + "</div>"
                + actionSection
                + "</div>"
                + "<div class='footer'>"
                + "<p>&copy; "
                + appName
                + " - Universidad de Sevilla</p>"
                + "</div>"
                + "</div>"
                + "</body></html>";
    }

    // ─── Booking emails ─────────────────────────────────────────────

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    /** Email de confirmación de reserva pagada (al alumno y al tutor). */
    public void sendBookingConfirmationEmail(
            String alumnoEmail,
            String alumnoNombre,
            String tutorEmail,
            String tutorNombre,
            LocalDate dia,
            LocalTime horaInicio,
            LocalTime horaFin,
            String modalidad,
            java.math.BigDecimal importeTotal) {

        String fecha = dia.format(DATE_FMT);
        String horario = horaInicio.format(TIME_FMT) + " – " + horaFin.format(TIME_FMT);

        // Email al alumno
        String alumnoSubject = appName + " - Confirmación de reserva";
        String alumnoBody =
                "<html><body style='font-family:Arial,sans-serif;color:#333'><div"
                    + " style='max-width:600px;margin:0 auto;padding:20px'><div"
                    + " style='background:#2D3250;color:white;padding:20px;text-align:center;border-radius:5px"
                    + " 5px 0 0'><h1>Reserva confirmada ✓</h1></div><div"
                    + " style='background:#f9f9f9;padding:20px;border:1px solid #ddd'><p>Hola"
                    + " <strong>"
                        + alumnoNombre
                        + "</strong>,</p>"
                        + "<p>Tu clase con <strong>"
                        + tutorNombre
                        + "</strong> ha sido confirmada y pagada.</p><div"
                        + " style='background:#e8f4f8;padding:15px;border-left:4px solid"
                        + " #2D3250;margin:20px 0'><strong>Detalles de la reserva:</strong><br>📅"
                        + " Fecha: "
                        + fecha
                        + "<br>"
                        + "🕐 Horario: "
                        + horario
                        + "<br>"
                        + "📍 Modalidad: "
                        + modalidad
                        + "<br>"
                        + "💰 Importe: "
                        + importeTotal
                        + "€</div><p>Recibirás un recordatorio antes de la clase.</p></div><div"
                        + " style='background:#f0f0f0;padding:15px;text-align:center;font-size:12px;border-radius:0"
                        + " 0 5px 5px'><p>&copy; "
                        + appName
                        + "</p></div></div></body></html>";

        sendHtmlEmailSafe(alumnoEmail, alumnoSubject, alumnoBody);

        // Email al tutor
        String tutorSubject = appName + " - Nueva reserva confirmada";
        String tutorBody =
                "<html><body style='font-family:Arial,sans-serif;color:#333'><div"
                    + " style='max-width:600px;margin:0 auto;padding:20px'><div"
                    + " style='background:#2D3250;color:white;padding:20px;text-align:center;border-radius:5px"
                    + " 5px 0 0'><h1>Nueva clase reservada</h1></div><div"
                    + " style='background:#f9f9f9;padding:20px;border:1px solid #ddd'><p>Hola"
                    + " <strong>"
                        + tutorNombre
                        + "</strong>,</p>"
                        + "<p><strong>"
                        + alumnoNombre
                        + "</strong> ha confirmado y pagado una clase contigo.</p><div"
                        + " style='background:#e8f4f8;padding:15px;border-left:4px solid"
                        + " #2D3250;margin:20px 0'>📅 Fecha: "
                        + fecha
                        + "<br>"
                        + "🕐 Horario: "
                        + horario
                        + "<br>"
                        + "📍 Modalidad: "
                        + modalidad
                        + "<br>"
                        + "💰 Importe: "
                        + importeTotal
                        + "€</div></div><div"
                        + " style='background:#f0f0f0;padding:15px;text-align:center;font-size:12px;border-radius:0"
                        + " 0 5px 5px'><p>&copy; "
                        + appName
                        + "</p></div></div></body></html>";

        sendHtmlEmailSafe(tutorEmail, tutorSubject, tutorBody);
    }

    /** Email de cancelación de reserva (al alumno). */
    public void sendBookingCancellationEmail(
            String alumnoEmail,
            String alumnoNombre,
            String tutorNombre,
            LocalDate dia,
            LocalTime horaInicio,
            LocalTime horaFin,
            String motivo) {

        String fecha = dia.format(DATE_FMT);
        String horario = horaInicio.format(TIME_FMT) + " – " + horaFin.format(TIME_FMT);
        String motivoHtml =
                motivo != null && !motivo.isBlank()
                        ? "<p><strong>Motivo:</strong> " + motivo + "</p>"
                        : "";

        String subject = appName + " - Reserva cancelada";
        String body =
                "<html><body style='font-family:Arial,sans-serif;color:#333'><div"
                    + " style='max-width:600px;margin:0 auto;padding:20px'><div"
                    + " style='background:#c0392b;color:white;padding:20px;text-align:center;border-radius:5px"
                    + " 5px 0 0'><h1>Reserva cancelada</h1></div><div"
                    + " style='background:#f9f9f9;padding:20px;border:1px solid #ddd'><p>Hola"
                    + " <strong>"
                        + alumnoNombre
                        + "</strong>,</p>"
                        + "<p>Tu profesor <strong>"
                        + tutorNombre
                        + "</strong> ha cancelado la clase prevista para:</p><div"
                        + " style='background:#fdecea;padding:15px;border-left:4px solid"
                        + " #c0392b;margin:20px 0'>📅 "
                        + fecha
                        + "<br>"
                        + "🕐 "
                        + horario
                        + "</div>"
                        + motivoHtml
                        + "<p>Puedes solicitar una nueva clase desde el perfil del profesor.</p>"
                        + "</div><div"
                        + " style='background:#f0f0f0;padding:15px;text-align:center;font-size:12px;border-radius:0"
                        + " 0 5px 5px'><p>&copy; "
                        + appName
                        + "</p></div></div></body></html>";

        sendHtmlEmailSafe(alumnoEmail, subject, body);
    }

    /** Email de reprogramación de reserva (al alumno). */
    public void sendBookingRescheduledEmail(
            String alumnoEmail,
            String alumnoNombre,
            String tutorNombre,
            LocalDate diaAnterior,
            LocalTime horaInicioAnterior,
            LocalTime horaFinAnterior,
            LocalDate nuevoDia,
            LocalTime nuevaHoraInicio,
            LocalTime nuevaHoraFin) {

        String subject = appName + " - Reserva reprogramada";
        String body =
                "<html><body style='font-family:Arial,sans-serif;color:#333'><div"
                    + " style='max-width:600px;margin:0 auto;padding:20px'><div"
                    + " style='background:#e67e22;color:white;padding:20px;text-align:center;border-radius:5px"
                    + " 5px 0 0'><h1>Reserva reprogramada</h1></div><div"
                    + " style='background:#f9f9f9;padding:20px;border:1px solid #ddd'><p>Hola"
                    + " <strong>"
                        + alumnoNombre
                        + "</strong>,</p>"
                        + "<p>Tu profesor <strong>"
                        + tutorNombre
                        + "</strong> ha reprogramado tu clase:</p><div"
                        + " style='background:#fff3cd;padding:15px;border-left:4px solid"
                        + " #e67e22;margin:20px 0'><strong>Antes:</strong> "
                        + diaAnterior.format(DATE_FMT)
                        + " "
                        + horaInicioAnterior.format(TIME_FMT)
                        + " – "
                        + horaFinAnterior.format(TIME_FMT)
                        + "<br>"
                        + "<strong>Ahora:</strong> "
                        + nuevoDia.format(DATE_FMT)
                        + " "
                        + nuevaHoraInicio.format(TIME_FMT)
                        + " – "
                        + nuevaHoraFin.format(TIME_FMT)
                        + "</div></div><div"
                        + " style='background:#f0f0f0;padding:15px;text-align:center;font-size:12px;border-radius:0"
                        + " 0 5px 5px'><p>&copy; "
                        + appName
                        + "</p></div></div></body></html>";

        sendHtmlEmailSafe(alumnoEmail, subject, body);
    }

    /** Email de recordatorio 24h antes de la clase. */
    public void sendBookingReminderEmail(
            String email,
            String nombre,
            String tutorNombre,
            LocalDate dia,
            LocalTime horaInicio,
            LocalTime horaFin,
            String modalidad) {

        String fecha = dia.format(DATE_FMT);
        String horario = horaInicio.format(TIME_FMT) + " – " + horaFin.format(TIME_FMT);

        String subject = appName + " - Recordatorio: tu clase es mañana";
        String body =
                "<html><body style='font-family:Arial,sans-serif;color:#333'><div"
                    + " style='max-width:600px;margin:0 auto;padding:20px'><div"
                    + " style='background:#2D3250;color:white;padding:20px;text-align:center;border-radius:5px"
                    + " 5px 0 0'><h1>⏰ Recordatorio de clase</h1></div><div"
                    + " style='background:#f9f9f9;padding:20px;border:1px solid #ddd'><p>Hola"
                    + " <strong>"
                        + nombre
                        + "</strong>,</p><p>Te recordamos que mañana tienes una clase"
                        + " programada:</p><div"
                        + " style='background:#e8f4f8;padding:15px;border-left:4px solid"
                        + " #2D3250;margin:20px 0'>👨‍🏫 Profesor: "
                        + tutorNombre
                        + "<br>"
                        + "📅 Fecha: "
                        + fecha
                        + "<br>"
                        + "🕐 Horario: "
                        + horario
                        + "<br>"
                        + "📍 Modalidad: "
                        + modalidad
                        + "</div><p>¡Prepárate y no llegues tarde!</p></div><div"
                        + " style='background:#f0f0f0;padding:15px;text-align:center;font-size:12px;border-radius:0"
                        + " 0 5px 5px'><p>&copy; "
                        + appName
                        + "</p></div></div></body></html>";

        sendHtmlEmailSafe(email, subject, body);
    }

    private void sendHtmlEmailSafe(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email '{}' enviado a: {}", subject, to);
        } catch (Exception e) {
            log.error("Error al enviar email '{}' a {}: {}", subject, to, e.getMessage());
        }
    }
}
