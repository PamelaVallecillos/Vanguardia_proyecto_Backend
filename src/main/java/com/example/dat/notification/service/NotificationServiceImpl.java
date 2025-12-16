package com.example.dat.notification.service;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.example.dat.enums.NotificationType;
import com.example.dat.notification.dto.NotificationDTO;
import com.example.dat.notification.entity.Notification;
import com.example.dat.notification.repo.NotificationRepo;
import com.example.dat.users.entity.User;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService{

    private final NotificationRepo notificationRepo;
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String mailFrom;


    @Override
    @Async
    public void sendEmail(NotificationDTO notificationDTO, User user) {

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setTo(notificationDTO.getRecipient());
            helper.setSubject(notificationDTO.getSubject());
            if (mailFrom != null && !mailFrom.isBlank()) {
                helper.setFrom(mailFrom);
            }


            // Use template if provided
            if (notificationDTO.getTemplateName() != null){

                Context context = new Context();
                context.setVariables(notificationDTO.getTemplateVariables());
                String htmlContent = templateEngine.process(notificationDTO.getTemplateName(), context);

                helper.setText(htmlContent, true);

            }else{
                helper.setText(notificationDTO.getMessage(), true);
            }


            mailSender.send(mimeMessage);
            log.info("Email sent out to {}", notificationDTO.getRecipient());


            //save to our database table
            Notification notificationToSave = Notification.builder()
                    .recipient(notificationDTO.getRecipient())
                    .subject(notificationDTO.getSubject())
                    .message(notificationDTO.getMessage())
                    .type(NotificationType.EMAIL)
                    .user(user)
                    .build();

            notificationRepo.save(notificationToSave);

        }catch (Exception e){
            log.error("Failed to send email", e);
        }

    }

    @Override
    @Async
    public void sendExpedienteNotification(String userEmail, String userName, String expedienteNumber, String patientName) {
        
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );
            
            helper.setTo(userEmail);
            helper.setSubject("Número de Expediente Asignado - AgendaSalud");
            if (mailFrom != null && !mailFrom.isBlank()) {
                helper.setFrom(mailFrom);
            }
            
            // Usar la plantilla expediente-notification.html
            Context context = new Context();
            context.setVariable("name", userName);
            context.setVariable("expedienteNumber", expedienteNumber);
            context.setVariable("patientName", patientName);
            
            String htmlContent = templateEngine.process("expediente-notification", context);
            helper.setText(htmlContent, true);
            
            mailSender.send(mimeMessage);
            log.info("Expediente notification email sent to {}: Expediente #{}", userEmail, expedienteNumber);
            
        } catch (Exception e) {
            log.error("Failed to send expediente notification email to {}", userEmail, e);
        }
    }
}
