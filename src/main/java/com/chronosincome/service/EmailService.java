package com.chronosincome.service;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.*;
import com.sendgrid.helpers.mail.objects.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    @Value("${sendgrid.api.key}")
    private String sendGridApiKey;

    @Value("${app.mail.from}")
    private String fromEmail;

    public void sendPasswordResetEmail(String toEmail, String name, String link) {
        Email from = new Email(fromEmail);
        Email to = new Email(toEmail);
        String subject = "Recuperação de senha - Chronos Income";
        Content content = new Content("text/plain",
                "Olá, " + name + "!\n\n"
                        + "Clique no link abaixo para redefinir sua senha:\n"
                        + link + "\n\n"
                        + "O link expira em 1 hora.\n\n"
                        + "Se você não solicitou isso, ignore este e-mail.\n\n"
                        + "— Equipe Chronos Income");

        Mail mail = new Mail(from, subject, to, content);
        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();

        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            sg.api(request);
        } catch (Exception e) {
            log.error("Erro ao enviar e-mail via SendGrid: {}", e.getMessage());
            throw new RuntimeException("Falha ao enviar e-mail de recuperação");
        }
    }
}