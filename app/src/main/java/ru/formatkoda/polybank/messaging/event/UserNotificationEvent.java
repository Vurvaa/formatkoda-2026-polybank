package ru.formatkoda.polybank.messaging.event;

import java.util.List;

public record UserNotificationEvent(
        Long userId,
        List<String> notificationTypeNames,
        String notificationTemplateName,
        Object entity
) {}
