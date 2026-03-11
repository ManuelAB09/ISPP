package es.us.meerkat.backend.service;

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
}
