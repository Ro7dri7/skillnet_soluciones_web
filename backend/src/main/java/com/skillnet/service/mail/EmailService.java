package com.skillnet.service.mail;

import com.skillnet.persistence.entity.core.Course;
import com.skillnet.persistence.entity.payments.Payment;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final DateTimeFormatter RECEIPT_DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-PE"));

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${skillnet.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${skillnet.mail.from:noreply@skillnet.com}")
    private String fromAddress;

    @Value("${skillnet.mail.from-name:SkillNet}")
    private String fromName;

    public void sendPasswordResetEmail(String toEmail, String resetUrl) {
        String subject = "Restablece tu contraseña — SkillNet";
        String body = """
                Hola,

                Recibimos una solicitud para restablecer la contraseña de tu cuenta en SkillNet.

                Abre este enlace (válido 24 horas):
                %s

                Si no solicitaste este cambio, ignora este correo.

                — Equipo SkillNet
                """.formatted(resetUrl);

        sendPlainText(toEmail, subject, body, "Password reset");
    }

    public void sendVerificationCodeEmail(String toEmail, String code, String firstName) {
        String greeting = firstName != null && !firstName.isBlank() ? "Hola " + firstName.trim() : "Hola";
        String subject = "Tu código de verificación — SkillNet";
        String plain = """
                %s,

                Tu código de verificación en SkillNet es:

                %s

                Válido por 30 minutos. Si no creaste esta cuenta, ignora este correo.

                — Equipo SkillNet
                """.formatted(greeting, code);

        String html = """
                <!DOCTYPE html>
                <html lang="es">
                <body style="margin:0;background:#f4f7fb;font-family:Segoe UI,Arial,sans-serif;color:#032b60;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="padding:32px 16px;">
                    <tr><td align="center">
                      <table width="520" cellpadding="0" cellspacing="0" style="background:#fff;border-radius:16px;overflow:hidden;box-shadow:0 8px 24px rgba(3,43,96,.08);">
                        <tr><td style="background:#145bff;padding:24px 28px;color:#fff;">
                          <h1 style="margin:0;font-size:22px;">Verifica tu correo</h1>
                          <p style="margin:8px 0 0;opacity:.9;font-size:14px;">SkillNet</p>
                        </td></tr>
                        <tr><td style="padding:28px;">
                          <p style="margin:0 0 16px;font-size:15px;line-height:1.5;">%s,</p>
                          <p style="margin:0 0 20px;font-size:14px;line-height:1.6;color:#4a6fa5;">
                            Ingresa este código en la pantalla de verificación para continuar:
                          </p>
                          <div style="text-align:center;margin:24px 0;">
                            <span style="display:inline-block;padding:16px 28px;background:#eef4ff;border-radius:12px;font-size:32px;font-weight:800;letter-spacing:8px;color:#145bff;">%s</span>
                          </div>
                          <p style="margin:0;font-size:12px;color:#64748b;">Válido 30 minutos. No compartas este código.</p>
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(greeting, code);

        sendHtml(toEmail, subject, plain, html, "Verification code", true);
    }

    public void sendPurchaseReceiptEmail(Payment payment, List<Course> courses, String misCursosUrl) {
        if (payment == null || payment.getUser() == null) {
            return;
        }
        String toEmail = payment.getClientEmail() != null && !payment.getClientEmail().isBlank()
                ? payment.getClientEmail()
                : payment.getUser().getEmail();
        if (toEmail == null || toEmail.isBlank()) {
            return;
        }

        String clientName = payment.getClientName() != null && !payment.getClientName().isBlank()
                ? payment.getClientName()
                : payment.getUser().getFirstName();
        String dateText = payment.getCreatedAt() != null
                ? RECEIPT_DATE.format(payment.getCreatedAt().atZone(LIMA))
                : "";
        String amountText = formatMoney(payment.getAmount());
        String paymentId = payment.getId() != null ? String.valueOf(payment.getId()) : "—";

        StringBuilder itemsPlain = new StringBuilder();
        StringBuilder itemsHtml = new StringBuilder();
        for (Course course : courses) {
            if (course == null) {
                continue;
            }
            itemsPlain.append("• ").append(course.getTitle()).append("\n");
            itemsHtml.append("""
                    <tr>
                      <td style="padding:10px 0;border-bottom:1px solid #eef2ff;font-size:14px;">%s</td>
                      <td style="padding:10px 0;border-bottom:1px solid #eef2ff;text-align:right;font-size:14px;color:#4a6fa5;">Infoproducto</td>
                    </tr>
                    """.formatted(escapeHtml(course.getTitle())));
        }
        if (itemsPlain.isEmpty() && payment.getCourse() != null) {
            itemsPlain.append("• ").append(payment.getCourse().getTitle()).append("\n");
            itemsHtml.append("""
                    <tr>
                      <td style="padding:10px 0;border-bottom:1px solid #eef2ff;font-size:14px;">%s</td>
                      <td style="padding:10px 0;border-bottom:1px solid #eef2ff;text-align:right;font-size:14px;color:#4a6fa5;">Infoproducto</td>
                    </tr>
                    """.formatted(escapeHtml(payment.getCourse().getTitle())));
        }

        String subject = "Confirmación de compra — SkillNet";
        String plain = """
                Hola %s,

                Tu pago fue procesado correctamente.

                Pedido #%s
                Fecha: %s
                Total: %s

                Productos:
                %s

                Accede a tus infoproductos:
                %s

                — Equipo SkillNet
                """.formatted(
                clientName != null ? clientName : "Cliente",
                paymentId,
                dateText,
                amountText,
                itemsPlain.toString().trim(),
                misCursosUrl);

        String html = """
                <!DOCTYPE html>
                <html lang="es">
                <body style="margin:0;background:#f4f7fb;font-family:Segoe UI,Arial,sans-serif;color:#032b60;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="padding:32px 16px;">
                    <tr><td align="center">
                      <table width="560" cellpadding="0" cellspacing="0" style="background:#fff;border-radius:16px;overflow:hidden;box-shadow:0 8px 24px rgba(3,43,96,.08);">
                        <tr><td style="background:linear-gradient(135deg,#145bff,#00a87e);padding:28px;color:#fff;">
                          <p style="margin:0 0 6px;font-size:13px;opacity:.9;">Pago exitoso</p>
                          <h1 style="margin:0;font-size:24px;">¡Gracias por tu compra!</h1>
                        </td></tr>
                        <tr><td style="padding:28px;">
                          <p style="margin:0 0 18px;font-size:15px;">Hola <strong>%s</strong>,</p>
                          <p style="margin:0 0 22px;font-size:14px;line-height:1.6;color:#4a6fa5;">
                            Confirmamos tu pago. Ya puedes acceder a tus infoproductos desde SkillNet.
                          </p>
                          <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f8f9ff;border-radius:12px;padding:16px 18px;margin-bottom:22px;">
                            <tr><td style="font-size:12px;color:#64748b;">Pedido</td><td style="text-align:right;font-weight:700;">#%s</td></tr>
                            <tr><td style="font-size:12px;color:#64748b;padding-top:8px;">Fecha</td><td style="text-align:right;">%s</td></tr>
                            <tr><td style="font-size:12px;color:#64748b;padding-top:8px;">Total pagado</td><td style="text-align:right;font-size:18px;font-weight:800;color:#145bff;">%s</td></tr>
                          </table>
                          <h2 style="margin:0 0 12px;font-size:16px;">Resumen</h2>
                          <table width="100%%" cellpadding="0" cellspacing="0">%s</table>
                          <p style="margin:24px 0 0;text-align:center;">
                            <a href="%s" style="display:inline-block;background:#145bff;color:#fff;text-decoration:none;padding:14px 28px;border-radius:10px;font-weight:700;">Ir a mis infoproductos</a>
                          </p>
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(
                escapeHtml(clientName != null ? clientName : "Cliente"),
                paymentId,
                dateText,
                amountText,
                itemsHtml.toString(),
                misCursosUrl);

        sendHtml(toEmail, subject, plain, html, "Purchase receipt");
    }

    private void sendPlainText(String toEmail, String subject, String body, String logLabel) {
        if (!mailEnabled || mailSender == null) {
            log.info("[mail-disabled] {} for {} → {}", logLabel, toEmail, body.lines().findFirst().orElse(""));
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(formatFromHeader());
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("{} email sent to {}", logLabel, toEmail);
        } catch (Exception ex) {
            log.error("Failed to send {} email to {}: {}", logLabel, toEmail, ex.getMessage());
            log.warn("[mail-fallback] Contenido no enviado:\n{}", body);
        }
    }

    private void sendHtml(String toEmail, String subject, String plain, String html, String logLabel) {
        sendHtml(toEmail, subject, plain, html, logLabel, false);
    }

    private void sendHtml(
            String toEmail, String subject, String plain, String html, String logLabel, boolean failOnError) {
        if (!mailEnabled || mailSender == null) {
            log.warn(
                    "[mail-disabled] {} no enviado a {}. Activa SKILLNET_MAIL_ENABLED=true y credenciales SMTP en backend/.env",
                    logLabel,
                    toEmail);
            log.info("[mail-disabled] Vista previa ({}):\n{}", logLabel, plain);
            if (failOnError) {
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "El envío de correo no está configurado. Contacta al administrador.");
            }
            return;
        }
        try {
            var mime = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(mime, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress, fromName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(plain, html);
            mailSender.send(mime);
            log.info("{} email sent to {}", logLabel, toEmail);
        } catch (Exception ex) {
            log.error("Failed to send {} email to {}: {}", logLabel, toEmail, ex.getMessage());
            log.warn("[mail-fallback] Revisa SPRING_MAIL_PASSWORD (contraseña de aplicación Gmail). Contenido:\n{}", plain);
            if (failOnError) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "No se pudo enviar el correo de verificación. Revisa la configuración SMTP del servidor.");
            }
        }
    }

    private String formatFromHeader() {
        if (fromName == null || fromName.isBlank()) {
            return fromAddress;
        }
        return "%s <%s>".formatted(fromName.trim(), fromAddress);
    }

    private static String formatMoney(BigDecimal amount) {
        if (amount == null) {
            return "USD 0.00";
        }
        return "USD " + amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
