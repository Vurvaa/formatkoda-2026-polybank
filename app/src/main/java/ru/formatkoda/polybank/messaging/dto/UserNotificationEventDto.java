package ru.formatkoda.polybank.messaging.dto;

import lombok.NonNull;

import java.time.OffsetDateTime;

public record UserNotificationEventDto(
        Long id,
        @NonNull String login,
        @NonNull String name,
        @NonNull String lastName,
        String email,
        @NonNull OffsetDateTime createdAt,
        OffsetDateTime blockedAt
) {}
