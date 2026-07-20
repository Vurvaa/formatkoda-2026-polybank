package ru.formatkoda.authorization.auth.domain;

import lombok.NonNull;

import java.time.OffsetDateTime;

public record UserEntity(
        Long id,
        @NonNull UserLogin login,
        UserEmail email,
        @NonNull String name,
        @NonNull String lastName,
        @NonNull String passwordHash,
        @NonNull OffsetDateTime createdAt,
        OffsetDateTime blockedAt
) {
    public boolean isBlocked() {
        return blockedAt != null;
    }
}
