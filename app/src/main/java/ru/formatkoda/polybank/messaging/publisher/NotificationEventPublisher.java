package ru.formatkoda.polybank.messaging.publisher;

import ru.formatkoda.polybank.messaging.dto.UserNotificationEventDto;

import java.util.List;

public interface NotificationEventPublisher {

    void publishUserNotificationEvent(UserNotificationEventDto user, List<String> notificationTypeName, String notificationTemplateName);
}
