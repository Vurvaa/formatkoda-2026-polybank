package ru.formatkoda.notification.sender;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import ru.formatkoda.notification.domain.DeliveryEntity;

@Component
@RequiredArgsConstructor
public class EmailNotificationSender implements NotificationSender {
    private final JavaMailSender mailSender;

    @Override
    public DeliveryEntity.Type getNotificationType() {
        return DeliveryEntity.Type.EMAIL;
    }

    @Override
    public boolean sendNotification(String notification, String destination) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@polybank.ru");
            message.setTo(destination);
            message.setSubject("Polybank notification");
            message.setText(notification);

            mailSender.send(message);
            return true;
        } catch (MailException _) {
            return false;
        }
    }
}
