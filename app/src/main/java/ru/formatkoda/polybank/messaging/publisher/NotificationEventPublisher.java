package ru.formatkoda.polybank.messaging.publisher;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.formatkoda.polybank.messaging.dto.AccountNotificationEventDto;
import ru.formatkoda.polybank.messaging.dto.TransactionNotificationEventDto;
import ru.formatkoda.polybank.messaging.dto.UserNotificationEventDto;

import java.util.List;

public interface NotificationEventPublisher {

    @Getter
    @RequiredArgsConstructor
    enum NotificationTemplate {
        USER_REGISTERED("UserRegistered"),
        ACCOUNT_CREATED("AccountCreated"),
        TRANSACTION_WITHDRAW("TransactionWithdraw"),
        TRANSACTION_TOP_UP("TransactionTopUp"),
        TRANSACTION_BETWEEN_PERSON_ACCOUNTS("TransactionBetweenPersonAccounts"),
        TRANSACTION_TOP_UP_BETWEEN_ACCOUNTS("TransactionTopUpBetweenAccounts"),
        TRANSACTION_WITHDRAW_BETWEEN_ACCOUNTS("TransactionWithdrawBetweenAccounts");

        private final String eventType;
    }

    void publishUserNotificationEvent(
            UserNotificationEventDto user,
            List<String> notificationTypeNames,
            NotificationTemplate notificationTemplateName
    );

    void publishAccountNotificationEvent(
            AccountNotificationEventDto account,
            List<String> notificationTypeNames,
            NotificationTemplate notificationTemplateName
    );

    void publishTransactionNotificationEvent(
            TransactionNotificationEventDto transaction,
            List<String> notificationTypeNames,
            NotificationTemplate notificationTemplateName
    );
}
