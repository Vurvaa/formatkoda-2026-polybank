package ru.formatkoda.polybank.messaging.publisher;

import ru.formatkoda.polybank.domain.user.UserEntity;

public interface UserNotificationEventPublisher {

    void publishUserNotificationEvent(UserEntity user, String notificationTypeName, String notificationTemplateName);
}
