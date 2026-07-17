package ru.formatkoda.notification.sender;

import ru.formatkoda.notification.domain.DeliveryEntity;

public interface NotificationSender {
    DeliveryEntity.Type getNotificationType();
    boolean sendNotification(String notification, String destination);
}
