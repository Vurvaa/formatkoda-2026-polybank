package ru.formatkoda.polybank.messaging.event;

import java.util.Map;

public record UserNotificationEvent(
        Long userId,
        String notificationTypeName,
        String notificationTemplateName,
        Object entity
) {
    private static final Map<String, String> map = Map.of(
            "USER_REGISTERED", "UserRegistered"
    );

    public static String getUserNotificationEventType(String key) {
        return map.get(key);
    }
}
