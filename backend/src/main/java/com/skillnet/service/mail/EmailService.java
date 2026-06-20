package com.skillnet.service.mail;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${skillnet.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${skillnet.mail.from:noreply@skillnet.com}")
    private String fromAddress;

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

        if (!mailEnabled || mailSender == null) {
            log.info("[mail-disabled] Password reset for {} → {}", toEmail, resetUrl);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
        log.info("Password reset email sent to {}", toEmail);
    }
}
