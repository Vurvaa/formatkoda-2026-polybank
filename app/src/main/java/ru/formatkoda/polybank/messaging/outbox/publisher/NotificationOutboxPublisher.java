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
import java.util.Map;

@Component
@RequiredArgsConstructor
public class NotificationOutboxPublisher implements NotificationEventPublisher {
    private static final String TOPIC = "bank.notifications";

    private static final String AGGREGATE_TYPE_USER = "User";
    private static final String AGGREGATE_TYPE_ACCOUNT = "Account";
    private static final String AGGREGATE_TYPE_TRANSACTION = "Transaction";

    private final OutboxWriter outboxWriter;

    private static final Map<String, String> notificationEventTypeRegistry = Map.of(
            "USER_REGISTERED", "UserRegistered",
            "ACCOUNT_CREATED", "AccountCreated",
            "TRANSACTION_WITHDRAW", "TransactionWithdraw",
            "TRANSACTION_TOP_UP", "TransactionTopUp",
            "TRANSACTION_BETWEEN_PERSON_ACCOUNTS", "TransactionBetweenPersonAccounts",
            "TRANSACTION_TOP_UP_BETWEEN_ACCOUNTS", "TransactionTopUpBetweenAccounts",
            "TRANSACTION_WITHDRAW_BETWEEN_ACCOUNTS", "TransactionWithdrawBetweenAccounts"
    );

    public enum AvailableNotificationMethods {
        EMAIL
    }

    private static String getUserNotificationEventType(String key) {
        return notificationEventTypeRegistry.get(key);
    }

    @Override
    public void publishUserNotificationEvent(
            UserNotificationEventDto user,
            List<String> notificationTypeNames,
            String notificationTemplateName
    ) {
        NotificationEvent payload = new NotificationEvent(
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

    @Override
    public void publishAccountNotificationEvent(
            AccountNotificationEventDto account,
            List<String> notificationTypeNames,
            String notificationTemplateName
    ) {
        NotificationEvent payload = new NotificationEvent(
                notificationTypeNames,
                notificationTemplateName,
                account
        );

        outboxWriter.append(
                TOPIC,
                AGGREGATE_TYPE_ACCOUNT,
                account.userId().toString(),
                getUserNotificationEventType(notificationTemplateName),
                payload
        );
    }

    public void publishTransactionNotificationEvent(
            TransactionNotificationEventDto transaction,
            List<String> notificationTypeNames,
            String notificationTemplateName
    ) {
        NotificationEvent payload = new NotificationEvent(
                notificationTypeNames,
                notificationTemplateName,
                transaction
        );

        outboxWriter.append(
                TOPIC,
                AGGREGATE_TYPE_TRANSACTION,
                transaction.id().toString(),
                getUserNotificationEventType(notificationTemplateName),
                payload
        );
    }
}
