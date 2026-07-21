package ru.formatkoda.polybank.messaging.event;

import java.util.List;

public record NotificationEvent(
        List<String> notificationTypeNames,
        String notificationTemplateName,
        Object entity
) {
}
