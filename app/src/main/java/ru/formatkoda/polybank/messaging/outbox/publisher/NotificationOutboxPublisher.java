package ru.formatkoda.polybank.messaging.outbox.publisher;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.formatkoda.polybank.messaging.dto.AccountNotificationEventDto;
import ru.formatkoda.polybank.messaging.dto.TransactionNotificationEventDto;
import ru.formatkoda.polybank.messaging.dto.UserNotificationEventDto;
import ru.formatkoda.polybank.messaging.event.NotificationEvent;
import ru.formatkoda.polybank.messaging.outbox.infrastructure.OutboxWriter;
import ru.formatkoda.polybank.messaging.publisher.NotificationEventPublisher;

import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationOutboxPublisher implements NotificationEventPublisher {
    private static final String TOPIC = "bank.notifications";

    private static final String AGGREGATE_TYPE_USER = "User";
    private static final String AGGREGATE_TYPE_ACCOUNT = "Account";
    private static final String AGGREGATE_TYPE_TRANSACTION = "Transaction";

    private final OutboxWriter outboxWriter;

    public enum AvailableNotificationMethods {
        EMAIL
    }

    @Override
    public void publishUserNotificationEvent(
            UserNotificationEventDto user,
            List<String> notificationTypeNames,
            NotificationTemplate notificationTemplate
    ) {
        NotificationEvent payload = new NotificationEvent(
                notificationTypeNames,
                notificationTemplate.name(),
                user
        );

        outboxWriter.append(
                TOPIC,
                AGGREGATE_TYPE_USER,
                user.id().toString(),
                notificationTemplate.getEventType(),
                payload
        );
    }

    @Override
    public void publishAccountNotificationEvent(
            AccountNotificationEventDto account,
            List<String> notificationTypeNames,
            NotificationTemplate notificationTemplate
    ) {
        NotificationEvent payload = new NotificationEvent(
                notificationTypeNames,
                notificationTemplate.name(),
                account
        );

        outboxWriter.append(
                TOPIC,
                AGGREGATE_TYPE_ACCOUNT,
                account.userId().toString(),
                notificationTemplate.getEventType(),
                payload
        );
    }

    @Override
    public void publishTransactionNotificationEvent(
            TransactionNotificationEventDto transaction,
            List<String> notificationTypeNames,
            NotificationTemplate notificationTemplate
    ) {
        NotificationEvent payload = new NotificationEvent(
                notificationTypeNames,
                notificationTemplate.name(),
                transaction
        );

        outboxWriter.append(
                TOPIC,
                AGGREGATE_TYPE_TRANSACTION,
                transaction.id().toString(),
                notificationTemplate.getEventType(),
                payload
        );
    }
}
