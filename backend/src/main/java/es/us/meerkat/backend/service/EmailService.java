package es.us.meerkat.backend.service;

import java.nio.charset.StandardCharsets;
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

import es.us.meerkat.backend.entity.AsistenciaEvento;
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

    @Value("${spring.mail.from:noreply@meerkat.es}")
    private String from;

    @Value("${app.name:Meerkat}")
    private String appName;

    @Value("${app.url:http://localhost:3000}")
    private String appUrl;

    private final JavaMailSender mailSender;
    private final AsistenciaEventoRepository asistenciaEventoRepository;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", new Locale("es", "ES"));
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // ===============================
    // CÓDIGO EXISTENTE — SIN CAMBIOS
    // ===============================

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
                        .replace("{{totalAsistentes}}", String.valueOf(asistencias.size()))
                        .replace("{{urlPreferencias}}", appUrl + "/settings/notifications");

        // ── Bloque virtual / ubicación (mutuamente excluyentes) ─────────────
        //
        // REGLA: si esVirtual=true → mostrar aviso de acceso por plataforma,
        // NUNCA mostrar la ubicación física.
        // si esVirtual=false → mostrar ubicación si existe, nunca el bloque virtual.
        //
        if (Boolean.TRUE.equals(evento.getEsVirtual())) {
            // Mostrar bloque virtual con enlace a "Mis eventos" en la plataforma
            html =
                    html.replace("{{#esVirtual}}", "")
                            .replace("{{/esVirtual}}", "")
                            .replace("{{urlMisEventos}}", appUrl + "/my-events");
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
        final MimeMessage message = mailSender.createMimeMessage();
        final MimeMessageHelper helper =
                new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
        helper.setFrom(from, appName);
        helper.setTo(to);
        helper.setSubject(asunto);
        helper.setText(html, true);
        mailSender.send(message);
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
}
