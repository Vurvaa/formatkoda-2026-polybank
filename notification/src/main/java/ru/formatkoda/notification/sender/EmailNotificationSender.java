package ru.formatkoda.notification.sender;

import org.springframework.stereotype.Component;
import ru.formatkoda.notification.domain.DeliveryEntity;

@Component
public class EmailNotificationSender implements NotificationSender {
    @Override
    public DeliveryEntity.Type getNotificationType() {
        return DeliveryEntity.Type.EMAIL;
    }

    @Override
    public boolean sendNotification(String notification, String destination) {
        return true;
    }
}
