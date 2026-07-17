package ru.formatkoda.polybank.messaging.dto;

import lombok.NonNull;

import java.time.OffsetDateTime;

public record TransactionNotificationEventDto(
        @NonNull Long id,
        String fromAccountNumber,
        String toAccountNumber,
        Long fromUserId,
        Long toUserId,
        String name,
        String email,
        @NonNull String amount,
        @NonNull String type,
        @NonNull String status,
        @NonNull OffsetDateTime createdAt
) {
}
