package ru.formatkoda.polybank.messaging.outbox.publisher;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.formatkoda.polybank.messaging.dto.UserNotificationEventDto;
import ru.formatkoda.polybank.messaging.event.UserNotificationEvent;
import ru.formatkoda.polybank.messaging.outbox.infrastructure.OutboxWriter;
import ru.formatkoda.polybank.messaging.publisher.NotificationEventPublisher;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class NotificationOutboxPublisher implements NotificationEventPublisher {
    private static final String TOPIC = "bank.notifications";
    private static final String AGGREGATE_TYPE_USER = "User";

    private final OutboxWriter outboxWriter;

    private static final Map<String, String> notificationEventTypeRegistry = Map.of(
            "USER_REGISTERED", "UserRegistered"
    );

    private static String getUserNotificationEventType(String key) {
        return notificationEventTypeRegistry.get(key);
    }

    @Override
    public void publishUserNotificationEvent(
            UserNotificationEventDto user,
            List<String> notificationTypeNames,
            String notificationTemplateName
    ) {
        UserNotificationEvent payload = new UserNotificationEvent(
                user.id(),
                notificationTypeNames,
                notificationTemplateName,
                user
        );

        outboxWriter.append(
                TOPIC,
                AGGREGATE_TYPE_USER,
                user.id().toString(),
                getUserNotificationEventType(notificationTemplateName),
                payload
        );
    }
}
