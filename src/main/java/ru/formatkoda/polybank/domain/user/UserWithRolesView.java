package ru.formatkoda.polybank.domain.user;

import lombok.NonNull;

import java.time.OffsetDateTime;
import java.util.List;

public record UserWithRolesView(
        Long id,
        @NonNull UserLogin login,
        @NonNull String name,
        @NonNull String lastName,
        @NonNull List<String> roles,
        @NonNull OffsetDateTime createdAt,
        OffsetDateTime blockedAt
) {
}
