package ru.formatkoda.polybank.messaging.dto;

import lombok.NonNull;
import ru.formatkoda.polybank.domain.user.UserLogin;

import java.time.OffsetDateTime;

public record UserNotificationEventDto(
        Long id,
        @NonNull UserLogin login,
        @NonNull String name,
        @NonNull String lastName,
        String email,
        @NonNull OffsetDateTime createdAt,
        OffsetDateTime blockedAt
) {}
