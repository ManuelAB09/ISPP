package es.us.meerkat.backend.service.emails;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

import es.us.meerkat.backend.entity.AsistenciaEvento;
import es.us.meerkat.backend.entity.Comunidad;
import es.us.meerkat.backend.entity.EstadoAsistencia;
import es.us.meerkat.backend.entity.Evento;
import es.us.meerkat.backend.entity.TipoEvento;
import es.us.meerkat.backend.entity.TipoRecordatorio;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.AsistenciaEventoRepository;
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

    @Value("${app.url:http://localhost:3000}")
    private String appUrl;

    @Value("${sendgrid.api-key:}")
    private String sendgridApiKey;

    private final JavaMailSender mailSender;
    private final AsistenciaEventoRepository asistenciaEventoRepository;

    @SuppressWarnings("deprecation")
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", new Locale("es", "ES"));

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // ===============================
    // ENVÍO CENTRALIZADO (SMTP o SendGrid HTTP API)
    // ===============================

    private boolean useSendGridApi() {
        return sendgridApiKey != null && !sendgridApiKey.isBlank();
    }

    private void doSendHtml(String to, String subject, String htmlContent)
            throws MessagingException {
        if (useSendGridApi()) {
            sendViaSendGridApi(to, subject, htmlContent, true);
        } else {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
        }
    }

    private void doSendPlain(String to, String subject, String text) {
        if (useSendGridApi()) {
            sendViaSendGridApi(to, subject, text, false);
        } else {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
        }
    }

    private void sendViaSendGridApi(String to, String subject, String content, boolean isHtml) {
        Email fromEmail = new Email(from, appName);
        Email toEmail = new Email(to);
        String contentType = isHtml ? "text/html" : "text/plain";
        Content mailContent = new Content(contentType, content);
        Mail mail = new Mail(fromEmail, subject, toEmail, mailContent);

        SendGrid sg = new SendGrid(sendgridApiKey);
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);
            log.info("SendGrid API respondió {} para email a {}", response.getStatusCode(), to);
            if (response.getStatusCode() >= 400) {
                log.error("SendGrid API error body: {}", response.getBody());
                throw new RuntimeException("SendGrid API respondió " + response.getStatusCode());
            }
        } catch (IOException e) {
            log.error(
                    "SendGrid API I/O error para {}: [{}] {}",
                    to,
                    e.getClass().getSimpleName(),
                    e.getMessage());
            throw new RuntimeException("No se pudo enviar email vía SendGrid API", e);
        }
    }

    // ===============================
    // CÓDIGO EXISTENTE
    // ===============================

    public void sendPasswordResetEmail(
            final String to, final String userName, final String temporaryPassword)
            throws MessagingException {
        String subject = appName + " - Recuperación de contraseña";
        String htmlContent = buildPasswordResetHtmlEmail(userName, temporaryPassword);

        try {
            doSendHtml(to, subject, htmlContent);
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
        try {
            doSendPlain(to, subject, text);
            log.info("Email enviado a: {}", to);
        } catch (final Exception e) {
            log.error("Error al enviar email a {}: {}", to, e.getMessage());
            throw new RuntimeException("No se pudo enviar el email", e);
        }
    }

    // ===============================
    // RECORDATORIOS DE EVENTOS
    // ===============================

    /** Envía un email de recordatorio global (24h, 1h, 30min) para un evento. */
    public void enviarRecordatorio(
            final Usuario usuario, final Evento evento, final TipoRecordatorio tipo)
            throws Exception {

        final String html = buildHtml(evento, tipo.getDescripcion());
        final String asunto = buildAsunto(evento, tipo.getDescripcion());
        enviarMime(usuario.getEmail(), asunto, html);
        log.info(
                "Email recordatorio [{}] enviado a {} para evento {}",
                tipo,
                usuario.getEmail(),
                evento.getId());
    }

    /**
     * Envía un email de recordatorio para una alarma personalizada. Usa la etiqueta libre de la
     * alarma ("2 días antes", "1 hora antes", etc.)
     */
    public void enviarRecordatorioAlarma(
            final Usuario usuario, final Evento evento, final String etiqueta) throws Exception {

        final String html = buildHtml(evento, etiqueta);
        final String asunto = buildAsunto(evento, etiqueta);
        enviarMime(usuario.getEmail(), asunto, html);
        log.info(
                "Email alarma personalizada [{}] enviado a {} para evento {}",
                etiqueta,
                usuario.getEmail(),
                evento.getId());
    }

    // ===============================
    // CONSTRUCCIÓN DEL HTML
    // ===============================

    /**
     * Método central que construye el HTML del email para cualquier tipo de recordatorio. Aplica
     * las dos reglas clave: - Si esVirtual=true → se muestra el enlace y NO se muestra la ubicación
     * física. - Los asistentes se listan como filas con inicial + nombre completo.
     */
    private String buildHtml(final Evento evento, final String etiqueta) throws Exception {
        final ClassPathResource resource =
                new ClassPathResource("templates/email-recordatorio.html");
        String html = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

        final TipoEvento tipoEvento =
                evento.getTipoEvento() != null ? evento.getTipoEvento() : TipoEvento.OTRO;

        // ── Asistentes confirmados ──────────────────────────────────────────
        final List<AsistenciaEvento> asistencias =
                asistenciaEventoRepository.findByEventoIdAndEstado(
                        evento.getId(), EstadoAsistencia.CONFIRMADA);

        final String horaInicio =
                evento.getFechaHora() != null
                        ? evento.getFechaHora().format(TIME_FORMATTER)
                        : "--:--";
        final String horaFin =
                evento.getFechaFin() != null
                        ? evento.getFechaFin().format(TIME_FORMATTER)
                        : "--:--";
        final String fechaFormateada =
                evento.getFechaHora() != null ? evento.getFechaHora().format(DATE_FORMATTER) : "";

        // ── Placeholders simples ────────────────────────────────────────────
        html =
                html.replace("{{icono}}", tipoEvento.getIcono())
                        .replace("{{tipoRecordatorioTexto}}", escapeHtml(etiqueta))
                        .replace("{{tituloEvento}}", escapeHtml(evento.getTitulo()))
                        .replace("{{tipoEventoNombre}}", tipoEvento.getNombre())
                        .replace(
                                "{{comunidadNombre}}",
                                evento.getComunidad() != null
                                        ? escapeHtml(evento.getComunidad().getNombre())
                                        : "")
                        .replace("{{fechaFormateada}}", fechaFormateada)
                        .replace("{{horaInicio}}", horaInicio)
                        .replace("{{horaFin}}", horaFin)
                        .replace("{{totalAsistentes}}", String.valueOf(asistencias.size()));

        // ── Bloque virtual / ubicación (mutuamente excluyentes) ─────────────
        //
        // REGLA: si esVirtual=true → mostrar aviso de acceso por plataforma,
        // NUNCA mostrar la ubicación física.
        // si esVirtual=false → mostrar ubicación si existe, nunca el bloque virtual.
        //
        if (Boolean.TRUE.equals(evento.getEsVirtual())) {
            // Mostrar bloque virtual con enlace a "Mis eventos" en la plataforma
            html = html.replace("{{#esVirtual}}", "").replace("{{/esVirtual}}", "");
            // Siempre ocultar ubicación física en eventos virtuales
            html = removeBlock(html, "{{#tieneUbicacion}}", "{{/tieneUbicacion}}");

        } else {
            // Evento presencial: ocultar bloque virtual
            html = removeBlock(html, "{{#esVirtual}}", "{{/esVirtual}}");

            // Mostrar ubicación física si existe
            if (evento.getUbicacion() != null) {
                final String direccion =
                        evento.getUbicacion().getDireccion() != null
                                ? evento.getUbicacion().getDireccion()
                                : "";
                html =
                        html.replace("{{#tieneUbicacion}}", "")
                                .replace("{{/tieneUbicacion}}", "")
                                .replace(
                                        "{{ubicacionNombre}}",
                                        escapeHtml(evento.getUbicacion().getNombre()))
                                .replace("{{#ubicacionDireccion}}", "")
                                .replace("{{/ubicacionDireccion}}", "")
                                .replace("{{ubicacionDireccion}}", escapeHtml(direccion));
            } else {
                html = removeBlock(html, "{{#tieneUbicacion}}", "{{/tieneUbicacion}}");
            }
        }

        // ── Qué llevar ──────────────────────────────────────────────────────
        if (evento.getQueLlevar() != null && !evento.getQueLlevar().isBlank()) {
            html =
                    html.replace("{{#tieneQueLlevar}}", "")
                            .replace("{{/tieneQueLlevar}}", "")
                            .replace("{{queLlevar}}", escapeHtml(evento.getQueLlevar()));
        } else {
            html = removeBlock(html, "{{#tieneQueLlevar}}", "{{/tieneQueLlevar}}");
        }

        // ── Descripción ─────────────────────────────────────────────────────
        if (evento.getDescripcion() != null && !evento.getDescripcion().isBlank()) {
            html =
                    html.replace("{{#tieneDescripcion}}", "")
                            .replace("{{/tieneDescripcion}}", "")
                            .replace("{{descripcion}}", escapeHtml(evento.getDescripcion()));
        } else {
            html = removeBlock(html, "{{#tieneDescripcion}}", "{{/tieneDescripcion}}");
        }

        // ── Lista de asistentes ─────────────────────────────────────────────
        // Cada asistente se renderiza como una fila: avatar con inicial + nombre
        // completo.
        if (!asistencias.isEmpty()) {
            final StringBuilder filasHtml = new StringBuilder();
            for (final AsistenciaEvento ae : asistencias) {
                final String nombre = ae.getUsuario().getNombre();
                final String inicial =
                        nombre != null && !nombre.isBlank()
                                ? String.valueOf(nombre.charAt(0)).toUpperCase()
                                : "?";
                filasHtml
                        .append("<div class=\"attendee-row\">")
                        .append("<div class=\"attendee-avatar\">")
                        .append(escapeHtml(inicial))
                        .append("</div>")
                        .append("<span class=\"attendee-name\">")
                        .append(escapeHtml(nombre))
                        .append("</span>")
                        .append("</div>");
            }

            html =
                    html.replace("{{#tieneAsistentes}}", "")
                            .replace("{{/tieneAsistentes}}", "")
                            .replace("{{#asistentes}}", "")
                            .replace("{{/asistentes}}", "")
                            // El placeholder {{nombre}} e {{inicial}} ya están sustituidos dentro
                            // del loop
                            .replace("{{inicial}}", "") // limpiar si queda alguno sin reemplazar
                            .replace("{{nombre}}", "");

            // Insertar las filas generadas en lugar del bloque de asistentes vacío
            // Para esto buscamos el contenedor y reemplazamos su contenido
            html =
                    html.replace(
                            "<div class=\"attendees-box\">\n        \n        </div>",
                            "<div class=\"attendees-box\">" + filasHtml + "</div>");

            // Fallback más robusto: sustituir directamente el bloque completo
            // El bloque ya tiene las filas porque las pusimos con replace anterior,
            // así que simplemente limpiamos el bloque sinAsistentes
            html = removeBlock(html, "{{#sinAsistentes}}", "{{/sinAsistentes}}");

        } else {
            // Sin asistentes: quitar el bloque tieneAsistentes y mostrar mensaje vacío
            html = removeBlock(html, "{{#tieneAsistentes}}", "{{/tieneAsistentes}}");
            html = html.replace("{{#sinAsistentes}}", "").replace("{{/sinAsistentes}}", "");
        }

        return html;
    }

    // ===============================
    // HELPERS
    // ===============================

    private String buildAsunto(final Evento evento, final String etiqueta) {
        final TipoEvento tipo =
                evento.getTipoEvento() != null ? evento.getTipoEvento() : TipoEvento.OTRO;
        return String.format(
                "%s %s — %s (%s)",
                tipo.getIcono(),
                evento.getTitulo(),
                etiqueta,
                evento.getFechaHora() != null ? evento.getFechaHora().format(TIME_FORMATTER) : "");
    }

    private void enviarMime(final String to, final String asunto, final String html)
            throws Exception {
        doSendHtml(to, asunto, html);
    }

    private String removeBlock(final String html, final String open, final String close) {
        int start = html.indexOf(open);
        int end = html.indexOf(close);
        if (start == -1 || end == -1) {
            return html;
        }

        return html.substring(0, start) + html.substring(end + close.length());
    }

    private String escapeHtml(final String input) {
        if (input == null) {
            return "";
        }

        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
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
        String subject = appName + " - Verifica tu cuenta";
        String htmlContent =
                buildVerificationHtmlEmail(userName, verificationToken, verificationUrl);

        try {
            doSendHtml(to, subject, htmlContent);
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
        String subject = appName + " - Plan institucional asignado";
        String htmlContent =
                buildInstitutionInviteHtmlEmail(
                        institutionName, planName, isNewAccount, verificationToken, frontendUrl);

        try {
            doSendHtml(to, subject, htmlContent);
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

    /** Email al alumno confirmando que ÉL ha cancelado la clase. */
    public void sendAlumnoCancelledConfirmationEmail(
            String alumnoEmail,
            String alumnoNombre,
            String tutorNombre,
            LocalDate dia,
            LocalTime horaInicio,
            LocalTime horaFin,
            String motivo) {

        String fecha = dia.format(DATE_FMT);
        String horario = horaInicio.format(TIME_FMT) + " \u2013 " + horaFin.format(TIME_FMT);
        String motivoHtml =
                motivo != null && !motivo.isBlank()
                        ? "<p><strong>Motivo:</strong> " + motivo + "</p>"
                        : "";

        String subject = appName + " - Has cancelado tu clase";
        String body =
                "<html><body style='font-family:Arial,sans-serif;color:#333'><div"
                    + " style='max-width:600px;margin:0 auto;padding:20px'><div"
                    + " style='background:#c0392b;color:white;padding:20px;text-align:center;border-radius:5px"
                    + " 5px 0 0'><h1>Clase cancelada</h1></div><div"
                    + " style='background:#f9f9f9;padding:20px;border:1px solid #ddd'><p>Hola"
                    + " <strong>"
                        + alumnoNombre
                        + "</strong>,</p>"
                        + "<p>Has cancelado tu clase con <strong>"
                        + tutorNombre
                        + "</strong> prevista para:</p><div"
                        + " style='background:#fdecea;padding:15px;border-left:4px solid"
                        + " #c0392b;margin:20px 0'>\uD83D\uDCC5 "
                        + fecha
                        + "<br>"
                        + "\uD83D\uDD50 "
                        + horario
                        + "</div>"
                        + motivoHtml
                        + "<p>Puedes reservar una nueva clase cuando quieras.</p></div><div"
                        + " style='background:#f0f0f0;padding:15px;text-align:center;font-size:12px;border-radius:0"
                        + " 0 5px 5px'><p>&copy; "
                        + appName
                        + "</p></div></div></body></html>";

        sendHtmlEmailSafe(alumnoEmail, subject, body);
    }

    /** Email al tutor notificando que su alumno ha cancelado la clase. */
    public void sendTutorNotificationAlumnoCancelledEmail(
            String tutorEmail,
            String tutorNombre,
            String alumnoNombre,
            LocalDate dia,
            LocalTime horaInicio,
            LocalTime horaFin,
            String motivo) {

        String fecha = dia.format(DATE_FMT);
        String horario = horaInicio.format(TIME_FMT) + " \u2013 " + horaFin.format(TIME_FMT);
        String motivoHtml =
                motivo != null && !motivo.isBlank()
                        ? "<p><strong>Motivo:</strong> " + motivo + "</p>"
                        : "";

        String subject = appName + " - Tu alumno ha cancelado la clase";
        String body =
                "<html><body style='font-family:Arial,sans-serif;color:#333'><div"
                    + " style='max-width:600px;margin:0 auto;padding:20px'><div"
                    + " style='background:#c0392b;color:white;padding:20px;text-align:center;border-radius:5px"
                    + " 5px 0 0'><h1>Clase cancelada por el alumno</h1></div><div"
                    + " style='background:#f9f9f9;padding:20px;border:1px solid #ddd'><p>Hola"
                    + " <strong>"
                        + tutorNombre
                        + "</strong>,</p>"
                        + "<p>Tu alumno <strong>"
                        + alumnoNombre
                        + "</strong> ha cancelado la clase prevista para:</p><div"
                        + " style='background:#fdecea;padding:15px;border-left:4px solid"
                        + " #c0392b;margin:20px 0'>\uD83D\uDCC5 "
                        + fecha
                        + "<br>"
                        + "\uD83D\uDD50 "
                        + horario
                        + "</div>"
                        + motivoHtml
                        + "</div><div"
                        + " style='background:#f0f0f0;padding:15px;text-align:center;font-size:12px;border-radius:0"
                        + " 0 5px 5px'><p>&copy; "
                        + appName
                        + "</p></div></div></body></html>";

        sendHtmlEmailSafe(tutorEmail, subject, body);
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

    /** Email cuando una solicitud aceptada expira sin pago. */
    public void sendBookingExpiredEmail(
            String email,
            String alumnoNombre,
            String tutorNombre,
            LocalDate dia,
            LocalTime horaInicio,
            LocalTime horaFin) {

        String fecha = dia.format(DATE_FMT);
        String horario = horaInicio.format(TIME_FMT) + " – " + horaFin.format(TIME_FMT);

        String subject = appName + " - Solicitud de clase expirada";
        String body =
                "<html><body style='font-family:Arial,sans-serif;color:#333'><div"
                    + " style='max-width:600px;margin:0 auto;padding:20px'><div"
                    + " style='background:#2D3250;color:white;padding:20px;text-align:center;border-radius:5px"
                    + " 5px 0 0'><h1>⏰ Solicitud expirada</h1></div><div"
                    + " style='background:#f9f9f9;padding:20px;border:1px solid #ddd'><p>Hola"
                    + " <strong>"
                        + alumnoNombre
                        + "</strong>,</p><p>Tu solicitud de clase con <strong>"
                        + tutorNombre
                        + "</strong> ha expirado porque no se realizó el pago antes de la"
                        + " fecha de la clase:</p><div"
                        + " style='background:#fde8e8;padding:15px;border-left:4px solid"
                        + " #e53e3e;margin:20px 0'>"
                        + "📅 Fecha: "
                        + fecha
                        + "<br>"
                        + "🕐 Horario: "
                        + horario
                        + "</div><p>Si deseas reprogramar, puedes enviar una nueva solicitud al"
                        + " profesor.</p></div><div"
                        + " style='background:#f0f0f0;padding:15px;text-align:center;font-size:12px;border-radius:0"
                        + " 0 5px 5px'><p>&copy; "
                        + appName
                        + "</p></div></div></body></html>";

        sendHtmlEmailSafe(email, subject, body);
    }

    /**
     * Email cuando un evento al que el usuario está apuntado ha sido modificado.
     *
     * @param usuario destinatario con asistencia confirmada.
     * @param evento evento actualizado.
     */
    public void sendEventUpdatedEmail(final Usuario usuario, final Evento evento) {
        if (usuario == null || usuario.getEmail() == null || evento == null) {
            return;
        }

        final String nombreUsuario = usuario.getNombre() != null ? usuario.getNombre() : "";
        final String fecha =
                evento.getFechaHora() != null ? evento.getFechaHora().format(DATE_FORMATTER) : "";
        final String horaInicio =
                evento.getFechaHora() != null
                        ? evento.getFechaHora().format(TIME_FORMATTER)
                        : "--:--";
        final String horaFin =
                evento.getFechaFin() != null
                        ? evento.getFechaFin().format(TIME_FORMATTER)
                        : "--:--";
        final String comunidad =
                evento.getComunidad() != null && evento.getComunidad().getNombre() != null
                        ? evento.getComunidad().getNombre()
                        : "Comunidad";
        final String modalidad =
                Boolean.TRUE.equals(evento.getEsVirtual()) ? "Online" : "Presencial";
        final String ubicacion =
                !Boolean.TRUE.equals(evento.getEsVirtual()) && evento.getUbicacion() != null
                        ? evento.getUbicacion().getNombre()
                        : "";

        final String subject = "🔄 Evento actualizado: " + escapeHtml(evento.getTitulo());
        final String body =
                "<html><body style='font-family:Arial,sans-serif;color:#333'><div"
                    + " style='max-width:600px;margin:0 auto;padding:20px'><div"
                    + " style='background:#2D3250;color:white;padding:20px;text-align:center;border-radius:5px"
                    + " 5px 0 0'><h1>Evento actualizado</h1></div><div"
                    + " style='background:#f9f9f9;padding:20px;border:1px solid #ddd'><p>Hola"
                    + " <strong>"
                        + escapeHtml(nombreUsuario)
                        + "</strong>,</p><p>Se han aplicado cambios en un evento al que estás"
                        + " apuntado:</p><div"
                        + " style='background:#e8f4f8;padding:15px;border-left:4px solid"
                        + " #2D3250;margin:20px 0'><strong>"
                        + escapeHtml(evento.getTitulo())
                        + "</strong><br>"
                        + "🏠 Comunidad: "
                        + escapeHtml(comunidad)
                        + "<br>"
                        + "📅 Fecha: "
                        + fecha
                        + "<br>"
                        + "🕐 Hora: "
                        + horaInicio
                        + " – "
                        + horaFin
                        + "<br>"
                        + "💻 Modalidad: "
                        + modalidad
                        + (ubicacion != null && !ubicacion.isBlank()
                                ? "<br>📍 Ubicación: " + escapeHtml(ubicacion)
                                : "")
                        + "</a></p></div><div"
                        + " style='background:#f0f0f0;padding:15px;text-align:center;font-size:12px;border-radius:0"
                        + " 0 5px 5px'><p>&copy; "
                        + appName
                        + "</p></div></div></body></html>";

        sendHtmlEmailSafe(usuario.getEmail(), subject, body);
    }

    /**
     * Email al dueño de una comunidad cuando un usuario solicita acceso a una comunidad privada.
     *
     * @param dueno destinatario (creador de la comunidad).
     * @param comunidad comunidad privada donde se solicita acceso.
     * @param solicitante usuario que solicita acceso.
     * @param mensaje mensaje opcional adjunto a la solicitud.
     */
    public void sendCommunityAccessRequestEmail(
            final Usuario dueno,
            final Comunidad comunidad,
            final Usuario solicitante,
            final String mensaje) {
        if (dueno == null || dueno.getEmail() == null || comunidad == null || solicitante == null) {
            return;
        }

        final String nombreDueno = dueno.getNombre() != null ? dueno.getNombre() : "";
        final String nombreSolicitante =
                solicitante.getNombre() != null ? solicitante.getNombre() : "Usuario";
        final String nombreComunidad =
                comunidad.getNombre() != null ? comunidad.getNombre() : "tu comunidad";

        final String mensajeHtml =
                mensaje != null && !mensaje.isBlank()
                        ? "<p style='margin-top:12px'><strong>Mensaje:</strong><br>"
                                + escapeHtml(mensaje)
                                + "</p>"
                        : "";

        final String subject = "📩 Nueva solicitud de acceso: " + nombreComunidad;
        final String body =
                "<html><body style='font-family:Arial,sans-serif;color:#333'><div"
                    + " style='max-width:600px;margin:0 auto;padding:20px'><div"
                    + " style='background:#2D3250;color:white;padding:20px;text-align:center;border-radius:5px"
                    + " 5px 0 0'><h1>Nueva solicitud de acceso</h1></div><div"
                    + " style='background:#f9f9f9;padding:20px;border:1px solid #ddd'><p>Hola"
                    + " <strong>"
                        + escapeHtml(nombreDueno)
                        + "</strong>,</p><p>"
                        + escapeHtml(nombreSolicitante)
                        + " ha solicitado unirse a la comunidad <strong>"
                        + escapeHtml(nombreComunidad)
                        + "</strong>.</p>"
                        + mensajeHtml
                        + "<p style='margin-top:16px'>Puedes revisar y responder la solicitud desde"
                        + " la pantalla de la comunidad.</p></div><div"
                        + " style='background:#f0f0f0;padding:15px;text-align:center;font-size:12px;border-radius:0"
                        + " 0 5px 5px'><p>&copy; "
                        + appName
                        + "</p></div></div></body></html>";

        sendHtmlEmailSafe(dueno.getEmail(), subject, body);
    }

    /**
     * Email a un miembro de comunidad cuando se publica un nuevo mensaje en el chat.
     *
     * @param destinatario usuario miembro que recibe el aviso.
     * @param comunidad comunidad donde se ha publicado el mensaje.
     * @param remitente usuario que publicó el mensaje.
     * @param contenido contenido del mensaje.
     */
    public void sendCommunityMessageEmail(
            final Usuario destinatario,
            final Comunidad comunidad,
            final Usuario remitente,
            final String contenido) {
        if (destinatario == null
                || destinatario.getEmail() == null
                || destinatario.getEmail().isBlank()
                || comunidad == null
                || remitente == null) {
            return;
        }

        final String nombreDestinatario =
                destinatario.getNombre() != null ? destinatario.getNombre() : "";
        final String nombreComunidad =
                comunidad.getNombre() != null ? comunidad.getNombre() : "tu comunidad";
        final String nombreRemitente =
                remitente.getNombre() != null ? remitente.getNombre() : "Un miembro";
        final String vistaPrevia =
                contenido != null && !contenido.isBlank()
                        ? (contenido.length() > 220
                                ? contenido.substring(0, 220) + "..."
                                : contenido)
                        : "Se ha publicado un nuevo mensaje en la comunidad.";

        final String subject = "💬 Nuevo mensaje en comunidad: " + nombreComunidad;
        final String body =
                "<html><body style='font-family:Arial,sans-serif;color:#333'><div"
                    + " style='max-width:600px;margin:0 auto;padding:20px'><div"
                    + " style='background:#2D3250;color:white;padding:20px;text-align:center;border-radius:5px"
                    + " 5px 0 0'><h1>Nuevo mensaje en tu comunidad</h1></div><div"
                    + " style='background:#f9f9f9;padding:20px;border:1px solid #ddd'><p>Hola"
                    + " <strong>"
                        + escapeHtml(nombreDestinatario)
                        + "</strong>,</p><p><strong>"
                        + escapeHtml(nombreRemitente)
                        + "</strong> ha enviado un mensaje en <strong>"
                        + escapeHtml(nombreComunidad)
                        + "</strong>.</p><div"
                        + " style='background:#eef2ff;padding:14px;border-left:4px solid"
                        + " #2D3250;margin:18px 0;white-space:pre-wrap'>"
                        + escapeHtml(vistaPrevia)
                        + "</div><p>Entra en la comunidad para leer y responder en el"
                        + " chat.</p></div><div"
                        + " style='background:#f0f0f0;padding:15px;text-align:center;font-size:12px;border-radius:0"
                        + " 0 5px 5px'><p>&copy; "
                        + appName
                        + "</p></div></div></body></html>";

        sendHtmlEmailSafe(destinatario.getEmail(), subject, body);
    }

    /**
     * Email a un miembro de comunidad cuando se le menciona explícitamente en el chat.
     *
     * @param destinatario usuario miembro que recibe el aviso.
     * @param comunidad comunidad donde ocurrió la mención.
     * @param remitente usuario que escribió la mención.
     * @param contenido contenido del mensaje.
     */
    public void sendCommunityMentionEmail(
            final Usuario destinatario,
            final Comunidad comunidad,
            final Usuario remitente,
            final String contenido) {
        if (destinatario == null
                || destinatario.getEmail() == null
                || destinatario.getEmail().isBlank()
                || comunidad == null
                || remitente == null) {
            return;
        }

        final String nombreDestinatario =
                destinatario.getNombre() != null ? destinatario.getNombre() : "";
        final String nombreComunidad =
                comunidad.getNombre() != null ? comunidad.getNombre() : "tu comunidad";
        final String nombreRemitente =
                remitente.getNombre() != null ? remitente.getNombre() : "Un miembro";
        final String vistaPrevia =
                contenido != null && !contenido.isBlank()
                        ? (contenido.length() > 220
                                ? contenido.substring(0, 220) + "..."
                                : contenido)
                        : "Te han mencionado en un mensaje de comunidad.";

        final String subject = "🔔 Te han mencionado en: " + nombreComunidad;
        final String body =
                "<html><body style='font-family:Arial,sans-serif;color:#333'><div"
                    + " style='max-width:600px;margin:0 auto;padding:20px'><div"
                    + " style='background:#2D3250;color:white;padding:20px;text-align:center;border-radius:5px"
                    + " 5px 0 0'><h1>Te han mencionado en tu comunidad</h1></div><div"
                    + " style='background:#f9f9f9;padding:20px;border:1px solid #ddd'><p>Hola"
                    + " <strong>"
                        + escapeHtml(nombreDestinatario)
                        + "</strong>,</p><p><strong>"
                        + escapeHtml(nombreRemitente)
                        + "</strong> te ha mencionado en <strong>"
                        + escapeHtml(nombreComunidad)
                        + "</strong>.</p><div"
                        + " style='background:#eef2ff;padding:14px;border-left:4px solid"
                        + " #2D3250;margin:18px 0;white-space:pre-wrap'>"
                        + escapeHtml(vistaPrevia)
                        + "</div><p>Entra en la comunidad para responder en el chat.</p></div><div"
                        + " style='background:#f0f0f0;padding:15px;text-align:center;font-size:12px;border-radius:0"
                        + " 0 5px 5px'><p>&copy; "
                        + appName
                        + "</p></div></div></body></html>";

        sendHtmlEmailSafe(destinatario.getEmail(), subject, body);
    }

    /**
     * Email a un miembro de comunidad cuando se publica un nuevo anuncio.
     *
     * @param destinatario usuario miembro que recibe el aviso.
     * @param comunidad comunidad donde se publica el anuncio.
     * @param autor usuario que publica el anuncio.
     * @param anuncio anuncio publicado.
     */
    public void sendCommunityAnnouncementEmail(
            final Usuario destinatario,
            final Comunidad comunidad,
            final Usuario autor,
            final es.us.meerkat.backend.entity.Anuncio anuncio) {
        if (destinatario == null
                || destinatario.getEmail() == null
                || destinatario.getEmail().isBlank()
                || comunidad == null
                || anuncio == null) {
            return;
        }

        final String nombreDestinatario =
                destinatario.getNombre() != null ? destinatario.getNombre() : "";
        final String nombreComunidad =
                comunidad.getNombre() != null ? comunidad.getNombre() : "tu comunidad";
        final String nombreAutor =
                autor != null && autor.getNombre() != null ? autor.getNombre() : "Un administrador";
        final String titulo = anuncio.getTitulo() != null ? anuncio.getTitulo() : "Nuevo anuncio";
        final String contenido =
                anuncio.getContenido() != null && !anuncio.getContenido().isBlank()
                        ? anuncio.getContenido()
                        : "Hay un nuevo anuncio disponible en la comunidad.";

        final String vistaPrevia =
                contenido.length() > 220 ? contenido.substring(0, 220) + "..." : contenido;

        final String subject = "📢 Nuevo anuncio en comunidad: " + nombreComunidad;
        final String body =
                "<html><body style='font-family:Arial,sans-serif;color:#333'><div"
                    + " style='max-width:600px;margin:0 auto;padding:20px'><div"
                    + " style='background:#2D3250;color:white;padding:20px;text-align:center;border-radius:5px"
                    + " 5px 0 0'><h1>Nuevo anuncio en tu comunidad</h1></div><div"
                    + " style='background:#f9f9f9;padding:20px;border:1px solid #ddd'><p>Hola"
                    + " <strong>"
                        + escapeHtml(nombreDestinatario)
                        + "</strong>,</p><p><strong>"
                        + escapeHtml(nombreAutor)
                        + "</strong> ha publicado un anuncio en <strong>"
                        + escapeHtml(nombreComunidad)
                        + "</strong>.</p><div"
                        + " style='background:#fff7ed;padding:14px;border-left:4px solid"
                        + " #c2410c;margin:18px 0'><strong>"
                        + escapeHtml(titulo)
                        + "</strong><p style='margin-top:10px;white-space:pre-wrap'>"
                        + escapeHtml(vistaPrevia)
                        + "</p></div><p>Entra en la comunidad para ver el anuncio"
                        + " completo.</p></div><div"
                        + " style='background:#f0f0f0;padding:15px;text-align:center;font-size:12px;border-radius:0"
                        + " 0 5px 5px'><p>&copy; "
                        + appName
                        + "</p></div></div></body></html>";

        sendHtmlEmailSafe(destinatario.getEmail(), subject, body);
    }

    private void sendHtmlEmailSafe(String to, String subject, String htmlBody) {
        try {
            doSendHtml(to, subject, htmlBody);
            log.info("Email '{}' enviado a: {}", subject, to);
        } catch (Exception e) {
            log.error("Error al enviar email '{}' a {}: {}", subject, to, e.getMessage());
        }
    }
}
