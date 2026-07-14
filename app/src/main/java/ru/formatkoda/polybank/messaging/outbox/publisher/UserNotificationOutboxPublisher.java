package ru.formatkoda.polybank.messaging.outbox.publisher;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.formatkoda.polybank.domain.user.UserEntity;
import ru.formatkoda.polybank.messaging.event.UserNotificationEvent;
import ru.formatkoda.polybank.messaging.outbox.infrastructure.OutboxWriter;
import ru.formatkoda.polybank.messaging.publisher.UserNotificationEventPublisher;

@Component
@RequiredArgsConstructor
public class UserNotificationOutboxPublisher implements UserNotificationEventPublisher {
    private static final String TOPIC = "bank.notifications";
    private static final String AGGREGATE_TYPE = "User";

    private final OutboxWriter outboxWriter;

    @Override
    public void publishUserNotificationEvent(
            UserEntity user,
            String notificationTypeName,
            String notificationTemplateName
    ) {
        UserNotificationEvent payload = new UserNotificationEvent(
                user.id(),
                notificationTypeName,
                notificationTemplateName,
                user
        );

        outboxWriter.append(
                TOPIC,
                AGGREGATE_TYPE,
                user.id().toString(),
                UserNotificationEvent.getUserNotificationEventType(notificationTemplateName),
                payload
        );
    }
}
