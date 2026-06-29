package com.skillnet.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class MailStartupLogger {

    private static final Logger log = LoggerFactory.getLogger(MailStartupLogger.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${skillnet.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${skillnet.mail.from:noreply@skillnet.com}")
    private String mailFrom;

    @Value("${skillnet.mail.from-name:SkillNet}")
    private String mailFromName;

    @EventListener(ApplicationReadyEvent.class)
    public void logMailStatus() {
        if (!mailEnabled) {
            log.warn(
                    "Correo DESACTIVADO (skillnet.mail.enabled=false). Los códigos de verificación solo aparecen en consola. "
                            + "Activa SKILLNET_MAIL_ENABLED=true y SMTP en backend/.env para enviar a Gmail.");
            return;
        }
        if (!StringUtils.hasText(mailHost) || !StringUtils.hasText(mailUsername) || !StringUtils.hasText(mailPassword)) {
            log.error(
                    "Correo activado pero SMTP incompleto. Configura SPRING_MAIL_HOST, SPRING_MAIL_USERNAME y "
                            + "SPRING_MAIL_PASSWORD en backend/.env y reinicia el backend.");
            return;
        }
        if (mailSender == null) {
            log.error(
                    "Correo activado pero JavaMailSender no está disponible. Revisa spring.mail.* en backend/.env.");
            return;
        }
        String sender = StringUtils.hasText(mailFromName) ? mailFromName + " <" + mailFrom + ">" : mailFrom;
        log.info("Correo ACTIVO — remitente {} vía SMTP {} ({})", sender, mailUsername, mailHost);
    }
}
