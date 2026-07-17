package ru.formatkoda.polybank.messaging.publisher;

import ru.formatkoda.polybank.messaging.dto.AccountNotificationEventDto;
import ru.formatkoda.polybank.messaging.dto.TransactionNotificationEventDto;
import ru.formatkoda.polybank.messaging.dto.UserNotificationEventDto;

import java.util.List;

public interface NotificationEventPublisher {

    void publishUserNotificationEvent(
            UserNotificationEventDto user,
            List<String> notificationTypeNames,
            String notificationTemplateName
    );

    void publishAccountNotificationEvent(
            AccountNotificationEventDto account,
            List<String> notificationTypeNames,
            String notificationTemplateName
    );

    void publishTransactionNotificationEvent(
            TransactionNotificationEventDto transaction,
            List<String> notificationTypeNames,
            String notificationTemplateName
    );
}
