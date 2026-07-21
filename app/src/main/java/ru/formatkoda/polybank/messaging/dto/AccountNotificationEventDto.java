package ru.formatkoda.polybank.messaging.dto;

import lombok.NonNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AccountNotificationEventDto(
        @NonNull Long id,
        @NonNull String number,
        @NonNull Long userId,
        @NonNull String name,
        String email,
        @NonNull BigDecimal balance,
        @NonNull String type,
        @NonNull String status,
        @NonNull OffsetDateTime createdAt
) {
}
