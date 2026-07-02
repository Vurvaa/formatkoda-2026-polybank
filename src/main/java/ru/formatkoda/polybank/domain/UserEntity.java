package ru.formatkoda.polybank.domain;

import lombok.NonNull;

import java.time.OffsetDateTime;

public record UserEntity(
        Long id,
        @NonNull String login,
        @NonNull String name,
        @NonNull String lastName,
        @NonNull String passwordHash,
        OffsetDateTime createdAt,
        OffsetDateTime blockedAt
) {
    public boolean isBlocked() {
        return blockedAt != null;
    }
}
