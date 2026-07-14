package ru.formatkoda.notification.service;

import org.springframework.stereotype.Component;
import ru.formatkoda.notification.domain.DeliveryEntity;
import ru.formatkoda.notification.sender.NotificationSender;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class NotificationSenderRegistry {
    private final Map<DeliveryEntity.Type, NotificationSender> notificationSenders;

    public NotificationSenderRegistry(List<NotificationSender> notificationSenders) {
        this.notificationSenders = notificationSenders.stream().collect(
                Collectors.toMap(
                        NotificationSender::getNotificationType,
                        Function.identity()
                )
        );
    }

    public NotificationSender getNotificationSender(DeliveryEntity.Type notificationType) {
        NotificationSender sender = notificationSenders.get(notificationType);
        if (sender == null) {
            throw new RuntimeException("unknown notification type");
        }

        return sender;
    }
}
