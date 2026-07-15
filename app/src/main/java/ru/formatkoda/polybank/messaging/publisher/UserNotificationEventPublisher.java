package ru.formatkoda.polybank.messaging.publisher;

import ru.formatkoda.polybank.domain.user.UserEntity;

import java.util.List;

public interface UserNotificationEventPublisher {

    void publishUserNotificationEvent(UserEntity user, List<String> notificationTypeName, String notificationTemplateName);
}
